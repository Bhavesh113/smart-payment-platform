package com.smartpay.platform.dto;

import java.util.List;

public record AdminAnalyticsResponse(
        AnalyticsResponse overview,
        long totalUsers,
        long activeUsers,
        long highRiskTransactions,
        List<TransactionResponse> recentTransactions,
        List<TransactionResponse> suspiciousTransactions) {
}
