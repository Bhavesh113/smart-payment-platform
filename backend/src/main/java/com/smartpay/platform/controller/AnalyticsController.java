package com.smartpay.platform.controller;

import com.smartpay.platform.dto.AnalyticsResponse;
import com.smartpay.platform.dto.ApiResponse;
import com.smartpay.platform.security.UserPrincipal;
import com.smartpay.platform.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/me")
    @Operation(summary = "My transaction analytics", description = "Totals, rates, today's activity, status split and a daily series.")
    public ApiResponse<AnalyticsResponse> mine(@AuthenticationPrincipal UserPrincipal principal,
                                               @RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok("Analytics fetched", analyticsService.forUser(principal.getId(), days));
    }
}
