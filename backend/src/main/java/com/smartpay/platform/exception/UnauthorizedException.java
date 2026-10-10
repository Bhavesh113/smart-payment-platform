package com.smartpay.platform.exception;

/** The caller is authenticated but not allowed to touch this resource (mapped to HTTP 403). */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
