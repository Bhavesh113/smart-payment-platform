package com.smartpay.platform.service.fraud;

import java.util.Optional;

/** One independent risk rule. Returns a result only when the rule is triggered. */
public interface FraudRule {

    Optional<RuleResult> evaluate(RiskContext context);
}
