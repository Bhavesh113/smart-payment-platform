package com.smartpay.platform.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class WalletTest {

    private Wallet walletWith(String balance) {
        return Wallet.builder().balance(new BigDecimal(balance)).currency("INR").status(WalletStatus.ACTIVE).build();
    }

    @Test
    void creditIncreasesBalance() {
        Wallet wallet = walletWith("100.00");
        wallet.credit(new BigDecimal("25.50"));
        assertThat(wallet.getBalance()).isEqualByComparingTo("125.50");
    }

    @Test
    void debitDecreasesBalance() {
        Wallet wallet = walletWith("100.00");
        wallet.debit(new BigDecimal("40.00"));
        assertThat(wallet.getBalance()).isEqualByComparingTo("60.00");
    }

    @Test
    void debitCanEmptyTheWalletButNeverGoNegative() {
        Wallet wallet = walletWith("50.00");
        wallet.debit(new BigDecimal("50.00"));
        assertThat(wallet.getBalance()).isEqualByComparingTo("0");
        assertThatThrownBy(() -> wallet.debit(new BigDecimal("0.01"))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsNonPositiveAmounts() {
        Wallet wallet = walletWith("10.00");
        assertThatThrownBy(() -> wallet.credit(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> wallet.debit(new BigDecimal("-5"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void canCoverComparesAgainstBalance() {
        Wallet wallet = walletWith("10.00");
        assertThat(wallet.canCover(new BigDecimal("10.00"))).isTrue();
        assertThat(wallet.canCover(new BigDecimal("10.01"))).isFalse();
    }
}
