package com.smartpay.platform.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyStat(LocalDate date, long count, BigDecimal amount) {
}
