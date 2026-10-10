package com.smartpay.platform.event;

import com.smartpay.platform.entity.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentFailedEvent(String transactionReference, Long transactionId, Long senderId, Long receiverId, BigDecimal amount, String currency, TransactionType transactionType, String failureReason, Instant occurredAt) {
}
