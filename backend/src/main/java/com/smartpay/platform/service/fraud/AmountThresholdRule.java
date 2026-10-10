package com.smartpay.platform.service.fraud;

import com.smartpay.platform.config.PaymentProperties;
import com.smartpay.platform.entity.RiskLevel;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Per-transaction ceiling (BLOCKED) plus softer HIGH / MEDIUM bands below it. */
@Component
@RequiredArgsConstructor
public class AmountThresholdRule implements FraudRule {

    private static final BigDecimal HIGH_BAND = new BigDecimal("0.80");

    private final PaymentProperties properties;

    @Override
    public Optional<RuleResult> evaluate(RiskContext ctx) {
        BigDecimal amount = ctx.amount();
        BigDecimal max = properties.maxTransactionLimit();
        if (amount.compareTo(max) > 0) {
            return Optional.of(new RuleResult("AMOUNT_ABOVE_LIMIT", RiskLevel.BLOCKED,
                    "Amount exceeds the per-transaction limit of " + max.toPlainString()));
        }
        if (amount.compareTo(max.multiply(HIGH_BAND)) >= 0) {
            return Optional.of(new RuleResult("AMOUNT_NEAR_LIMIT", RiskLevel.HIGH,
                    "Amount is close to the per-transaction limit"));
        }
        if (amount.compareTo(properties.mediumRiskThreshold()) >= 0) {
            return Optional.of(new RuleResult("HIGH_VALUE", RiskLevel.MEDIUM, "High-value transfer"));
        }
        return Optional.empty();
    }
}
