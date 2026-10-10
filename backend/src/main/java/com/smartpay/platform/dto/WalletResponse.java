package com.smartpay.platform.dto;

import com.smartpay.platform.entity.WalletStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record WalletResponse(Long id, Long userId, BigDecimal balance, String currency, WalletStatus status,
                             Instant updatedAt) {
}
