package com.smartpay.platform.service.fraud;

import com.smartpay.platform.entity.RiskLevel;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SelfTransferRule implements FraudRule {

    @Override
    public Optional<RuleResult> evaluate(RiskContext ctx) {
        if (ctx.sender().getId().equals(ctx.receiver().getId())) {
            return Optional.of(new RuleResult("SELF_TRANSFER", RiskLevel.BLOCKED,
                    "Sender and receiver cannot be the same user"));
        }
        return Optional.empty();
    }
}
