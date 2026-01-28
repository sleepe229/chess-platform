package com.chess.user.service;

import com.chess.common.exception.NotFoundException;
import com.chess.user.algorithm.Glicko2Calculator;
import com.chess.user.domain.Rating;
import com.chess.user.dto.RatingDto;
import com.chess.user.messaging.UserEventPublisher;
import com.chess.user.repo.RatingHistoryRepository;
import com.chess.user.repo.RatingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private RatingHistoryRepository ratingHistoryRepository;

    @Mock
    private UserEventPublisher eventPublisher;

    @Mock
    private Glicko2Calculator glicko2Calculator;

    private RatingService ratingService;

    @BeforeEach
    void setUp() {
        ratingService = new RatingService(
                ratingRepository,
                ratingHistoryRepository,
                eventPublisher,
                glicko2Calculator
        );
    }

    @Nested
    @DisplayName("Initialize Ratings Tests")
    class InitializeRatingsTests {

        @Test
        @DisplayName("Should initialize ratings for all time controls")
        void shouldInitializeRatingsForAllTimeControls() {
            UUID userId = UUID.randomUUID();

            when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> inv.getArgument(0));

            ratingService.initializeRatings(userId);

            ArgumentCaptor<Rating> ratingCaptor = ArgumentCaptor.forClass(Rating.class);
            verify(ratingRepository, times(4)).save(ratingCaptor.capture());

            List<Rating> savedRatings = ratingCaptor.getAllValues();
            assertThat(savedRatings).hasSize(4);

            List<String> timeControls = savedRatings.stream()
                    .map(Rating::getTimeControl)
                    .toList();
            assertThat(timeControls).containsExactlyInAnyOrder("BULLET", "BLITZ", "RAPID", "CLASSICAL");

            for (Rating rating : savedRatings) {
                assertThat(rating.getUserId()).isEqualTo(userId);
                assertThat(rating.getRating()).isEqualTo(1500.0);
                assertThat(rating.getRatingDeviation()).isEqualTo(350.0);
                assertThat(rating.getVolatility()).isEqualTo(0.06);
                assertThat(rating.getGamesPlayed()).isEqualTo(0);
                assertThat(rating.getPeakRating()).isEqualTo(1500.0);
            }
        }
    }

    @Nested
    @DisplayName("Get User Ratings Tests")
    class GetUserRatingsTests {

        @Test
        @DisplayName("Should return all ratings for user")
        void shouldReturnAllRatingsForUser() {
            UUID userId = UUID.randomUUID();
            List<Rating> ratings = List.of(
                    createRating(userId, "BULLET", 1550.0, 200.0),
                    createRating(userId, "BLITZ", 1600.0, 180.0),
                    createRating(userId, "RAPID", 1500.0, 220.0),
                    createRating(userId, "CLASSICAL", 1450.0, 250.0)
            );

            when(ratingRepository.findByUserId(userId)).thenReturn(ratings);

            List<RatingDto> result = ratingService.getUserRatings(userId);

            assertThat(result).hasSize(4);
            assertThat(result.stream().map(RatingDto::getTimeControl).toList())
                    .containsExactlyInAnyOrder("BULLET", "BLITZ", "RAPID", "CLASSICAL");
        }

        @Test
        @DisplayName("Should return empty list when no ratings found")
        void shouldReturnEmptyListWhenNoRatingsFound() {
            UUID userId = UUID.randomUUID();

            when(ratingRepository.findByUserId(userId)).thenReturn(List.of());

            List<RatingDto> result = ratingService.getUserRatings(userId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Rating Tests")
    class GetRatingTests {

        @Test
        @DisplayName("Should return rating when found")
        void shouldReturnRatingWhenFound() {
            UUID userId = UUID.randomUUID();
            String timeControl = "BLITZ";
            Rating rating = createRating(userId, timeControl, 1600.0, 180.0);

            when(ratingRepository.findByUserIdAndTimeControl(userId, timeControl))
                    .thenReturn(Optional.of(rating));

            Rating result = ratingService.getRating(userId, timeControl);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getTimeControl()).isEqualTo(timeControl);
            assertThat(result.getRating()).isEqualTo(1600.0);
        }

        @Test
        @DisplayName("Should throw NotFoundException when rating not found")
        void shouldThrowNotFoundExceptionWhenRatingNotFound() {
            UUID userId = UUID.randomUUID();
            String timeControl = "BLITZ";

            when(ratingRepository.findByUserIdAndTimeControl(userId, timeControl))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> ratingService.getRating(userId, timeControl))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Rating not found for time control: BLITZ");
        }
    }

    @Nested
    @DisplayName("Update Rating After Game Tests")
    class UpdateRatingAfterGameTests {

        @Test
        @DisplayName("Should update rating after win")
        void shouldUpdateRatingAfterWin() {
            UUID playerId = UUID.randomUUID();
            UUID opponentId = UUID.randomUUID();
            UUID gameId = UUID.randomUUID();
            String timeControl = "BLITZ";
            double score = 1.0; // Win

            Rating playerRating = createRating(playerId, timeControl, 1500.0, 200.0);
            Rating opponentRating = createRating(opponentId, timeControl, 1500.0, 200.0);

            Glicko2Calculator.RatingResult ratingResult = Glicko2Calculator.RatingResult.builder()
                    .rating(1520.0)
                    .ratingDeviation(190.0)
                    .volatility(0.06)
                    .build();

            when(ratingRepository.findByUserIdAndTimeControl(playerId, timeControl))
                    .thenReturn(Optional.of(playerRating));
            when(ratingRepository.findByUserIdAndTimeControl(opponentId, timeControl))
                    .thenReturn(Optional.of(opponentRating));
            when(glicko2Calculator.calculateNewRating(
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(ratingResult);
            when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> inv.getArgument(0));

            ratingService.updateRatingAfterGame(playerId, opponentId, timeControl, score, gameId);

            verify(ratingRepository).save(any(Rating.class));
            verify(ratingHistoryRepository).save(any());
            verify(eventPublisher).publishRatingUpdated(
                    eq(playerId), eq(timeControl), eq(1500.0), eq(1520.0),
                    eq(200.0), eq(190.0), eq(gameId)
            );
        }

        @Test
        @DisplayName("Should update rating after loss")
        void shouldUpdateRatingAfterLoss() {
            UUID playerId = UUID.randomUUID();
            UUID opponentId = UUID.randomUUID();
            UUID gameId = UUID.randomUUID();
            String timeControl = "RAPID";
            double score = 0.0; // Loss

            Rating playerRating = createRating(playerId, timeControl, 1500.0, 200.0);
            Rating opponentRating = createRating(opponentId, timeControl, 1500.0, 200.0);

            Glicko2Calculator.RatingResult ratingResult = Glicko2Calculator.RatingResult.builder()
                    .rating(1480.0)
                    .ratingDeviation(190.0)
                    .volatility(0.06)
                    .build();

            when(ratingRepository.findByUserIdAndTimeControl(playerId, timeControl))
                    .thenReturn(Optional.of(playerRating));
            when(ratingRepository.findByUserIdAndTimeControl(opponentId, timeControl))
                    .thenReturn(Optional.of(opponentRating));
            when(glicko2Calculator.calculateNewRating(
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(ratingResult);
            when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> inv.getArgument(0));

            ratingService.updateRatingAfterGame(playerId, opponentId, timeControl, score, gameId);

            verify(ratingRepository).save(any(Rating.class));
            verify(ratingHistoryRepository).save(any());
        }

        @Test
        @DisplayName("Should update rating after draw")
        void shouldUpdateRatingAfterDraw() {
            UUID playerId = UUID.randomUUID();
            UUID opponentId = UUID.randomUUID();
            UUID gameId = UUID.randomUUID();
            String timeControl = "CLASSICAL";
            double score = 0.5; // Draw

            Rating playerRating = createRating(playerId, timeControl, 1500.0, 200.0);
            Rating opponentRating = createRating(opponentId, timeControl, 1500.0, 200.0);

            Glicko2Calculator.RatingResult ratingResult = Glicko2Calculator.RatingResult.builder()
                    .rating(1500.0)
                    .ratingDeviation(190.0)
                    .volatility(0.06)
                    .build();

            when(ratingRepository.findByUserIdAndTimeControl(playerId, timeControl))
                    .thenReturn(Optional.of(playerRating));
            when(ratingRepository.findByUserIdAndTimeControl(opponentId, timeControl))
                    .thenReturn(Optional.of(opponentRating));
            when(glicko2Calculator.calculateNewRating(
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(ratingResult);
            when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> inv.getArgument(0));

            ratingService.updateRatingAfterGame(playerId, opponentId, timeControl, score, gameId);

            verify(ratingRepository).save(any(Rating.class));
            verify(ratingHistoryRepository).save(any());
        }
    }

    private Rating createRating(UUID userId, String timeControl, double rating, double rd) {
        return Rating.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .timeControl(timeControl)
                .rating(rating)
                .ratingDeviation(rd)
                .volatility(0.06)
                .gamesPlayed(10)
                .peakRating(rating)
                .updatedAt(Instant.now())
                .build();
    }
}
