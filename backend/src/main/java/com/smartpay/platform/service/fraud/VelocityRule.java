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

/** Too many transfer attempts in a short window: BLOCKED at the limit, MEDIUM one attempt before it. */
@Component
@RequiredArgsConstructor
public class VelocityRule implements FraudRule {

    private final TransactionRepository transactionRepository;
    private final PaymentProperties properties;
    private final Clock clock;

    @Override
    public Optional<RuleResult> evaluate(RiskContext ctx) {
        Instant since = Instant.now(clock).minus(Duration.ofMinutes(properties.velocityWindowMinutes()));
        long recent = transactionRepository.countRecentBySender(
                ctx.sender().getId(), TransactionType.TRANSFER, since, ctx.transactionId());
        int limit = properties.velocityLimit();
        if (recent >= limit) {
            return Optional.of(new RuleResult("VELOCITY_EXCEEDED", RiskLevel.BLOCKED,
                    "Too many transactions (" + recent + ") in the last " + properties.velocityWindowMinutes()
                            + " minutes"));
        }
        if (recent >= limit - 1) {
            return Optional.of(new RuleResult("VELOCITY_WARNING", RiskLevel.MEDIUM,
                    "Rapid transaction activity detected"));
        }
        return Optional.empty();
    }
}
