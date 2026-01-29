package com.chess.auth.service;

import com.chess.auth.domain.RefreshToken;
import com.chess.auth.domain.User;
import com.chess.auth.repo.RefreshTokenRepository;
import com.chess.common.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService")
class RefreshTokenServiceTest {

    private static final String TOKEN_VALUE = "my-refresh-token";
    private static final long REFRESH_VALIDITY_MS = 86400_000L;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository);
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenValidityMs", REFRESH_VALIDITY_MS);
    }

    @Nested
    @DisplayName("createRefreshToken")
    class CreateRefreshToken {

        @Test
        void savesTokenWithHashedValueAndExpiry() {
            User user = User.builder().id(UUID.randomUUID()).email("u@e.com").build();
            RefreshToken saved = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .tokenHash("hash")
                    .expiresAt(Instant.now().plusMillis(REFRESH_VALIDITY_MS))
                    .build();
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(saved);

            RefreshToken result = refreshTokenService.createRefreshToken(user, TOKEN_VALUE);

            assertThat(result).isNotNull();
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());
            RefreshToken captured = captor.getValue();
            assertThat(captured.getUser()).isEqualTo(user);
            assertThat(captured.getTokenHash()).isNotBlank();
            assertThat(captured.getExpiresAt()).isAfter(Instant.now());
        }
    }

    @Nested
    @DisplayName("validateRefreshToken")
    class ValidateRefreshToken {

        @Test
        void returnsTokenWhenFoundAndValid() {
            User user = User.builder().id(UUID.randomUUID()).email("u@e.com").build();
            RefreshToken token = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .tokenHash("any-hash")
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revokedAt(null)
                    .build();
            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

            RefreshToken result = refreshTokenService.validateRefreshToken(TOKEN_VALUE);

            assertThat(result).isEqualTo(token);
        }

        @Test
        void throwsUnauthorizedWhenTokenNotFound() {
            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.validateRefreshToken(TOKEN_VALUE))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid refresh token");
        }

        @Test
        void throwsUnauthorizedWhenTokenRevoked() {
            RefreshToken token = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .user(User.builder().id(UUID.randomUUID()).build())
                    .tokenHash("hash")
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revokedAt(Instant.now())
                    .build();
            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> refreshTokenService.validateRefreshToken(TOKEN_VALUE))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("expired or revoked");
        }
    }

    @Nested
    @DisplayName("revokeRefreshToken")
    class RevokeRefreshToken {

        @Test
        void revokesAndSavesWhenTokenFound() {
            RefreshToken token = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .user(User.builder().id(UUID.randomUUID()).build())
                    .tokenHash("hash")
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

            refreshTokenService.revokeRefreshToken(TOKEN_VALUE);

            verify(refreshTokenRepository).save(token);
            assertThat(token.getRevokedAt()).isNotNull();
        }

        @Test
        void doesNothingWhenTokenNotFound() {
            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            refreshTokenService.revokeRefreshToken(TOKEN_VALUE);

            verify(refreshTokenRepository, never()).save(any());
        }
    }
}
