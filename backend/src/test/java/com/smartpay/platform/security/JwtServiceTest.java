package com.smartpay.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smartpay.platform.config.JwtProperties;
import com.smartpay.platform.entity.Role;
import com.smartpay.platform.entity.User;
import com.smartpay.platform.entity.UserStatus;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-longer-than-32-chars";

    private final UserPrincipal principal = new UserPrincipal(User.builder().id(7L).email("alice@example.com")
            .password("hash").role(Role.USER).status(UserStatus.ACTIVE).build());

    @Test
    void generatedTokenCarriesEmailAndClaims() {
        JwtService jwt = new JwtService(new JwtProperties(SECRET, 60));
        String token = jwt.generateToken(principal);

        assertThat(jwt.extractEmail(token)).isEqualTo("alice@example.com");
        assertThat(jwt.parse(token).get("role", String.class)).isEqualTo("USER");
        assertThat(jwt.parse(token).get("uid", Long.class)).isEqualTo(7L);
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwt = new JwtService(new JwtProperties(SECRET, 60));
        String token = jwt.generateToken(principal);
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        assertThatThrownBy(() -> jwt.parse(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = new JwtService(new JwtProperties("another-secret-another-secret-another-secret", 60))
                .generateToken(principal);
        JwtService jwt = new JwtService(new JwtProperties(SECRET, 60));

        assertThatThrownBy(() -> jwt.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwt = new JwtService(new JwtProperties(SECRET, -1));
        String token = jwt.generateToken(principal);

        assertThatThrownBy(() -> jwt.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void shortSecretFailsFast() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("too-short", 60)))
                .isInstanceOf(IllegalStateException.class);
    }
}
