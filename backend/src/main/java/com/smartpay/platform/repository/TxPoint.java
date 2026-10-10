package com.smartpay.platform.repository;

import com.smartpay.platform.entity.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** Lightweight projection used to build the "transactions by date" series. */
public record TxPoint(Instant createdAt, TransactionStatus status, BigDecimal amount) {
}
