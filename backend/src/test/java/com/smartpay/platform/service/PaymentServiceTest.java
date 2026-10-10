package com.smartpay.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartpay.platform.config.PaymentProperties;
import com.smartpay.platform.dto.AddMoneyRequest;
import com.smartpay.platform.dto.PaymentRequest;
import com.smartpay.platform.dto.PaymentResponse;
import com.smartpay.platform.dto.TransactionResponse;
import com.smartpay.platform.entity.*;
import com.smartpay.platform.exception.DuplicateTransactionException;
import com.smartpay.platform.exception.InsufficientBalanceException;
import com.smartpay.platform.exception.InvalidTransactionException;
import com.smartpay.platform.exception.ResourceNotFoundException;
import com.smartpay.platform.repository.TransactionRepository;
import com.smartpay.platform.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class PaymentServiceTest {

    private final UserRepository userRepo = mock(UserRepository.class);
    private final TransactionRepository txRepo = mock(TransactionRepository.class);
    private final PaymentProcessor processor = mock(PaymentProcessor.class);
    private final TransactionService transactionService = mock(TransactionService.class);
    private final PaymentProperties props = new PaymentProperties(new BigDecimal("50000"), new BigDecimal("100000"),
            5, 10, new BigDecimal("20000"), 3, "INR", new BigDecimal("100000"), "Asia/Kolkata");

    private PaymentService service;
    private User alice;
    private User bob;
    private Transaction pending;

    @BeforeEach
    void setUp() {
        service = new PaymentService(userRepo, new IdempotencyService(txRepo), processor, transactionService, props);
        alice = User.builder().id(1L).name("Alice").email("alice@example.com").role(Role.USER).status(UserStatus.ACTIVE).build();
        bob = User.builder().id(2L).name("Bob").email("bob@example.com").role(Role.USER).status(UserStatus.ACTIVE).build();
        pending = Transaction.builder().id(100L).transactionReference("TXN-TEST-0001").sender(alice).receiver(bob)
                .amount(new BigDecimal("250.00")).currency("INR").transactionType(TransactionType.TRANSFER)
                .status(TransactionStatus.PENDING).idempotencyKey("K1").build();

        when(userRepo.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepo.findByEmail("bob@example.com")).thenReturn(Optional.of(bob));
        when(txRepo.findWithPartiesBySenderIdAndIdempotencyKey(1L, "K1")).thenReturn(Optional.empty());
    }

    private static PaymentRequest request(String amount) {
        return new PaymentRequest("bob@example.com", new BigDecimal(amount), "Lunch");
    }

    private static TransactionResponse response(TransactionStatus status, String failure) {
        return new TransactionResponse(100L, "TXN-TEST-0001", 1L, "Alice", "alice@example.com", 2L, "Bob",
                "bob@example.com", new BigDecimal("250.00"), "INR", TransactionType.TRANSFER, status, RiskLevel.LOW,
                null, "Lunch", failure, "DEBIT", null, null);
    }

    private void stubHappyPipeline() {
        when(processor.createPending(eq(1L), eq(2L), any(), any(), eq("K1"), eq(TransactionType.TRANSFER)))
                .thenReturn(pending);
        when(processor.startProcessing(100L)).thenReturn(true);
        when(processor.execute(100L)).thenReturn(new PaymentProcessor.ExecutionOutcome(null));
        when(transactionService.getResponse(100L, 1L)).thenReturn(response(TransactionStatus.SUCCESS, null));
    }

    @Test
    void successfulPaymentRunsEveryStageInOrder() {
        stubHappyPipeline();

        PaymentResponse result = service.sendMoney(1L, request("250.00"), "K1");

        assertThat(result.idempotentReplay()).isFalse();
        assertThat(result.transaction().status()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(result.message()).isEqualTo("Payment processed successfully");
        var order = org.mockito.Mockito.inOrder(processor);
        order.verify(processor).createPending(eq(1L), eq(2L), any(), any(), eq("K1"), eq(TransactionType.TRANSFER));
        order.verify(processor).startProcessing(100L);
        order.verify(processor).execute(100L);
    }

    @Test
    void duplicateIdempotencyKeyReturnsExistingTransactionWithoutCreatingAnother() {
        when(txRepo.findWithPartiesBySenderIdAndIdempotencyKey(1L, "K1")).thenReturn(Optional.of(pending));
        when(transactionService.getResponse(100L, 1L)).thenReturn(response(TransactionStatus.SUCCESS, null));

        PaymentResponse result = service.sendMoney(1L, request("250.00"), "K1");

        assertThat(result.idempotentReplay()).isTrue();
        assertThat(result.transaction().id()).isEqualTo(100L);
        assertThat(result.message()).startsWith("Duplicate request detected");
        verify(processor, never()).createPending(any(), any(), any(), any(), any(), any());
        verify(processor, never()).execute(any());
    }

    @Test
    void sameKeyWithDifferentAmountIsRejectedAsConflict() {
        when(txRepo.findWithPartiesBySenderIdAndIdempotencyKey(1L, "K1")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.sendMoney(1L, request("999.00"), "K1"))
                .isInstanceOf(DuplicateTransactionException.class)
                .hasMessageContaining("different payment");
        verify(processor, never()).createPending(any(), any(), any(), any(), any(), any());
    }

    @Test
    void concurrentDuplicateLosingTheRaceIsTreatedAsReplay() {
        when(txRepo.findWithPartiesBySenderIdAndIdempotencyKey(1L, "K1"))
                .thenReturn(Optional.empty(), Optional.of(pending));
        when(processor.createPending(any(), any(), any(), any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("uk_tx_sender_idempotency"));
        when(transactionService.getResponse(100L, 1L)).thenReturn(response(TransactionStatus.PROCESSING, null));

        PaymentResponse result = service.sendMoney(1L, request("250.00"), "K1");

        assertThat(result.idempotentReplay()).isTrue();
        verify(processor, never()).execute(any());
    }

    @Test
    void unknownReceiverIsRejectedBeforeAnythingIsCreated() {
        when(userRepo.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendMoney(1L,
                new PaymentRequest("ghost@example.com", new BigDecimal("10"), null), "K1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Receiver not found");
        verify(processor, never()).createPending(any(), any(), any(), any(), any(), any());
    }

    @Test
    void businessFailureFromProcessorIsRethrownToTheCaller() {
        when(processor.createPending(any(), any(), any(), any(), eq("K1"), any())).thenReturn(pending);
        when(processor.startProcessing(100L)).thenReturn(true);
        when(processor.execute(100L)).thenReturn(new PaymentProcessor.ExecutionOutcome(
                new InsufficientBalanceException("Insufficient wallet balance", "TXN-TEST-0001")));

        assertThatThrownBy(() -> service.sendMoney(1L, request("250.00"), "K1"))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Insufficient wallet balance");
        verify(processor, never()).markFailed(any(), anyString());
    }

    @Test
    void unexpectedErrorMarksTransactionFailedSoItIsNeverStuck() {
        when(processor.createPending(any(), any(), any(), any(), eq("K1"), any())).thenReturn(pending);
        when(processor.startProcessing(100L)).thenReturn(true);
        when(processor.execute(100L)).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.sendMoney(1L, request("250.00"), "K1"))
                .isInstanceOf(IllegalStateException.class);
        verify(processor).markFailed(eq(100L), anyString());
    }

    @Test
    void missingIdempotencyKeyIsRejected() {
        assertThatThrownBy(() -> service.sendMoney(1L, request("10"), "  "))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Idempotency-Key");
    }

    @Test
    void addMoneyAboveConfiguredLimitIsRejected() {
        assertThatThrownBy(() -> service.addMoney(1L, new AddMoneyRequest(new BigDecimal("100000.01")), "K1"))
                .isInstanceOf(InvalidTransactionException.class);
        verify(processor, never()).createPending(any(), any(), any(), any(), any(), any());
    }
}
