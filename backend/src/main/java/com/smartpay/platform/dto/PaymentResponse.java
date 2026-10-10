package com.smartpay.platform.dto;

/** idempotentReplay is true when the Idempotency-Key matched an earlier request and no new payment was created. */
public record PaymentResponse(TransactionResponse transaction, boolean idempotentReplay, String message) {
}
