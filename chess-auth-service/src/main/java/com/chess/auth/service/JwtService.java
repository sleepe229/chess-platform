package com.chess.auth.service;

import com.chess.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtTokenProvider jwtTokenProvider;

    public String generateAccessToken(UUID userId, List<String> roles) {
        return jwtTokenProvider.generateAccessToken(userId, roles);
    }

    public String generateRefreshToken(UUID userId) {
        return jwtTokenProvider.generateRefreshToken(userId);
    }

    public UUID getUserIdFromToken(String token) {
        return jwtTokenProvider.getUserIdFromToken(token);
    }

    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    public long getAccessTokenValiditySeconds() {
        return jwtTokenProvider.getAccessTokenValidityMs() / 1000;
    }

    public long getRefreshTokenValiditySeconds() {
        return jwtTokenProvider.getRefreshTokenValidityMs() / 1000;
    }
}
