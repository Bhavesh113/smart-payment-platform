package com.smartpay.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartpay.platform.config.PaymentProperties;
import com.smartpay.platform.entity.*;
import com.smartpay.platform.event.PaymentCompletedEvent;
import com.smartpay.platform.event.PaymentFailedEvent;
import com.smartpay.platform.exception.FraudDetectedException;
import com.smartpay.platform.exception.InsufficientBalanceException;
import com.smartpay.platform.exception.InvalidTransactionException;
import com.smartpay.platform.repository.TransactionRepository;
import com.smartpay.platform.repository.UserRepository;
import com.smartpay.platform.repository.WalletRepository;
import com.smartpay.platform.service.fraud.RiskAssessment;
import com.smartpay.platform.service.fraud.RuleResult;
import com.smartpay.platform.util.TransactionReferenceGenerator;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class PaymentProcessorTest {

    private final TransactionRepository txRepo = mock(TransactionRepository.class);
    private final WalletRepository walletRepo = mock(WalletRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    private final FraudDetectionService fraud = mock(FraudDetectionService.class);
    private final TransactionEventService events = mock(TransactionEventService.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final TransactionReferenceGenerator refs = mock(TransactionReferenceGenerator.class);
    private final PaymentProperties props = new PaymentProperties(new BigDecimal("50000"), new BigDecimal("100000"),
            5, 10, new BigDecimal("20000"), 3, "INR", new BigDecimal("100000"), "Asia/Kolkata");
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-07T10:00:00Z"), ZoneOffset.UTC);

    private PaymentProcessor processor;
    private User alice;
    private User bob;
    private Wallet aliceWallet;
    private Wallet bobWallet;
    private Transaction tx;

    @BeforeEach
    void setUp() {
        processor = new PaymentProcessor(txRepo, walletRepo, userRepo, fraud, events, publisher, refs, props, clock);
        alice = user(1L, "Alice");
        bob = user(2L, "Bob");
        aliceWallet = wallet(10L, alice, "1000.00");
        bobWallet = wallet(20L, bob, "100.00");
        tx = transfer("250.00");

        when(txRepo.findWithPartiesById(100L)).thenReturn(Optional.of(tx));
        when(walletRepo.findByUserIdForUpdate(1L)).thenReturn(Optional.of(aliceWallet));
        when(walletRepo.findByUserIdForUpdate(2L)).thenReturn(Optional.of(bobWallet));
        when(fraud.evaluate(any())).thenReturn(new RiskAssessment(RiskLevel.LOW, List.of()));
    }

    private static User user(Long id, String name) {
        return User.builder().id(id).name(name).email(name.toLowerCase() + "@example.com").role(Role.USER)
                .status(UserStatus.ACTIVE).build();
    }

    private static Wallet wallet(Long id, User owner, String balance) {
        return Wallet.builder().id(id).user(owner).balance(new BigDecimal(balance)).currency("INR")
                .status(WalletStatus.ACTIVE).build();
    }

    private Transaction transfer(String amount) {
        return Transaction.builder().id(100L).transactionReference("TXN-TEST-0001").sender(alice).receiver(bob)
                .amount(new BigDecimal(amount)).currency("INR").transactionType(TransactionType.TRANSFER)
                .status(TransactionStatus.PROCESSING).idempotencyKey("k1").build();
    }

    @Test
    void successfulTransferMovesMoneyAndMarksSuccess() {
        PaymentProcessor.ExecutionOutcome outcome = processor.execute(100L);

        assertThat(outcome.succeeded()).isTrue();
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(tx.getRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(aliceWallet.getBalance()).isEqualByComparingTo("750.00");
        assertThat(bobWallet.getBalance()).isEqualByComparingTo("350.00");
        verify(publisher).publishEvent(any(PaymentCompletedEvent.class));
        verify(events).record(eq(100L), eq(TransactionEventType.PAYMENT_COMPLETED), eq(EventStatus.SUCCESS), anyString());
    }

    @Test
    void insufficientBalanceFailsTransactionWithoutTouchingWallets() {
        tx.setAmount(new BigDecimal("5000.00"));

        PaymentProcessor.ExecutionOutcome outcome = processor.execute(100L);

        assertThat(outcome.failure()).isInstanceOf(InsufficientBalanceException.class);
        assertThat(outcome.failure().getTransactionReference()).isEqualTo("TXN-TEST-0001");
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(tx.getFailureReason()).isEqualTo("Insufficient wallet balance");
        assertThat(aliceWallet.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(bobWallet.getBalance()).isEqualByComparingTo("100.00");
        verify(fraud, never()).evaluate(any());
        verify(publisher).publishEvent(any(PaymentFailedEvent.class));
    }

    @Test
    void fraudBlockFailsTransactionAndKeepsRiskLevel() {
        when(fraud.evaluate(any())).thenReturn(new RiskAssessment(RiskLevel.BLOCKED,
                List.of(new RuleResult("VELOCITY_EXCEEDED", RiskLevel.BLOCKED, "Too many transactions"))));

        PaymentProcessor.ExecutionOutcome outcome = processor.execute(100L);

        assertThat(outcome.failure()).isInstanceOf(FraudDetectedException.class);
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(tx.getRiskLevel()).isEqualTo(RiskLevel.BLOCKED);
        assertThat(aliceWallet.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(bobWallet.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void highRiskPaymentIsStillProcessedButFlagged() {
        when(fraud.evaluate(any())).thenReturn(new RiskAssessment(RiskLevel.HIGH,
                List.of(new RuleResult("AMOUNT_NEAR_LIMIT", RiskLevel.HIGH, "Close to limit"))));

        PaymentProcessor.ExecutionOutcome outcome = processor.execute(100L);

        assertThat(outcome.succeeded()).isTrue();
        assertThat(tx.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(aliceWallet.getBalance()).isEqualByComparingTo("750.00");
    }

    @Test
    void inactiveReceiverFailsWithInvalidTransaction() {
        bob.setStatus(UserStatus.INACTIVE);

        PaymentProcessor.ExecutionOutcome outcome = processor.execute(100L);

        assertThat(outcome.failure()).isInstanceOf(InvalidTransactionException.class);
        assertThat(outcome.failure().getMessage()).contains("Receiver account is inactive");
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(aliceWallet.getBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void frozenSenderWalletFails() {
        aliceWallet.setStatus(WalletStatus.FROZEN);

        PaymentProcessor.ExecutionOutcome outcome = processor.execute(100L);

        assertThat(outcome.failure()).isInstanceOf(InvalidTransactionException.class);
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.FAILED);
    }

    @Test
    void topUpCreditsOnlyTheUsersWallet() {
        Transaction topUp = Transaction.builder().id(100L).transactionReference("TXN-TEST-0002").sender(alice)
                .receiver(alice).amount(new BigDecimal("500.00")).currency("INR")
                .transactionType(TransactionType.ADD_MONEY).status(TransactionStatus.PROCESSING)
                .idempotencyKey("k2").build();
        when(txRepo.findWithPartiesById(100L)).thenReturn(Optional.of(topUp));

        PaymentProcessor.ExecutionOutcome outcome = processor.execute(100L);

        assertThat(outcome.succeeded()).isTrue();
        assertThat(topUp.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(aliceWallet.getBalance()).isEqualByComparingTo("1500.00");
        assertThat(bobWallet.getBalance()).isEqualByComparingTo("100.00");
        verify(fraud, never()).evaluate(any());
    }

    @Test
    void startProcessingOnlySucceedsForTheCallerThatWinsTheTransition() {
        when(txRepo.transition(eq(100L), eq(TransactionStatus.PENDING), eq(TransactionStatus.PROCESSING),
                any(Instant.class))).thenReturn(1, 0);

        assertThat(processor.startProcessing(100L)).isTrue();
        assertThat(processor.startProcessing(100L)).isFalse();
        verify(events).record(eq(100L), eq(TransactionEventType.PROCESSING_STARTED), eq(EventStatus.SUCCESS), any());
    }

    @Test
    void markFailedRescuesAStuckProcessingTransaction() {
        processor.markFailed(100L, "Internal processing error");

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(tx.getFailureReason()).isEqualTo("Internal processing error");
    }

    @Test
    void markFailedIgnoresAlreadyFinishedTransactions() {
        tx.setStatus(TransactionStatus.SUCCESS);

        processor.markFailed(100L, "late failure");

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
    }
}
