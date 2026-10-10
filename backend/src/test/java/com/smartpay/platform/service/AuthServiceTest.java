package com.smartpay.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartpay.platform.config.PaymentProperties;
import com.smartpay.platform.dto.LoginRequest;
import com.smartpay.platform.dto.LoginResponse;
import com.smartpay.platform.dto.RegisterRequest;
import com.smartpay.platform.dto.UserResponse;
import com.smartpay.platform.entity.Role;
import com.smartpay.platform.entity.User;
import com.smartpay.platform.entity.UserStatus;
import com.smartpay.platform.entity.Wallet;
import com.smartpay.platform.exception.DuplicateResourceException;
import com.smartpay.platform.repository.UserRepository;
import com.smartpay.platform.repository.WalletRepository;
import com.smartpay.platform.security.JwtService;
import com.smartpay.platform.security.UserPrincipal;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private final UserRepository userRepo = mock(UserRepository.class);
    private final WalletRepository walletRepo = mock(WalletRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AuthenticationManager authManager = mock(AuthenticationManager.class);
    private final JwtService jwt = mock(JwtService.class);
    private final PaymentProperties props = new PaymentProperties(new BigDecimal("50000"), new BigDecimal("100000"),
            5, 10, new BigDecimal("20000"), 3, "INR", new BigDecimal("100000"), "Asia/Kolkata");

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepo, walletRepo, encoder, authManager, jwt, props);
    }

    @Test
    void registerHashesPasswordCreatesWalletAndAssignsUserRole() {
        when(userRepo.existsByEmail("new@example.com")).thenReturn(false);
        when(encoder.encode("Passw0rd!")).thenReturn("HASHED");
        when(userRepo.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(5L);
            return u;
        });

        UserResponse response = service.register(
                new RegisterRequest("New User", " New@Example.com ", "Passw0rd!", "9876543210"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(saved.capture());
        assertThat(saved.getValue().getPassword()).isEqualTo("HASHED");
        assertThat(saved.getValue().getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(response.email()).isEqualTo("new@example.com");

        ArgumentCaptor<Wallet> wallet = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepo).save(wallet.capture());
        assertThat(wallet.getValue().getBalance()).isEqualByComparingTo("0");
        assertThat(wallet.getValue().getCurrency()).isEqualTo("INR");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepo.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(
                new RegisterRequest("Dup", "dup@example.com", "Passw0rd!", "9876543210")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepo, never()).save(any());
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        User user = User.builder().id(5L).name("Al").email("al@example.com").password("h").phone("1")
                .role(Role.USER).status(UserStatus.ACTIVE).build();
        UserPrincipal principal = new UserPrincipal(user);
        when(authManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(userRepo.findById(5L)).thenReturn(Optional.of(user));
        when(jwt.generateToken(principal)).thenReturn("signed.jwt.token");
        when(jwt.getExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = service.login(new LoginRequest("al@example.com", "Passw0rd!"));

        assertThat(response.token()).isEqualTo("signed.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("al@example.com");
    }

    @Test
    void loginPropagatesBadCredentials() {
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> service.login(new LoginRequest("al@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
