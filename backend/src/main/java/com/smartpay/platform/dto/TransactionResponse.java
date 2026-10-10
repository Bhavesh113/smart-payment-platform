package com.smartpay.platform.dto;

import com.smartpay.platform.entity.RiskLevel;
import com.smartpay.platform.entity.TransactionStatus;
import com.smartpay.platform.entity.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;

/** direction is relative to the viewer: DEBIT (money left my wallet) or CREDIT (money arrived). */
public record TransactionResponse(
        Long id,
        String transactionReference,
        Long senderId,
        String senderName,
        String senderEmail,
        Long receiverId,
        String receiverName,
        String receiverEmail,
        BigDecimal amount,
        String currency,
        TransactionType transactionType,
        TransactionStatus status,
        RiskLevel riskLevel,
        String riskReasons,
        String description,
        String failureReason,
        String direction,
        Instant createdAt,
        Instant updatedAt) {
}
