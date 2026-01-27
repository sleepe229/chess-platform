package com.chess.auth.service;

import com.chess.auth.domain.RefreshToken;
import com.chess.auth.domain.User;
import com.chess.auth.repo.RefreshTokenRepository;
import com.chess.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-validity-ms:2592000000}")
    private long refreshTokenValidityMs;

    @Transactional
    public RefreshToken createRefreshToken(User user, String tokenValue) {
        String tokenHash = hashToken(tokenValue);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusMillis(refreshTokenValidityMs))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String tokenValue) {
        String tokenHash = hashToken(tokenValue);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeRefreshToken(String tokenValue) {
        String tokenHash = hashToken(tokenValue);

        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokenRepository.save(token);
                    log.info("Refresh token revoked: {}", token.getId());
                });
    }

    @Transactional
    @Scheduled(cron = "0 0 2 * * *") // Каждый день в 2:00
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired refresh tokens");
        refreshTokenRepository.deleteExpiredAndRevoked(Instant.now());
        log.info("Expired refresh tokens cleanup completed");
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }
}
