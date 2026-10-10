package com.smartpay.platform.controller;

import com.smartpay.platform.dto.AddMoneyRequest;
import com.smartpay.platform.dto.ApiResponse;
import com.smartpay.platform.dto.PaymentResponse;
import com.smartpay.platform.dto.WalletResponse;
import com.smartpay.platform.security.UserPrincipal;
import com.smartpay.platform.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    @Operation(summary = "Get my wallet and balance")
    public ApiResponse<WalletResponse> wallet(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Wallet fetched", walletService.getWallet(principal.getId()));
    }

    @PostMapping("/add-money")
    @Operation(summary = "Add money to my wallet",
            description = "Idempotent: repeating the same Idempotency-Key returns the original top-up instead of adding money twice.")
    public ResponseEntity<ApiResponse<PaymentResponse>> addMoney(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Unique key for this request, e.g. a UUID", example = "TOPUP-123456")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AddMoneyRequest request) {
        return PaymentController.toEntity(walletService.addMoney(principal.getId(), request, idempotencyKey));
    }
}
