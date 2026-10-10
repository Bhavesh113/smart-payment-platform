package com.smartpay.platform.exception;

public class InsufficientBalanceException extends PaymentException {
    public InsufficientBalanceException(String message, String transactionReference) {
        super(message, transactionReference);
    }
}
