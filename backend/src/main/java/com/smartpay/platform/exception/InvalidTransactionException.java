package com.smartpay.platform.exception;

public class InvalidTransactionException extends PaymentException {
    public InvalidTransactionException(String message) {
        this(message, null);
    }

    public InvalidTransactionException(String message, String transactionReference) {
        super(message, transactionReference);
    }
}
