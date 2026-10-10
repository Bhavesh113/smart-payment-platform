package com.smartpay.platform.dto;

import com.smartpay.platform.entity.EventStatus;
import com.smartpay.platform.entity.TransactionEventType;
import java.time.Instant;

public record TransactionEventResponse(Long id, TransactionEventType eventType, EventStatus eventStatus,
                                       String details, Instant createdAt) {
}
