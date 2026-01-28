package com.chess.user.config;

import com.chess.common.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class JwtConfig {

    private static final int MIN_SECRET_LENGTH = 32;

    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-ms:900000}") long accessTokenValidityMs,
            @Value("${jwt.refresh-token-validity-ms:2592000000}") long refreshTokenValidityMs) {

        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret must be provided via jwt.secret property");
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("JWT secret must be at least " + MIN_SECRET_LENGTH + " characters long");
        }

        log.debug("JWT Token Provider initialized for user-service");
        return new JwtTokenProvider(secret, accessTokenValidityMs, refreshTokenValidityMs);
    }
}
