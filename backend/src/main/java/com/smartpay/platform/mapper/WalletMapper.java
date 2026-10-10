package com.smartpay.platform.mapper;

import com.smartpay.platform.dto.WalletResponse;
import com.smartpay.platform.entity.Wallet;

public final class WalletMapper {

    private WalletMapper() {
    }

    public static WalletResponse toResponse(Wallet w) {
        return new WalletResponse(w.getId(), w.getUser().getId(), w.getBalance(), w.getCurrency(), w.getStatus(),
                w.getUpdatedAt());
    }
}
