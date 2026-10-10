package com.smartpay.platform.service.fraud;

import com.smartpay.platform.entity.RiskLevel;

public record RuleResult(String code, RiskLevel level, String message) {
}
