package com.smartpay.platform.controller;

import com.smartpay.platform.dto.ApiResponse;
import com.smartpay.platform.dto.PageResponse;
import com.smartpay.platform.dto.PaymentRequest;
import com.smartpay.platform.dto.PaymentResponse;
import com.smartpay.platform.dto.TransactionDetailResponse;
import com.smartpay.platform.dto.TransactionResponse;
import com.smartpay.platform.entity.TransactionStatus;
import com.smartpay.platform.security.UserPrincipal;
import com.smartpay.platform.service.PaymentService;
import com.smartpay.platform.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Send money and look up payments")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;
    private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Send money to another user",
            description = """
                    Runs the full pipeline: validation, idempotency check, status check, balance check, fraud/risk rules,
                    money movement and status update. Returns 201 for a new payment and 200 (with the original transaction
                    and header Idempotent-Replayed: true) when the Idempotency-Key was already used.""")
    public ResponseEntity<ApiResponse<PaymentResponse>> send(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Unique key for this payment, e.g. a UUID", example = "TXN-123456")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {
        return toEntity(paymentService.sendMoney(principal.getId(), request, idempotencyKey));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a payment by id, including its event timeline")
    public ApiResponse<TransactionDetailResponse> byId(@AuthenticationPrincipal UserPrincipal principal,
                                                       @PathVariable Long id) {
        return ApiResponse.ok("Payment fetched",
                transactionService.getDetailById(id, principal.getId(), principal.isAdmin()));
    }

    @GetMapping("/reference/{reference}")
    @Operation(summary = "Get a payment by its transaction reference")
    public ApiResponse<TransactionDetailResponse> byReference(@AuthenticationPrincipal UserPrincipal principal,
                                                              @PathVariable String reference) {
        return ApiResponse.ok("Payment fetched",
                transactionService.getDetailByReference(reference, principal.getId(), principal.isAdmin()));
    }

    @GetMapping("/history")
    @Operation(summary = "My payment history (paginated, filterable, sortable)")
    public ApiResponse<PageResponse<TransactionResponse>> history(
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
        return ApiResponse.ok("History fetched",
                transactionService.search(principal.getId(), filter, page, size, sortBy, direction));
    }

    static ResponseEntity<ApiResponse<PaymentResponse>> toEntity(PaymentResponse response) {
        ResponseEntity.BodyBuilder builder = ResponseEntity
                .status(response.idempotentReplay() ? HttpStatus.OK : HttpStatus.CREATED);
        if (response.idempotentReplay()) {
            builder.header("Idempotent-Replayed", "true");
        }
        return builder.body(ApiResponse.ok(response.message(), response));
    }
}
