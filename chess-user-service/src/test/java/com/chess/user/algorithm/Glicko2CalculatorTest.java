package com.chess.user.algorithm;

import com.chess.user.algorithm.Glicko2Calculator.RatingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class Glicko2CalculatorTest {

    private Glicko2Calculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new Glicko2Calculator();
    }

    @Nested
    @DisplayName("Basic Rating Calculations")
    class BasicRatingCalculations {

        @Test
        @DisplayName("Should increase rating after a win")
        void shouldIncreaseRatingAfterWin() {
            double playerRating = 1500.0;
            double playerRd = 200.0;
            double playerVolatility = 0.06;
            double opponentRating = 1500.0;
            double opponentRd = 200.0;
            double score = 1.0; // Win

            RatingResult result = calculator.calculateNewRating(
                    playerRating, playerRd, playerVolatility,
                    opponentRating, opponentRd, score
            );

            assertThat(result.getRating()).isGreaterThan(playerRating);
        }

        @Test
        @DisplayName("Should decrease rating after a loss")
        void shouldDecreaseRatingAfterLoss() {
            double playerRating = 1500.0;
            double playerRd = 200.0;
            double playerVolatility = 0.06;
            double opponentRating = 1500.0;
            double opponentRd = 200.0;
            double score = 0.0; // Loss

            RatingResult result = calculator.calculateNewRating(
                    playerRating, playerRd, playerVolatility,
                    opponentRating, opponentRd, score
            );

            assertThat(result.getRating()).isLessThan(playerRating);
        }

        @Test
        @DisplayName("Should have minimal change after a draw between equal players")
        void shouldHaveMinimalChangeAfterDrawBetweenEqualPlayers() {
            double playerRating = 1500.0;
            double playerRd = 200.0;
            double playerVolatility = 0.06;
            double opponentRating = 1500.0;
            double opponentRd = 200.0;
            double score = 0.5; // Draw

            RatingResult result = calculator.calculateNewRating(
                    playerRating, playerRd, playerVolatility,
                    opponentRating, opponentRd, score
            );

            // Rating should remain relatively stable (within a reasonable range)
            assertThat(result.getRating()).isCloseTo(playerRating, within(50.0));
        }
    }

    @Nested
    @DisplayName("Rating Deviation Tests")
    class RatingDeviationTests {

        @Test
        @DisplayName("Should decrease RD after game")
        void shouldDecreaseRdAfterGame() {
            double playerRating = 1500.0;
            double playerRd = 350.0; // High RD (new player)
            double playerVolatility = 0.06;
            double opponentRating = 1500.0;
            double opponentRd = 200.0;
            double score = 0.5;

            RatingResult result = calculator.calculateNewRating(
                    playerRating, playerRd, playerVolatility,
                    opponentRating, opponentRd, score
            );

            assertThat(result.getRatingDeviation()).isLessThan(playerRd);
        }

        @Test
        @DisplayName("Should have positive RD")
        void shouldHavePositiveRd() {
            double playerRating = 1500.0;
            double playerRd = 100.0; // Low RD
            double playerVolatility = 0.06;
            double opponentRating = 1500.0;
            double opponentRd = 200.0;
            double score = 1.0;

            RatingResult result = calculator.calculateNewRating(
                    playerRating, playerRd, playerVolatility,
                    opponentRating, opponentRd, score
            );

            assertThat(result.getRatingDeviation()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Upset Result Tests")
    class UpsetResultTests {

        @Test
        @DisplayName("Should gain more rating when beating a higher-rated opponent")
        void shouldGainMoreRatingWhenBeatingHigherRatedOpponent() {
            double playerRating = 1400.0;
            double playerRd = 200.0;
            double playerVolatility = 0.06;
            double highOpponentRating = 1700.0;
            double lowOpponentRating = 1400.0;
            double opponentRd = 200.0;
            double score = 1.0; // Win

            RatingResult resultHighOpponent = calculator.calculateNewRating(
                    playerRating, playerRd, playerVolatility,
                    highOpponentRating, opponentRd, score
            );

            RatingResult resultLowOpponent = calculator.calculateNewRating(
                    playerRating, playerRd, playerVolatility,
                    lowOpponentRating, opponentRd, score
            );

            double gainAgainstHigh = resultHighOpponent.getRating() - playerRating;
            double gainAgainstLow = resultLowOpponent.getRating() - playerRating;

            assertThat(gainAgainstHigh).isGreaterThan(gainAgainstLow);
        }

        @Test
        @DisplayName("Should lose less rating when losing to a higher-rated opponent")
        void shouldLoseLessRatingWhenLosingToHigherRatedOpponent() {
            double playerRating = 1500.0;
            double playerRd = 200.0;
            double playerVolatility = 0.06;
            double highOpponentRating = 1800.0;
            double lowOpponentRating = 1200.0;
            double opponentRd = 200.0;
            double score = 0.0; // Loss

            RatingResult resultHighOpponent = calculator.calculateNewRating(
                    playerRating, playerRd, playerVolatility,
                    highOpponentRating, opponentRd, score
            );

            RatingResult resultLowOpponent = calculator.calculateNewRating(
                    playerRating, playerRd, playerVolatility,
                    lowOpponentRating, opponentRd, score
            );

            double lossAgainstHigh = playerRating - resultHighOpponent.getRating();
            double lossAgainstLow = playerRating - resultLowOpponent.getRating();

            assertThat(lossAgainstHigh).isLessThan(lossAgainstLow);
        }
    }

    @Nested
    @DisplayName("Volatility Tests")
    class VolatilityTests {

        @Test
        @DisplayName("Should have positive volatility")
        void shouldHavePositiveVolatility() {
            double playerRating = 1500.0;
            double playerRd = 200.0;
            double playerVolatility = 0.06;
            double opponentRating = 1500.0;
            double opponentRd = 200.0;
            double score = 1.0;

            RatingResult result = calculator.calculateNewRating(
                    playerRating, playerRd, playerVolatility,
                    opponentRating, opponentRd, score
            );

            assertThat(result.getVolatility()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should have reasonable volatility range")
        void shouldHaveReasonableVolatilityRange() {
            double playerRating = 1500.0;
            double playerRd = 200.0;
            double playerVolatility = 0.06;
            double opponentRating = 1500.0;
            double opponentRd = 200.0;
            double score = 1.0;

            RatingResult result = calculator.calculateNewRating(
                    playerRating, playerRd, playerVolatility,
                    opponentRating, opponentRd, score
            );

            // Volatility should stay in a reasonable range
            assertThat(result.getVolatility()).isBetween(0.01, 0.15);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle extreme rating differences")
        void shouldHandleExtremeRatingDifferences() {
            double playerRating = 1000.0;
            double playerRd = 200.0;
            double playerVolatility = 0.06;
            double opponentRating = 2500.0;
            double opponentRd = 200.0;
            double score = 1.0; // Upset win

            RatingResult result = calculator.calculateNewRating(
                    playerRating, playerRd, playerVolatility,
                    opponentRating, opponentRd, score
            );

            assertThat(result.getRating()).isGreaterThan(playerRating);
            assertThat(result.getRatingDeviation()).isGreaterThan(0);
            assertThat(result.getVolatility()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should handle high RD values")
        void shouldHandleHighRdValues() {
            double playerRating = 1500.0;
            double playerRd = 350.0; // Maximum initial RD
            double playerVolatility = 0.06;
            double opponentRating = 1500.0;
            double opponentRd = 350.0;
            double score = 0.5;

            RatingResult result = calculator.calculateNewRating(
                    playerRating, playerRd, playerVolatility,
                    opponentRating, opponentRd, score
            );

            assertThat(result.getRating()).isFinite();
            assertThat(result.getRatingDeviation()).isFinite();
            assertThat(result.getRatingDeviation()).isLessThan(playerRd);
        }

        @Test
        @DisplayName("Should handle low RD values")
        void shouldHandleLowRdValues() {
            double playerRating = 1500.0;
            double playerRd = 50.0; // Low RD (active player)
            double playerVolatility = 0.06;
            double opponentRating = 1500.0;
            double opponentRd = 50.0;
            double score = 1.0;

            RatingResult result = calculator.calculateNewRating(
                    playerRating, playerRd, playerVolatility,
                    opponentRating, opponentRd, score
            );

            assertThat(result.getRating()).isFinite();
            assertThat(result.getRatingDeviation()).isFinite();
            assertThat(result.getRatingDeviation()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("RatingResult Tests")
    class RatingResultTests {

        @Test
        @DisplayName("Should create RatingResult with builder and allow property access")
        void shouldCreateRatingResultWithBuilder() {
            RatingResult result = RatingResult.builder()
                    .rating(1500.0)
                    .ratingDeviation(200.0)
                    .volatility(0.06)
                    .build();

            assertThat(result.getRating()).isEqualTo(1500.0);
            assertThat(result.getRatingDeviation()).isEqualTo(200.0);
            assertThat(result.getVolatility()).isEqualTo(0.06);
        }

        @Test
        @DisplayName("Should allow setting properties after construction")
        void shouldAllowSettingPropertiesAfterConstruction() {
            RatingResult result = RatingResult.builder()
                    .rating(1500.0)
                    .ratingDeviation(200.0)
                    .volatility(0.06)
                    .build();

            result.setRating(1600.0);
            result.setRatingDeviation(180.0);
            result.setVolatility(0.055);

            assertThat(result.getRating()).isEqualTo(1600.0);
            assertThat(result.getRatingDeviation()).isEqualTo(180.0);
            assertThat(result.getVolatility()).isEqualTo(0.055);
        }
    }
}
