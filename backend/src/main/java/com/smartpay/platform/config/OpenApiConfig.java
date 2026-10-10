package com.smartpay.platform.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Smart Payment Transaction Platform API",
        version = "1.0.0",
        description = """
                REST API for wallet payments with idempotency protection, rule-based fraud checks,
                an explicit transaction lifecycle (PENDING -> PROCESSING -> SUCCESS/FAILED),
                Kafka-driven notifications and transaction analytics.

                How to authorize: call POST /api/auth/login, copy data.token, click "Authorize" and paste it.
                Payment and add-money calls require an Idempotency-Key header.""",
        contact = @Contact(name = "Smart Payment Platform")))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
