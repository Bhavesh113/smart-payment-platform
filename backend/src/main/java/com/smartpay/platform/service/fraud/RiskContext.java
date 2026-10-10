package com.smartpay.platform.service.fraud;

import com.smartpay.platform.entity.User;
import java.math.BigDecimal;

/** Everything a rule needs to judge one transfer. transactionId is excluded from history counts. */
public record RiskContext(User sender, User receiver, BigDecimal amount, Long transactionId) {
}
