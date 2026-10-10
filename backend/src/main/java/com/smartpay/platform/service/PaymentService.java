package com.smartpay.platform.service;

import com.smartpay.platform.config.PaymentProperties;
import com.smartpay.platform.dto.AddMoneyRequest;
import com.smartpay.platform.dto.PaymentRequest;
import com.smartpay.platform.dto.PaymentResponse;
import com.smartpay.platform.dto.TransactionResponse;
import com.smartpay.platform.entity.Transaction;
import com.smartpay.platform.entity.TransactionType;
import com.smartpay.platform.entity.User;
import com.smartpay.platform.exception.InvalidTransactionException;
import com.smartpay.platform.exception.ResourceNotFoundException;
import com.smartpay.platform.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the payment pipeline. This class is intentionally NOT @Transactional: every stage commits on its own
 * through PaymentProcessor so that PENDING / PROCESSING / FAILED are durable states.
 *
 *   validate -> idempotency check -> create PENDING -> mark PROCESSING -> status / balance / fraud checks
 *            -> move money -> SUCCESS or FAILED -> (after commit) Kafka events
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final UserRepository userRepository;
    private final IdempotencyService idempotencyService;
    private final PaymentProcessor processor;
    private final TransactionService transactionService;
    private final PaymentProperties properties;

    public PaymentResponse sendMoney(Long senderId, PaymentRequest request, String idempotencyKey) {
        User sender = loadUser(senderId);
        User receiver = userRepository.findByEmail(request.receiverEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));
        return submit(sender, receiver, request.amount(), request.description(), idempotencyKey,
                TransactionType.TRANSFER);
    }

    public PaymentResponse addMoney(Long userId, AddMoneyRequest request, String idempotencyKey) {
        if (request.amount().compareTo(properties.maxAddMoney()) > 0) {
            throw new InvalidTransactionException(
                    "You can add at most " + properties.maxAddMoney().toPlainString() + " at a time");
        }
        User user = loadUser(userId);
        return submit(user, user, request.amount(), "Wallet top-up", idempotencyKey, TransactionType.ADD_MONEY);
    }

    private PaymentResponse submit(User sender, User receiver, BigDecimal amount, String description,
                                   String rawKey, TransactionType type) {
        String key = normalizeKey(rawKey);

        Optional<Transaction> existing = idempotencyService.findExisting(sender.getId(), key);
        if (existing.isPresent()) {
            return replay(existing.get(), sender.getId(), receiver.getId(), amount, type);
        }

        Transaction tx;
        try {
            tx = processor.createPending(sender.getId(), receiver.getId(), amount, description, key, type);
        } catch (DataIntegrityViolationException raceLost) {
            // Two identical requests passed the lookup together; the UNIQUE(sender_id, idempotency_key) constraint
            // let only one insert through. Treat the loser as a replay of the winner.
            return idempotencyService.findExisting(sender.getId(), key)
                    .map(found -> replay(found, sender.getId(), receiver.getId(), amount, type))
                    .orElseThrow(() -> raceLost);
        }
        log.info("Payment created ref={} type={} sender={} receiver={} amount={}", tx.getTransactionReference(),
                type, sender.getId(), receiver.getId(), amount);

        Long txId = tx.getId();
        if (!processor.startProcessing(txId)) {
            // Another worker already took this transaction; just report its current state.
            return response(txId, sender.getId(), false);
        }

        PaymentProcessor.ExecutionOutcome outcome;
        try {
            outcome = processor.execute(txId);
        } catch (RuntimeException unexpected) {
            log.error("Unexpected error while processing payment ref={}", tx.getTransactionReference(), unexpected);
            processor.markFailed(txId, "Internal processing error");
            throw unexpected;
        }
        if (!outcome.succeeded()) {
            throw outcome.failure();
        }
        return response(txId, sender.getId(), false);
    }

    private PaymentResponse replay(Transaction existing, Long senderId, Long receiverId, BigDecimal amount,
                                   TransactionType type) {
        idempotencyService.assertSamePayload(existing, receiverId, amount, type);
        log.info("Idempotent replay: returning existing transaction ref={} status={}",
                existing.getTransactionReference(), existing.getStatus());
        return response(existing.getId(), senderId, true);
    }

    private PaymentResponse response(Long transactionId, Long viewerId, boolean replay) {
        TransactionResponse tx = transactionService.getResponse(transactionId, viewerId);
        return new PaymentResponse(tx, replay, message(tx, replay));
    }

    private static String message(TransactionResponse tx, boolean replay) {
        String base = switch (tx.status()) {
            case SUCCESS -> "Payment processed successfully";
            case FAILED -> "Payment failed: " + tx.failureReason();
            case PENDING, PROCESSING -> "Payment is being processed";
            default -> "Payment is " + tx.status().name().toLowerCase();
        };
        return replay ? "Duplicate request detected. Returning the original transaction. " + base : base;
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidTransactionException("Idempotency-Key header is required");
        }
        String trimmed = key.trim();
        if (trimmed.length() > 100) {
            throw new InvalidTransactionException("Idempotency-Key must be at most 100 characters");
        }
        return trimmed;
    }

    private User loadUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
