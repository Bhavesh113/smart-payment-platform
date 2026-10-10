package com.smartpay.platform.controller;

import com.smartpay.platform.dto.ApiResponse;
import com.smartpay.platform.dto.NotificationResponse;
import com.smartpay.platform.security.UserPrincipal;
import com.smartpay.platform.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notifications created asynchronously by the Kafka consumers")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "My most recent notifications")
    public ApiResponse<List<NotificationResponse>> recent(@AuthenticationPrincipal UserPrincipal principal,
                                                          @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok("Notifications fetched", notificationService.recentForUser(principal.getId(), limit));
    }
}
