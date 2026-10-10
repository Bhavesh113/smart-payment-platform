package com.smartpay.platform.repository;

import com.smartpay.platform.entity.TransactionStatus;
import java.math.BigDecimal;

/** Result row of the GROUP BY status query (used by analytics). */
public record StatusStat(TransactionStatus status, Long count, BigDecimal totalAmount) {
}
