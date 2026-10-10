package com.smartpay.platform.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Rates are percentages of all transactions in scope. Amounts only count SUCCESS transactions. */
public record AnalyticsResponse(
        long totalTransactions,
        long successfulTransactions,
        long failedTransactions,
        long pendingTransactions,
        BigDecimal totalAmount,
        long todayTransactions,
        BigDecimal todayAmount,
        double successRate,
        double failureRate,
        Map<String, Long> transactionsByStatus,
        List<DailyStat> transactionsByDate) {
}
