package com.smartpay.platform.service.fraud;

import com.smartpay.platform.config.PaymentProperties;
import com.smartpay.platform.entity.RiskLevel;
import com.smartpay.platform.entity.TransactionType;
import com.smartpay.platform.repository.TransactionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Suspicious frequency pattern: repeated payments to one receiver in the window are flagged HIGH (not blocked). */
@Component
@RequiredArgsConstructor
public class RepeatedReceiverRule implements FraudRule {

    private final TransactionRepository transactionRepository;
    private final PaymentProperties properties;
    private final Clock clock;

    @Override
    public Optional<RuleResult> evaluate(RiskContext ctx) {
        Instant since = Instant.now(clock).minus(Duration.ofMinutes(properties.velocityWindowMinutes()));
        long toSameReceiver = transactionRepository.countRecentBetween(ctx.sender().getId(), ctx.receiver().getId(),
                TransactionType.TRANSFER, since, ctx.transactionId());
        if (toSameReceiver >= properties.repeatReceiverLimit()) {
            return Optional.of(new RuleResult("REPEATED_RECEIVER", RiskLevel.HIGH,
                    "Repeated payments to the same receiver in a short time"));
        }
        return Optional.empty();
    }
}
