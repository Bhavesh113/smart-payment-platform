package com.smartpay.platform.event;

import com.smartpay.platform.entity.NotificationType;
import java.time.Instant;

public record NotificationEvent(Long userId, Long transactionId, String transactionReference,
                                NotificationType type, String message, Instant occurredAt) {
}
