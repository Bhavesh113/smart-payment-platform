package com.smartpay.platform.service.fraud;

import com.smartpay.platform.entity.RiskLevel;
import com.smartpay.platform.entity.UserStatus;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AccountStatusRule implements FraudRule {

    @Override
    public Optional<RuleResult> evaluate(RiskContext ctx) {
        if (ctx.sender().getStatus() != UserStatus.ACTIVE) {
            return Optional.of(new RuleResult("SENDER_NOT_ACTIVE", RiskLevel.BLOCKED,
                    "Sender account is " + ctx.sender().getStatus().name().toLowerCase()));
        }
        if (ctx.receiver().getStatus() != UserStatus.ACTIVE) {
            return Optional.of(new RuleResult("RECEIVER_NOT_ACTIVE", RiskLevel.BLOCKED,
                    "Receiver account is " + ctx.receiver().getStatus().name().toLowerCase()));
        }
        return Optional.empty();
    }
}
