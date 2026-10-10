package com.smartpay.platform.service;

import com.smartpay.platform.entity.RiskLevel;
import com.smartpay.platform.service.fraud.FraudRule;
import com.smartpay.platform.service.fraud.RiskAssessment;
import com.smartpay.platform.service.fraud.RiskContext;
import com.smartpay.platform.service.fraud.RuleResult;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Runs every FraudRule and keeps the highest severity. New rules are added by creating a @Component that
 * implements FraudRule: this class never changes (open/closed principle).
 */
@Service
@Slf4j
public class FraudDetectionService {

    private final List<FraudRule> rules;

    public FraudDetectionService(List<FraudRule> rules) {
        this.rules = rules;
    }

    public RiskAssessment evaluate(RiskContext context) {
        List<RuleResult> triggered = rules.stream()
                .map(rule -> rule.evaluate(context))
                .flatMap(java.util.Optional::stream)
                .toList();
        RiskLevel level = triggered.stream()
                .map(RuleResult::level)
                .max(Comparator.naturalOrder())
                .orElse(RiskLevel.LOW);
        RiskAssessment assessment = new RiskAssessment(level, triggered);
        if (level != RiskLevel.LOW) {
            log.warn("Risk rules triggered for tx {}: level={} rules={}", context.transactionId(), level,
                    triggered.stream().map(RuleResult::code).toList());
        }
        return assessment;
    }
}
