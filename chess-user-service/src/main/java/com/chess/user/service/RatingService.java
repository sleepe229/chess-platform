package com.chess.user.service;

import com.chess.common.exception.NotFoundException;
import com.chess.user.algorithm.Glicko2Calculator;
import com.chess.user.domain.Rating;
import com.chess.user.domain.RatingHistory;
import com.chess.user.dto.RatingDto;
import com.chess.user.messaging.UserEventPublisher;
import com.chess.user.repo.RatingHistoryRepository;
import com.chess.user.repo.RatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final RatingHistoryRepository ratingHistoryRepository;
    private final UserEventPublisher eventPublisher;
    private final Glicko2Calculator glicko2Calculator;

    private static final List<String> TIME_CONTROLS = Arrays.asList(
            "BULLET",      // < 3 min
            "BLITZ",       // 3-10 min
            "RAPID",       // 10-30 min
            "CLASSICAL"    // > 30 min
    );

    @Transactional
    public void initializeRatings(UUID userId) {
        log.info("Initializing ratings for userId: {}", userId);

        for (String timeControl : TIME_CONTROLS) {
            Rating rating = Rating.builder()
                    .userId(userId)
                    .timeControl(timeControl)
                    .rating(1500.0)
                    .ratingDeviation(350.0)
                    .volatility(0.06)
                    .gamesPlayed(0)
                    .peakRating(1500.0)
                    .build();

            ratingRepository.save(rating);
        }

        log.info("Ratings initialized for userId: {}", userId);
    }

    @Transactional(readOnly = true)
    public List<RatingDto> getUserRatings(UUID userId) {
        List<Rating> ratings = ratingRepository.findByUserId(userId);

        return ratings.stream()
                .map(this::toRatingDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Rating getRating(UUID userId, String timeControl) {
        return ratingRepository.findByUserIdAndTimeControl(userId, timeControl)
                .orElseThrow(() -> new NotFoundException("Rating not found for time control: " + timeControl));
    }

    @Transactional
    public void updateRatingAfterGame(
            UUID userId,
            UUID opponentId,
            String timeControl,
            double score,
            UUID gameId) {

        log.info("Updating rating for userId: {}, opponent: {}, timeControl: {}, score: {}",
                userId, opponentId, timeControl, score);

        Rating playerRating = getRating(userId, timeControl);
        Rating opponentRating = getRating(opponentId, timeControl);

        double oldRating = playerRating.getRating();
        double oldRd = playerRating.getRatingDeviation();
        double oldVolatility = playerRating.getVolatility();

        // Glicko-2 расчет
        Glicko2Calculator.RatingResult result = glicko2Calculator.calculateNewRating(
                playerRating.getRating(),
                playerRating.getRatingDeviation(),
                playerRating.getVolatility(),
                opponentRating.getRating(),
                opponentRating.getRatingDeviation(),
                score
        );

        playerRating.updateRating(result.getRating(), result.getRatingDeviation(), result.getVolatility());
        ratingRepository.save(playerRating);

        // Сохраняем историю
        saveRatingHistory(
                userId,
                timeControl,
                oldRating,
                result.getRating(),
                oldRd,
                result.getRatingDeviation(),
                gameId,
                opponentId,
                opponentRating.getRating(),
                getResultString(score)
        );

        // Публикуем событие
        eventPublisher.publishRatingUpdated(
                userId,
                timeControl,
                oldRating,
                result.getRating(),
                oldRd,
                result.getRatingDeviation(),
                gameId
        );

        log.info("Rating updated for userId: {}. Old: {}, New: {}", userId, oldRating, result.getRating());
    }

    private void saveRatingHistory(
            UUID userId,
            String timeControl,
            double oldRating,
            double newRating,
            double oldRd,
            double newRd,
            UUID gameId,
            UUID opponentId,
            double opponentRating,
            String result) {

        RatingHistory history = RatingHistory.builder()
                .userId(userId)
                .timeControl(timeControl)
                .oldRating(oldRating)
                .newRating(newRating)
                .ratingChange(newRating - oldRating)
                .oldRd(oldRd)
                .newRd(newRd)
                .gameId(gameId)
                .opponentId(opponentId)
                .opponentRating(opponentRating)
                .result(result)
                .build();

        ratingHistoryRepository.save(history);
    }

    private String getResultString(double score) {
        if (score == 1.0) return "WIN";
        if (score == 0.0) return "LOSS";
        return "DRAW";
    }

    private RatingDto toRatingDto(Rating rating) {
        return RatingDto.builder()
                .timeControl(rating.getTimeControl())
                .rating(rating.getRating())
                .ratingDeviation(rating.getRatingDeviation())
                .volatility(rating.getVolatility())
                .gamesPlayed(rating.getGamesPlayed())
                .peakRating(rating.getPeakRating())
                .updatedAt(rating.getUpdatedAt())
                .build();
    }
}
