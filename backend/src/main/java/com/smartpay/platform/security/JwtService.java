package com.smartpay.platform.security;

import com.smartpay.platform.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration validity;

    public JwtService(JwtProperties properties) {
        byte[] secret = properties.secret() == null ? new byte[0] : properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("jwt.secret (JWT_SECRET) must be at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.validity = Duration.ofMinutes(properties.expirationMinutes());
    }

    public String generateToken(UserPrincipal principal) {
        Date now = new Date();
        return Jwts.builder()
                .subject(principal.getEmail())
                .claim("uid", principal.getId())
                .claim("role", principal.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + validity.toMillis()))
                .signWith(key)
                .compact();
    }

    /** Verifies signature and expiry; throws a JwtException subtype when the token is not acceptable. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public String extractEmail(String token) {
        return parse(token).getSubject();
    }

    public long getExpirationSeconds() {
        return validity.toSeconds();
    }
}
