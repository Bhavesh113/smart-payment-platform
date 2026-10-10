package com.smartpay.platform.dto;

public record LoginResponse(String token, String tokenType, long expiresInSeconds, UserResponse user) {
}
