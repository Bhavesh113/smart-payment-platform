package com.smartpay.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, @DefaultValue("60") long expirationMinutes) {
}
