package com.smartpay.platform.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Business limits used by the fraud/risk engine. Bound from payment.* properties. */
@ConfigurationProperties(prefix = "payment")
public record PaymentProperties(
        @DefaultValue("50000") BigDecimal maxTransactionLimit,
        @DefaultValue("100000") BigDecimal dailyLimit,
        @DefaultValue("5") int velocityLimit,
        @DefaultValue("10") int velocityWindowMinutes,
        @DefaultValue("20000") BigDecimal mediumRiskThreshold,
        @DefaultValue("3") int repeatReceiverLimit,
        @DefaultValue("INR") String currency,
        @DefaultValue("100000") BigDecimal maxAddMoney,
        @DefaultValue("Asia/Kolkata") String timezone) {
}
