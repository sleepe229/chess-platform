package com.chess.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RatingTest {

    @Nested
    @DisplayName("Update Rating Tests")
    class UpdateRatingTests {

        @Test
        @DisplayName("Should update rating values")
        void shouldUpdateRatingValues() {
            Rating rating = Rating.builder()
                    .id(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .timeControl("BLITZ")
                    .rating(1500.0)
                    .ratingDeviation(350.0)
                    .volatility(0.06)
                    .gamesPlayed(0)
                    .peakRating(1500.0)
                    .build();

            rating.updateRating(1550.0, 320.0, 0.058);

            assertThat(rating.getRating()).isEqualTo(1550.0);
            assertThat(rating.getRatingDeviation()).isEqualTo(320.0);
            assertThat(rating.getVolatility()).isEqualTo(0.058);
        }

        @Test
        @DisplayName("Should increment games played on update")
        void shouldIncrementGamesPlayedOnUpdate() {
            Rating rating = Rating.builder()
                    .id(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .timeControl("BLITZ")
                    .rating(1500.0)
                    .ratingDeviation(350.0)
                    .volatility(0.06)
                    .gamesPlayed(10)
                    .peakRating(1500.0)
                    .build();

            rating.updateRating(1520.0, 340.0, 0.06);

            assertThat(rating.getGamesPlayed()).isEqualTo(11);
        }

        @Test
        @DisplayName("Should update peak rating when new rating is higher")
        void shouldUpdatePeakRatingWhenNewRatingIsHigher() {
            Rating rating = Rating.builder()
                    .id(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .timeControl("RAPID")
                    .rating(1500.0)
                    .ratingDeviation(200.0)
                    .volatility(0.06)
                    .gamesPlayed(10)
                    .peakRating(1600.0)
                    .build();

            rating.updateRating(1650.0, 190.0, 0.06);

            assertThat(rating.getPeakRating()).isEqualTo(1650.0);
        }

        @Test
        @DisplayName("Should not update peak rating when new rating is lower")
        void shouldNotUpdatePeakRatingWhenNewRatingIsLower() {
            Rating rating = Rating.builder()
                    .id(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .timeControl("CLASSICAL")
                    .rating(1500.0)
                    .ratingDeviation(200.0)
                    .volatility(0.06)
                    .gamesPlayed(10)
                    .peakRating(1600.0)
                    .build();

            rating.updateRating(1450.0, 190.0, 0.06);

            assertThat(rating.getPeakRating()).isEqualTo(1600.0);
        }

        @Test
        @DisplayName("Should not update peak rating when new rating equals peak")
        void shouldNotUpdatePeakRatingWhenNewRatingEqualsPeak() {
            Rating rating = Rating.builder()
                    .id(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .timeControl("BULLET")
                    .rating(1500.0)
                    .ratingDeviation(200.0)
                    .volatility(0.06)
                    .gamesPlayed(10)
                    .peakRating(1600.0)
                    .build();

            rating.updateRating(1600.0, 190.0, 0.06);

            assertThat(rating.getPeakRating()).isEqualTo(1600.0);
        }
    }

    @Nested
    @DisplayName("Builder Defaults Tests")
    class BuilderDefaultsTests {

        @Test
        @DisplayName("Should have default rating of 1500")
        void shouldHaveDefaultRatingOf1500() {
            Rating rating = Rating.builder()
                    .id(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .timeControl("BLITZ")
                    .build();

            assertThat(rating.getRating()).isEqualTo(1500.0);
        }

        @Test
        @DisplayName("Should have default RD of 350")
        void shouldHaveDefaultRdOf350() {
            Rating rating = Rating.builder()
                    .id(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .timeControl("BLITZ")
                    .build();

            assertThat(rating.getRatingDeviation()).isEqualTo(350.0);
        }

        @Test
        @DisplayName("Should have default volatility of 0.06")
        void shouldHaveDefaultVolatilityOf006() {
            Rating rating = Rating.builder()
                    .id(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .timeControl("BLITZ")
                    .build();

            assertThat(rating.getVolatility()).isEqualTo(0.06);
        }

        @Test
        @DisplayName("Should have default games played of 0")
        void shouldHaveDefaultGamesPlayedOf0() {
            Rating rating = Rating.builder()
                    .id(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .timeControl("BLITZ")
                    .build();

            assertThat(rating.getGamesPlayed()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should have default peak rating of 1500")
        void shouldHaveDefaultPeakRatingOf1500() {
            Rating rating = Rating.builder()
                    .id(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .timeControl("BLITZ")
                    .build();

            assertThat(rating.getPeakRating()).isEqualTo(1500.0);
        }
    }
}
