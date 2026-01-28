package com.chess.auth.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    @Nested
    @DisplayName("isValid() Tests")
    class IsValidTests {

        @Test
        @DisplayName("Should return true when token is not expired and not revoked")
        void shouldReturnTrueWhenNotExpiredAndNotRevoked() {
            RefreshToken token = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .tokenHash("tokenHash")
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revokedAt(null)
                    .build();

            assertThat(token.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should return false when token is expired")
        void shouldReturnFalseWhenExpired() {
            RefreshToken token = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .tokenHash("tokenHash")
                    .expiresAt(Instant.now().minusSeconds(3600))
                    .revokedAt(null)
                    .build();

            assertThat(token.isValid()).isFalse();
        }

        @Test
        @DisplayName("Should return false when token is revoked")
        void shouldReturnFalseWhenRevoked() {
            RefreshToken token = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .tokenHash("tokenHash")
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revokedAt(Instant.now().minusSeconds(60))
                    .build();

            assertThat(token.isValid()).isFalse();
        }

        @Test
        @DisplayName("Should return false when token is both expired and revoked")
        void shouldReturnFalseWhenExpiredAndRevoked() {
            RefreshToken token = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .tokenHash("tokenHash")
                    .expiresAt(Instant.now().minusSeconds(3600))
                    .revokedAt(Instant.now().minusSeconds(60))
                    .build();

            assertThat(token.isValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("revoke() Tests")
    class RevokeTests {

        @Test
        @DisplayName("Should set revokedAt to current time")
        void shouldSetRevokedAtToCurrentTime() {
            RefreshToken token = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .tokenHash("tokenHash")
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revokedAt(null)
                    .build();

            Instant beforeRevoke = Instant.now();
            token.revoke();
            Instant afterRevoke = Instant.now();

            assertThat(token.getRevokedAt()).isNotNull();
            assertThat(token.getRevokedAt()).isAfterOrEqualTo(beforeRevoke);
            assertThat(token.getRevokedAt()).isBeforeOrEqualTo(afterRevoke);
        }

        @Test
        @DisplayName("Should make token invalid after revoke")
        void shouldMakeTokenInvalidAfterRevoke() {
            RefreshToken token = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .tokenHash("tokenHash")
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revokedAt(null)
                    .build();

            assertThat(token.isValid()).isTrue();

            token.revoke();

            assertThat(token.isValid()).isFalse();
        }
    }
}
