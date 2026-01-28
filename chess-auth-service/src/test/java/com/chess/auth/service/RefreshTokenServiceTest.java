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
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository);
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenValidityMs", 2592000000L);
    }

    @Nested
    @DisplayName("Create Refresh Token Tests")
    class CreateRefreshTokenTests {

        @Test
        @DisplayName("Should create refresh token successfully")
        void shouldCreateRefreshTokenSuccessfully() {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .email("test@example.com")
                    .build();
            String tokenValue = "test-token-value";

            RefreshToken savedToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .tokenHash("hashedToken")
                    .expiresAt(Instant.now().plusMillis(2592000000L))
                    .build();

            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedToken);

            RefreshToken result = refreshTokenService.createRefreshToken(user, tokenValue);

            assertThat(result).isNotNull();

            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(tokenCaptor.capture());
            RefreshToken capturedToken = tokenCaptor.getValue();
            assertThat(capturedToken.getUser()).isEqualTo(user);
            assertThat(capturedToken.getTokenHash()).isNotNull();
            assertThat(capturedToken.getTokenHash()).isNotEmpty();
            assertThat(capturedToken.getExpiresAt()).isAfter(Instant.now());
        }

        @Test
        @DisplayName("Should generate different hashes for different tokens")
        void shouldGenerateDifferentHashesForDifferentTokens() {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .email("test@example.com")
                    .build();

            RefreshToken savedToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .tokenHash("hashedToken")
                    .expiresAt(Instant.now().plusMillis(2592000000L))
                    .build();

            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedToken);

            refreshTokenService.createRefreshToken(user, "token1");
            refreshTokenService.createRefreshToken(user, "token2");

            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository, times(2)).save(tokenCaptor.capture());
            
            var capturedTokens = tokenCaptor.getAllValues();
            assertThat(capturedTokens.get(0).getTokenHash()).isNotEqualTo(capturedTokens.get(1).getTokenHash());
        }
    }

    @Nested
    @DisplayName("Validate Refresh Token Tests")
    class ValidateRefreshTokenTests {

        @Test
        @DisplayName("Should return token when valid")
        void shouldReturnTokenWhenValid() {
            String tokenValue = "valid-token";
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .email("test@example.com")
                    .build();

            RefreshToken token = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .tokenHash("hashedToken")
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revokedAt(null)
                    .build();

            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

            RefreshToken result = refreshTokenService.validateRefreshToken(tokenValue);

            assertThat(result).isNotNull();
            assertThat(result.getUser()).isEqualTo(user);
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when token not found")
        void shouldThrowUnauthorizedExceptionWhenTokenNotFound() {
            String tokenValue = "nonexistent-token";

            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.validateRefreshToken(tokenValue))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid refresh token");
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when token is expired")
        void shouldThrowUnauthorizedExceptionWhenTokenExpired() {
            String tokenValue = "expired-token";
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .email("test@example.com")
                    .build();

            RefreshToken token = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .tokenHash("hashedToken")
                    .expiresAt(Instant.now().minusSeconds(3600)) // Expired
                    .revokedAt(null)
                    .build();

            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> refreshTokenService.validateRefreshToken(tokenValue))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Refresh token expired or revoked");
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when token is revoked")
        void shouldThrowUnauthorizedExceptionWhenTokenRevoked() {
            String tokenValue = "revoked-token";
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .email("test@example.com")
                    .build();

            RefreshToken token = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .tokenHash("hashedToken")
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revokedAt(Instant.now().minusSeconds(60)) // Revoked
                    .build();

            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> refreshTokenService.validateRefreshToken(tokenValue))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Refresh token expired or revoked");
        }
    }

    @Nested
    @DisplayName("Revoke Refresh Token Tests")
    class RevokeRefreshTokenTests {

        @Test
        @DisplayName("Should revoke token when found")
        void shouldRevokeTokenWhenFound() {
            String tokenValue = "token-to-revoke";
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .email("test@example.com")
                    .build();

            RefreshToken token = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .tokenHash("hashedToken")
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revokedAt(null)
                    .build();

            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));
            when(refreshTokenRepository.save(any())).thenReturn(token);

            refreshTokenService.revokeRefreshToken(tokenValue);

            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(tokenCaptor.capture());
            assertThat(tokenCaptor.getValue().getRevokedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should do nothing when token not found")
        void shouldDoNothingWhenTokenNotFound() {
            String tokenValue = "nonexistent-token";

            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

            // Should not throw exception
            refreshTokenService.revokeRefreshToken(tokenValue);

            verify(refreshTokenRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Cleanup Expired Tokens Tests")
    class CleanupExpiredTokensTests {

        @Test
        @DisplayName("Should call repository to delete expired tokens")
        void shouldCallRepositoryToDeleteExpiredTokens() {
            refreshTokenService.cleanupExpiredTokens();

            verify(refreshTokenRepository).deleteExpiredAndRevoked(any(Instant.class));
        }
    }
}
