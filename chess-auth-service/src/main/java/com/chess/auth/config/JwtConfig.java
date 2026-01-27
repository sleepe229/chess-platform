package com.chess.auth.config;

import com.chess.auth.constants.JwtConstants;
import com.chess.common.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class JwtConfig {

    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-ms:" + JwtConstants.ACCESS_TOKEN_VALIDITY_MS + "}") long accessTokenValidityMs,
            @Value("${jwt.refresh-token-validity-ms:" + JwtConstants.REFRESH_TOKEN_VALIDITY_MS + "}") long refreshTokenValidityMs) {

        // Validate JWT secret
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret must be provided via jwt.secret property");
        }
        
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters long");
        }

        log.info("JWT Token Provider initialized with access token validity: {}ms, refresh token validity: {}ms", 
                accessTokenValidityMs, refreshTokenValidityMs);

        return new JwtTokenProvider(secret, accessTokenValidityMs, refreshTokenValidityMs);
    }
}
