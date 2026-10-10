package com.smartpay.platform.service;

import com.smartpay.platform.dto.AddMoneyRequest;
import com.smartpay.platform.dto.PaymentResponse;
import com.smartpay.platform.dto.WalletResponse;
import com.smartpay.platform.exception.ResourceNotFoundException;
import com.smartpay.platform.mapper.WalletMapper;
import com.smartpay.platform.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final PaymentService paymentService;

    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .map(WalletMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    /** Top-ups are recorded as ADD_MONEY transactions so they appear in history and are idempotent too. */
    public PaymentResponse addMoney(Long userId, AddMoneyRequest request, String idempotencyKey) {
        return paymentService.addMoney(userId, request, idempotencyKey);
    }
}
