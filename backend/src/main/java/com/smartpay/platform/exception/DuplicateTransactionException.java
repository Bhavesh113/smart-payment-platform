package com.smartpay.platform.exception;

/** Same Idempotency-Key reused with a different payload. */
public class DuplicateTransactionException extends PaymentException {
    public DuplicateTransactionException(String message, String transactionReference) {
        super(message, transactionReference);
    }
}
