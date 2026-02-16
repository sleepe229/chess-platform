package com.chess.analytics.config;

import com.chess.common.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-validity-ms:900000}")
    private long accessTokenValidityMs;

    @Value("${jwt.refresh-token-validity-ms:2592000000}")
    private long refreshTokenValidityMs;

    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider(jwtSecret, accessTokenValidityMs, refreshTokenValidityMs);
    }
}
