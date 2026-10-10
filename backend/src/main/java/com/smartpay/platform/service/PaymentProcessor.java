package com.smartpay.platform.service;

import com.smartpay.platform.config.PaymentProperties;
import com.smartpay.platform.entity.*;
import com.smartpay.platform.event.PaymentCompletedEvent;
import com.smartpay.platform.event.PaymentCreatedEvent;
import com.smartpay.platform.event.PaymentFailedEvent;
import com.smartpay.platform.exception.FraudDetectedException;
import com.smartpay.platform.exception.InsufficientBalanceException;
import com.smartpay.platform.exception.InvalidTransactionException;
import com.smartpay.platform.exception.PaymentException;
import com.smartpay.platform.exception.ResourceNotFoundException;
import com.smartpay.platform.repository.TransactionRepository;
import com.smartpay.platform.repository.UserRepository;
import com.smartpay.platform.repository.WalletRepository;
import com.smartpay.platform.service.fraud.RiskAssessment;
import com.smartpay.platform.service.fraud.RiskContext;
import com.smartpay.platform.util.TransactionReferenceGenerator;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The transactional "unit of work" steps of a payment. Each public method is its own database transaction and is
 * called from PaymentService (a different bean, so the @Transactional proxy is always applied):
 *
 *   createPending -> startProcessing -> execute  (or markFailed on unexpected errors)
 *
 * Splitting the lifecycle this way means PENDING and PROCESSING are real, committed states, and a business failure
 * (insufficient balance, fraud block) is stored as a FAILED transaction instead of being rolled back and lost.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessor {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final FraudDetectionService fraudDetectionService;
    private final TransactionEventService eventService;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionReferenceGenerator referenceGenerator;
    private final PaymentProperties properties;
    private final Clock clock;

    /** Result of execute(): failure is null on success. The failure itself has already been persisted. */
    public record ExecutionOutcome(PaymentException failure) {
        public boolean succeeded() {
            return failure == null;
        }
    }

    @Transactional
    public Transaction createPending(Long senderId, Long receiverId, BigDecimal amount, String description,
                                     String idempotencyKey, TransactionType type) {
        Transaction tx = transactionRepository.saveAndFlush(Transaction.builder()
                .transactionReference(referenceGenerator.next())
                .sender(userRepository.getReferenceById(senderId))
                .receiver(userRepository.getReferenceById(receiverId))
                .amount(amount)
                .currency(properties.currency())
                .transactionType(type)
                .status(TransactionStatus.PENDING)
                .description(description)
                .idempotencyKey(idempotencyKey)
                .build());
        eventService.record(tx.getId(), TransactionEventType.PAYMENT_CREATED, EventStatus.SUCCESS,
                type + " request accepted");
        eventPublisher.publishEvent(new PaymentCreatedEvent(tx.getTransactionReference(), tx.getId(), senderId,
                receiverId, amount, tx.getCurrency(), type, Instant.now(clock)));
        return tx;
    }

    /** @return true if this caller won the PENDING -> PROCESSING transition and should go on to execute(). */
    @Transactional
    public boolean startProcessing(Long transactionId) {
        int updated = transactionRepository.transition(transactionId, TransactionStatus.PENDING,
                TransactionStatus.PROCESSING, Instant.now(clock));
        if (updated == 1) {
            eventService.record(transactionId, TransactionEventType.PROCESSING_STARTED, EventStatus.SUCCESS, null);
        }
        return updated == 1;
    }

    @Transactional
    public ExecutionOutcome execute(Long transactionId) {
        Transaction tx = transactionRepository.findWithPartiesById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        try {
            if (tx.getTransactionType() == TransactionType.ADD_MONEY) {
                topUp(tx);
            } else {
                transfer(tx);
            }
            return new ExecutionOutcome(null);
        } catch (PaymentException ex) {
            // All validations run before any wallet is modified, so nothing needs to be undone here.
            markTransactionFailed(tx, ex.getMessage());
            return new ExecutionOutcome(ex);
        }
    }

    /** Safety net for unexpected technical errors so a transaction never stays stuck in PROCESSING. */
    @Transactional
    public void markFailed(Long transactionId, String reason) {
        transactionRepository.findWithPartiesById(transactionId)
                .filter(t -> t.getStatus() == TransactionStatus.PENDING || t.getStatus() == TransactionStatus.PROCESSING)
                .ifPresent(t -> markTransactionFailed(t, reason));
    }

    // ------------------------------------------------------------------------------------------------

    private void transfer(Transaction tx) {
        Long senderId = tx.getSender().getId();
        Long receiverId = tx.getReceiver().getId();
        BigDecimal amount = tx.getAmount();

        Map<Long, Wallet> wallets = lockWallets(tx, senderId, receiverId);
        Wallet senderWallet = wallets.get(senderId);
        Wallet receiverWallet = wallets.get(receiverId);

        // 1. user / wallet status
        requireActive(tx, tx.getSender(), senderWallet, "Sender");
        requireActive(tx, tx.getReceiver(), receiverWallet, "Receiver");

        // 2. balance
        if (!senderWallet.canCover(amount)) {
            throw new InsufficientBalanceException("Insufficient wallet balance", tx.getTransactionReference());
        }

        // 3. fraud / risk rules
        RiskAssessment risk = fraudDetectionService.evaluate(
                new RiskContext(tx.getSender(), tx.getReceiver(), amount, tx.getId()));
        tx.setRiskLevel(risk.level());
        tx.setRiskReasons(truncate(risk.summary(), 500));
        eventService.record(tx.getId(), TransactionEventType.RISK_EVALUATED, EventStatus.SUCCESS,
                "Risk level " + risk.level() + ": " + risk.summary());
        if (risk.isBlocked()) {
            throw new FraudDetectedException("Payment blocked by risk checks: " + risk.summary(),
                    tx.getTransactionReference());
        }

        // 4. move the money (both wallets are locked, so this is atomic with the status update)
        senderWallet.debit(amount);
        receiverWallet.credit(amount);
        markTransactionSucceeded(tx);
    }

    private void topUp(Transaction tx) {
        Long userId = tx.getReceiver().getId();
        Wallet wallet = lockWallets(tx, userId, userId).get(userId);
        requireActive(tx, tx.getReceiver(), wallet, "Account");
        wallet.credit(tx.getAmount());
        tx.setRiskLevel(RiskLevel.LOW);
        markTransactionSucceeded(tx);
    }

    /** Wallets are always locked in ascending id order so two opposite transfers cannot deadlock. */
    private Map<Long, Wallet> lockWallets(Transaction tx, Long... userIds) {
        List<Long> ordered = Stream.of(userIds).distinct().sorted().toList();
        Map<Long, Wallet> wallets = new HashMap<>();
        for (Long userId : ordered) {
            wallets.put(userId, walletRepository.findByUserIdForUpdate(userId)
                    .orElseThrow(() -> new InvalidTransactionException("Wallet not found",
                            tx.getTransactionReference())));
        }
        return wallets;
    }

    private void requireActive(Transaction tx, User user, Wallet wallet, String who) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidTransactionException(who + " account is " + user.getStatus().name().toLowerCase(),
                    tx.getTransactionReference());
        }
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new InvalidTransactionException(who + " wallet is " + wallet.getStatus().name().toLowerCase(),
                    tx.getTransactionReference());
        }
    }

    private void markTransactionSucceeded(Transaction tx) {
        tx.setStatus(TransactionStatus.SUCCESS);
        eventService.record(tx.getId(), TransactionEventType.PAYMENT_COMPLETED, EventStatus.SUCCESS,
                "Amount " + tx.getAmount().toPlainString() + " " + tx.getCurrency() + " settled");
        eventPublisher.publishEvent(new PaymentCompletedEvent(tx.getTransactionReference(), tx.getId(),
                tx.getSender().getId(), tx.getReceiver().getId(), tx.getAmount(), tx.getCurrency(),
                tx.getTransactionType(), tx.getRiskLevel(), Instant.now(clock)));
        log.info("Payment SUCCESS ref={} type={} amount={} risk={}", tx.getTransactionReference(),
                tx.getTransactionType(), tx.getAmount(), tx.getRiskLevel());
    }

    private void markTransactionFailed(Transaction tx, String reason) {
        tx.setStatus(TransactionStatus.FAILED);
        tx.setFailureReason(truncate(reason, 255));
        eventService.record(tx.getId(), TransactionEventType.PAYMENT_FAILED, EventStatus.FAILURE, reason);
        eventPublisher.publishEvent(new PaymentFailedEvent(tx.getTransactionReference(), tx.getId(),
                tx.getSender().getId(), tx.getReceiver().getId(), tx.getAmount(), tx.getCurrency(),
                tx.getTransactionType(), tx.getFailureReason(), Instant.now(clock)));
        log.warn("Payment FAILED ref={} reason={}", tx.getTransactionReference(), reason);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
