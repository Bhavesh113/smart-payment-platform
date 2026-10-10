package com.smartpay.platform.mapper;

import com.smartpay.platform.dto.NotificationResponse;
import com.smartpay.platform.dto.TransactionEventResponse;
import com.smartpay.platform.dto.TransactionResponse;
import com.smartpay.platform.entity.Notification;
import com.smartpay.platform.entity.Transaction;
import com.smartpay.platform.entity.TransactionEvent;
import com.smartpay.platform.entity.TransactionType;

public final class TransactionMapper {

    private TransactionMapper() {
    }

    /** viewerId may be null (admin views), in which case no direction is computed. */
    public static TransactionResponse toResponse(Transaction t, Long viewerId) {
        return new TransactionResponse(
                t.getId(), t.getTransactionReference(),
                t.getSender().getId(), t.getSender().getName(), t.getSender().getEmail(),
                t.getReceiver().getId(), t.getReceiver().getName(), t.getReceiver().getEmail(),
                t.getAmount(), t.getCurrency(), t.getTransactionType(), t.getStatus(), t.getRiskLevel(),
                t.getRiskReasons(), t.getDescription(), t.getFailureReason(),
                direction(t, viewerId), t.getCreatedAt(), t.getUpdatedAt());
    }

    public static TransactionEventResponse toResponse(TransactionEvent e) {
        return new TransactionEventResponse(e.getId(), e.getEventType(), e.getEventStatus(), e.getDetails(),
                e.getCreatedAt());
    }

    public static NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getTransaction().getId(),
                n.getTransaction().getTransactionReference(), n.getMessage(), n.getType(), n.getStatus(),
                n.getCreatedAt());
    }

    private static String direction(Transaction t, Long viewerId) {
        if (viewerId == null) {
            return null;
        }
        if (t.getTransactionType() == TransactionType.ADD_MONEY) {
            return "CREDIT";
        }
        if (viewerId.equals(t.getSender().getId())) {
            return "DEBIT";
        }
        return viewerId.equals(t.getReceiver().getId()) ? "CREDIT" : null;
    }
}
