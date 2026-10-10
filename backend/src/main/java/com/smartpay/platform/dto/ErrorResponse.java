package com.smartpay.platform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(boolean success, String message, Instant timestamp, String path,
                            Map<String, String> errors, String transactionReference) {
}
