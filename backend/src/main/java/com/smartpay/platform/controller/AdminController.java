package com.smartpay.platform.controller;

import com.smartpay.platform.dto.AdminAnalyticsResponse;
import com.smartpay.platform.dto.ApiResponse;
import com.smartpay.platform.dto.PageResponse;
import com.smartpay.platform.dto.TransactionResponse;
import com.smartpay.platform.dto.UserResponse;
import com.smartpay.platform.entity.RiskLevel;
import com.smartpay.platform.entity.TransactionStatus;
import com.smartpay.platform.service.AnalyticsService;
import com.smartpay.platform.service.TransactionService;
import com.smartpay.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Monitoring-only admin API (no create/update/delete). Protected twice: URL rule and @PreAuthorize. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Platform monitoring and analytics (ADMIN role only)")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final UserService userService;
    private final TransactionService transactionService;
    private final AnalyticsService analyticsService;

    @GetMapping("/users")
    @Operation(summary = "List users (paginated, searchable)")
    public ApiResponse<PageResponse<UserResponse>> users(@RequestParam(required = false) String search,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok("Users fetched", userService.search(search, page, size));
    }

    @GetMapping("/transactions")
    @Operation(summary = "List all transactions", description = "Same filters as the user endpoint plus riskLevel.")
    public ApiResponse<PageResponse<TransactionResponse>> transactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        var filter = new TransactionService.Filter(status, riskLevel, search, from, to);
        return ApiResponse.ok("Transactions fetched",
                transactionService.search(null, filter, page, size, sortBy, direction));
    }

    @GetMapping("/analytics")
    @Operation(summary = "Platform analytics", description = "Totals, volume, success/failure stats, recent and high-risk transactions.")
    public ApiResponse<AdminAnalyticsResponse> analytics(@RequestParam(defaultValue = "14") int days) {
        return ApiResponse.ok("Analytics fetched", analyticsService.forAdmin(days));
    }
}
