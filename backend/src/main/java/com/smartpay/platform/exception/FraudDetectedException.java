package com.smartpay.platform.exception;

public class FraudDetectedException extends PaymentException {
    public FraudDetectedException(String message, String transactionReference) {
        super(message, transactionReference);
    }
}
