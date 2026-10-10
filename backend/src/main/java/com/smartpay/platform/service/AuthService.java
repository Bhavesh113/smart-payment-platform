package com.smartpay.platform.service;

import com.smartpay.platform.config.PaymentProperties;
import com.smartpay.platform.dto.LoginRequest;
import com.smartpay.platform.dto.LoginResponse;
import com.smartpay.platform.dto.RegisterRequest;
import com.smartpay.platform.dto.UserResponse;
import com.smartpay.platform.entity.Role;
import com.smartpay.platform.entity.User;
import com.smartpay.platform.entity.UserStatus;
import com.smartpay.platform.entity.Wallet;
import com.smartpay.platform.entity.WalletStatus;
import com.smartpay.platform.exception.DuplicateResourceException;
import com.smartpay.platform.mapper.UserMapper;
import com.smartpay.platform.repository.UserRepository;
import com.smartpay.platform.repository.WalletRepository;
import com.smartpay.platform.security.JwtService;
import com.smartpay.platform.security.UserPrincipal;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PaymentProperties properties;

    /** Creates the user and an empty wallet atomically; registration always yields the USER role. */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        User user = userRepository.save(User.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .phone(request.phone().trim())
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());
        walletRepository.save(Wallet.builder()
                .user(user)
                .balance(BigDecimal.ZERO.setScale(2))
                .currency(properties.currency())
                .status(WalletStatus.ACTIVE)
                .build());
        log.info("User registered id={}", user.getId());
        return UserMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId()).orElseThrow();
        log.info("User logged in id={}", principal.getId());
        return new LoginResponse(jwtService.generateToken(principal), "Bearer", jwtService.getExpirationSeconds(),
                UserMapper.toResponse(user));
    }
}
