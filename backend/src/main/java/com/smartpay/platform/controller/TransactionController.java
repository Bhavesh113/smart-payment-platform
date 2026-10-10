package com.smartpay.platform.controller;

import com.smartpay.platform.dto.ApiResponse;
import com.smartpay.platform.dto.PageResponse;
import com.smartpay.platform.dto.TransactionDetailResponse;
import com.smartpay.platform.dto.TransactionResponse;
import com.smartpay.platform.entity.TransactionStatus;
import com.smartpay.platform.security.UserPrincipal;
import com.smartpay.platform.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transactions where I am sender or receiver (includes wallet top-ups)")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    @Operation(summary = "List my transactions",
            description = "Filter by status, free-text search (reference, description, name, email) and date range. sortBy: createdAt, amount, status, riskLevel.")
    public ApiResponse<PageResponse<TransactionResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        var filter = new TransactionService.Filter(status, null, search, from, to);
        return ApiResponse.ok("Transactions fetched",
                transactionService.search(principal.getId(), filter, page, size, sortBy, direction));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "List my transactions with a given status")
    public ApiResponse<PageResponse<TransactionResponse>> byStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable TransactionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var filter = new TransactionService.Filter(status, null, null, null, null);
        return ApiResponse.ok("Transactions fetched",
                transactionService.search(principal.getId(), filter, page, size, "createdAt", "desc"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Transaction details with event timeline")
    public ApiResponse<TransactionDetailResponse> byId(@AuthenticationPrincipal UserPrincipal principal,
                                                       @PathVariable Long id) {
        return ApiResponse.ok("Transaction fetched",
                transactionService.getDetailById(id, principal.getId(), principal.isAdmin()));
    }
}
