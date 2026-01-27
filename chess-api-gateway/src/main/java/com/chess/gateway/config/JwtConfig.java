package com.chess.gateway.config;

import com.chess.common.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-ms:900000}") long accessTokenValidityMs,
            @Value("${jwt.refresh-token-validity-ms:2592000000}") long refreshTokenValidityMs) {

        return new JwtTokenProvider(secret, accessTokenValidityMs, refreshTokenValidityMs);
    }
}
