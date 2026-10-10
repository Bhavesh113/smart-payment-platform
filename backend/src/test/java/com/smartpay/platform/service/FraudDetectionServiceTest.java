package com.smartpay.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.smartpay.platform.config.PaymentProperties;
import com.smartpay.platform.entity.RiskLevel;
import com.smartpay.platform.entity.Role;
import com.smartpay.platform.entity.TransactionStatus;
import com.smartpay.platform.entity.TransactionType;
import com.smartpay.platform.entity.User;
import com.smartpay.platform.entity.UserStatus;
import com.smartpay.platform.repository.TransactionRepository;
import com.smartpay.platform.service.fraud.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FraudDetectionServiceTest {

    private final TransactionRepository repo = mock(TransactionRepository.class);
    private final PaymentProperties props = new PaymentProperties(new BigDecimal("50000"), new BigDecimal("100000"),
            5, 10, new BigDecimal("20000"), 3, "INR", new BigDecimal("100000"), "Asia/Kolkata");
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-07T10:00:00Z"), ZoneOffset.UTC);

    private FraudDetectionService service;
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        service = new FraudDetectionService(List.of(
                new SelfTransferRule(), new AccountStatusRule(), new AmountThresholdRule(props),
                new DailyLimitRule(repo, props, clock), new VelocityRule(repo, props, clock),
                new RepeatedReceiverRule(repo, props, clock)));
        alice = user(1L, UserStatus.ACTIVE);
        bob = user(2L, UserStatus.ACTIVE);
    }

    private static User user(Long id, UserStatus status) {
        return User.builder().id(id).name("U" + id).email("u" + id + "@example.com").role(Role.USER)
                .status(status).build();
    }

    private RiskAssessment assess(BigDecimal amount) {
        return service.evaluate(new RiskContext(alice, bob, amount, 99L));
    }

    @Test
    void ordinaryPaymentIsLowRisk() {
        RiskAssessment result = assess(new BigDecimal("500"));
        assertThat(result.level()).isEqualTo(RiskLevel.LOW);
        assertThat(result.isBlocked()).isFalse();
        assertThat(result.triggeredRules()).isEmpty();
    }

    @Test
    void highValuePaymentIsMedium() {
        assertThat(assess(new BigDecimal("25000")).level()).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    void amountNearLimitIsHighButAllowed() {
        RiskAssessment result = assess(new BigDecimal("45000"));
        assertThat(result.level()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.isBlocked()).isFalse();
    }

    @Test
    void amountAboveLimitIsBlocked() {
        RiskAssessment result = assess(new BigDecimal("50000.01"));
        assertThat(result.isBlocked()).isTrue();
        assertThat(result.triggeredRules()).extracting(RuleResult::code).contains("AMOUNT_ABOVE_LIMIT");
    }

    @Test
    void selfTransferIsBlocked() {
        RiskAssessment result = service.evaluate(new RiskContext(alice, alice, new BigDecimal("10"), 99L));
        assertThat(result.isBlocked()).isTrue();
        assertThat(result.triggeredRules()).extracting(RuleResult::code).contains("SELF_TRANSFER");
    }

    @Test
    void blockedOrInactiveSenderIsBlocked() {
        alice.setStatus(UserStatus.BLOCKED);
        assertThat(assess(new BigDecimal("10")).triggeredRules()).extracting(RuleResult::code)
                .contains("SENDER_NOT_ACTIVE");
    }

    @Test
    void inactiveReceiverIsBlocked() {
        bob.setStatus(UserStatus.INACTIVE);
        RiskAssessment result = assess(new BigDecimal("10"));
        assertThat(result.isBlocked()).isTrue();
        assertThat(result.triggeredRules()).extracting(RuleResult::code).contains("RECEIVER_NOT_ACTIVE");
    }

    @Test
    void dailyLimitExceededIsBlocked() {
        when(repo.sumBySenderAndStatusSince(eq(1L), eq(TransactionType.TRANSFER), eq(TransactionStatus.SUCCESS),
                any(Instant.class))).thenReturn(new BigDecimal("90000"));
        RiskAssessment result = assess(new BigDecimal("15000"));
        assertThat(result.isBlocked()).isTrue();
        assertThat(result.triggeredRules()).extracting(RuleResult::code).contains("DAILY_LIMIT_EXCEEDED");
    }

    @Test
    void dailyTotalExactlyAtLimitIsAllowed() {
        when(repo.sumBySenderAndStatusSince(eq(1L), eq(TransactionType.TRANSFER), eq(TransactionStatus.SUCCESS),
                any(Instant.class))).thenReturn(new BigDecimal("60000"));
        assertThat(assess(new BigDecimal("40000")).triggeredRules()).extracting(RuleResult::code)
                .doesNotContain("DAILY_LIMIT_EXCEEDED");
    }

    @Test
    void tooManyRecentTransactionsIsBlocked() {
        when(repo.countRecentBySender(eq(1L), eq(TransactionType.TRANSFER), any(Instant.class), any()))
                .thenReturn(5L);
        RiskAssessment result = assess(new BigDecimal("10"));
        assertThat(result.isBlocked()).isTrue();
        assertThat(result.triggeredRules()).extracting(RuleResult::code).contains("VELOCITY_EXCEEDED");
    }

    @Test
    void oneAttemptBelowVelocityLimitRaisesWarning() {
        when(repo.countRecentBySender(eq(1L), eq(TransactionType.TRANSFER), any(Instant.class), any()))
                .thenReturn(4L);
        RiskAssessment result = assess(new BigDecimal("10"));
        assertThat(result.level()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(result.isBlocked()).isFalse();
    }

    @Test
    void repeatedPaymentsToSameReceiverAreHighRisk() {
        when(repo.countRecentBetween(eq(1L), eq(2L), eq(TransactionType.TRANSFER), any(Instant.class), any()))
                .thenReturn(3L);
        RiskAssessment result = assess(new BigDecimal("10"));
        assertThat(result.level()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.triggeredRules()).extracting(RuleResult::code).contains("REPEATED_RECEIVER");
    }

    @Test
    void highestSeverityWinsWhenSeveralRulesTrigger() {
        when(repo.countRecentBySender(eq(1L), eq(TransactionType.TRANSFER), any(Instant.class), any()))
                .thenReturn(4L);
        RiskAssessment result = assess(new BigDecimal("45000"));
        assertThat(result.triggeredRules()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(result.level()).isEqualTo(RiskLevel.HIGH);
    }
}
