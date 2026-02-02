package com.chess.game.config;

import com.chess.common.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
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
        JwtTokenProvider provider = new JwtTokenProvider(jwtSecret, accessTokenValidityMs, refreshTokenValidityMs);
        log.info("JWT Token Provider initialized with access token validity: {}ms, refresh token validity: {}ms",
                accessTokenValidityMs, refreshTokenValidityMs);
        return provider;
    }
}

