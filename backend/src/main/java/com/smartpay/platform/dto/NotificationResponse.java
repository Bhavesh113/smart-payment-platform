package com.smartpay.platform.dto;

import com.smartpay.platform.entity.NotificationStatus;
import com.smartpay.platform.entity.NotificationType;
import java.time.Instant;

public record NotificationResponse(Long id, Long transactionId, String transactionReference, String message,
                                   NotificationType type, NotificationStatus status, Instant createdAt) {
}
