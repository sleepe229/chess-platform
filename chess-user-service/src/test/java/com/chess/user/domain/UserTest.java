package com.chess.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Nested
    @DisplayName("Increment Methods Tests")
    class IncrementMethodsTests {

        @Test
        @DisplayName("Should increment games count")
        void shouldIncrementGamesCount() {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username("testuser")
                    .email("test@example.com")
                    .totalGames(10)
                    .build();

            user.incrementGames();

            assertThat(user.getTotalGames()).isEqualTo(11);
        }

        @Test
        @DisplayName("Should increment wins count")
        void shouldIncrementWinsCount() {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username("testuser")
                    .email("test@example.com")
                    .totalWins(5)
                    .build();

            user.incrementWins();

            assertThat(user.getTotalWins()).isEqualTo(6);
        }

        @Test
        @DisplayName("Should increment losses count")
        void shouldIncrementLossesCount() {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username("testuser")
                    .email("test@example.com")
                    .totalLosses(3)
                    .build();

            user.incrementLosses();

            assertThat(user.getTotalLosses()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should increment draws count")
        void shouldIncrementDrawsCount() {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username("testuser")
                    .email("test@example.com")
                    .totalDraws(2)
                    .build();

            user.incrementDraws();

            assertThat(user.getTotalDraws()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Win Rate Tests")
    class WinRateTests {

        @Test
        @DisplayName("Should return 0 when no games played")
        void shouldReturnZeroWhenNoGamesPlayed() {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username("testuser")
                    .email("test@example.com")
                    .totalGames(0)
                    .totalWins(0)
                    .build();

            assertThat(user.getWinRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should calculate 50% win rate")
        void shouldCalculate50PercentWinRate() {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username("testuser")
                    .email("test@example.com")
                    .totalGames(10)
                    .totalWins(5)
                    .build();

            assertThat(user.getWinRate()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should calculate 100% win rate")
        void shouldCalculate100PercentWinRate() {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username("testuser")
                    .email("test@example.com")
                    .totalGames(10)
                    .totalWins(10)
                    .build();

            assertThat(user.getWinRate()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should calculate 0% win rate")
        void shouldCalculate0PercentWinRate() {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username("testuser")
                    .email("test@example.com")
                    .totalGames(10)
                    .totalWins(0)
                    .build();

            assertThat(user.getWinRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should calculate fractional win rate")
        void shouldCalculateFractionalWinRate() {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username("testuser")
                    .email("test@example.com")
                    .totalGames(3)
                    .totalWins(1)
                    .build();

            assertThat(user.getWinRate()).isCloseTo(33.33, org.assertj.core.api.Assertions.within(0.01));
        }
    }

    @Nested
    @DisplayName("Builder Defaults Tests")
    class BuilderDefaultsTests {

        @Test
        @DisplayName("Should have default values")
        void shouldHaveDefaultValues() {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username("testuser")
                    .email("test@example.com")
                    .build();

            assertThat(user.getTotalGames()).isEqualTo(0);
            assertThat(user.getTotalWins()).isEqualTo(0);
            assertThat(user.getTotalLosses()).isEqualTo(0);
            assertThat(user.getTotalDraws()).isEqualTo(0);
        }
    }
}
