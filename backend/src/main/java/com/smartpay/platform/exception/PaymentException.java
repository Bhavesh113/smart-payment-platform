package com.smartpay.platform.exception;

import lombok.Getter;

/** Base type for business failures that relate to a payment; carries the reference when one exists. */
@Getter
public abstract class PaymentException extends RuntimeException {

    private final String transactionReference;

    protected PaymentException(String message, String transactionReference) {
        super(message);
        this.transactionReference = transactionReference;
    }
}
