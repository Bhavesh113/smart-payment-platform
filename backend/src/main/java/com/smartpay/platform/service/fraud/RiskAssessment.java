package com.smartpay.platform.service.fraud;

import com.smartpay.platform.entity.RiskLevel;
import java.util.List;
import java.util.stream.Collectors;

public record RiskAssessment(RiskLevel level, List<RuleResult> triggeredRules) {

    public boolean isBlocked() {
        return level == RiskLevel.BLOCKED;
    }

    public String summary() {
        if (triggeredRules.isEmpty()) {
            return "No risk rules triggered";
        }
        return triggeredRules.stream().map(RuleResult::message).collect(Collectors.joining("; "));
    }
}
