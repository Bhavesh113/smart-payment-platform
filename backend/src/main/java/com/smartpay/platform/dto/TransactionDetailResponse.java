package com.smartpay.platform.dto;

import java.util.List;

public record TransactionDetailResponse(TransactionResponse transaction, List<TransactionEventResponse> events) {
}
