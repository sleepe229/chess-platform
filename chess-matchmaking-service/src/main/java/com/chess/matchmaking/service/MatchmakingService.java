package com.chess.matchmaking.service;

import com.chess.matchmaking.config.MatchmakingProperties;
import com.chess.matchmaking.domain.QueuedPlayer;
import com.chess.matchmaking.dto.FindMatchRequest;
import com.chess.matchmaking.dto.MatchFoundDto;
import com.chess.matchmaking.dto.PlayerDequeuedDto;
import com.chess.matchmaking.dto.PlayerQueuedDto;
import com.chess.matchmaking.messaging.MatchmakingEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchmakingService {

    private final MatchmakingQueueService queueService;
    private final MatchmakingEventPublisher eventPublisher;
    private final MatchmakingProperties properties;

    private final Map<String, Long> playerQueueTimes = new ConcurrentHashMap<>();
    private final Map<String, Integer> playerRanges = new ConcurrentHashMap<>();
    private final Map<String, Double> playerRatings = new ConcurrentHashMap<>();
    private final Map<String, String> playerTimeControls = new ConcurrentHashMap<>();

    public void onPlayerQueued(PlayerQueuedDto dto) {
        String userId = dto.getUserId();
        String timeControl = dto.getTimeControl();
        Double rating = dto.getRating() != null ? dto.getRating() : 0.0;
        Double ratingDeviation = dto.getRatingDeviation();

        log.info("Adding player to queue: userId={}, timeControl={}, rating={}", userId, timeControl, rating);

        queueService.addPlayerToQueue(PlayerQueuedDto.builder()
                .userId(userId)
                .timeControl(timeControl)
                .rating(rating)
                .ratingDeviation(ratingDeviation)
                .build());

        String k = key(userId, timeControl);
        playerQueueTimes.put(k, System.currentTimeMillis());
        playerRanges.put(k, properties.getInitialRatingRange());
        playerRatings.put(k, rating);
        playerTimeControls.put(k, timeControl);

        FindMatchRequest findRequest = FindMatchRequest.builder()
                .userId(userId)
                .timeControl(timeControl)
                .rating(rating)
                .range(properties.getInitialRatingRange())
                .build();
        processPlayerMatchmaking(findRequest);
    }

    public void onPlayerDequeued(PlayerDequeuedDto dto) {
        String userId = dto.getUserId();
        String timeControl = dto.getTimeControl();

        log.info("Removing player from queue: userId={}, timeControl={}, reason={}",
                userId, timeControl, dto.getReason());

        queueService.removePlayerFromQueue(dto);

        String k = key(userId, timeControl);
        playerQueueTimes.remove(k);
        playerRanges.remove(k);
        playerRatings.remove(k);
        playerTimeControls.remove(k);
    }

    @Scheduled(fixedDelayString = "${matchmaking.range-expansion-interval-seconds:10}000")
    public void processMatchmaking() {
        long currentTime = System.currentTimeMillis();
        long intervalMs = properties.getRangeExpansionIntervalSeconds() * 1000L;

        playerQueueTimes.forEach((k, queueTime) -> {
            long timeInQueue = currentTime - queueTime;
            int expansions = (int) (timeInQueue / intervalMs);
            int newRange = Math.min(
                    properties.getInitialRatingRange() + (expansions * properties.getRatingRangeIncrement()),
                    properties.getMaxRatingRange());

            Integer currentRange = playerRanges.get(k);
            if (currentRange == null || newRange > currentRange) {
                playerRanges.put(k, newRange);
                log.debug("Expanded search range for {} to ±{}", k, newRange);
            }
        });

        playerRanges.forEach((k, range) -> {
            Double rating = playerRatings.get(k);
            String timeControl = playerTimeControls.get(k);
            if (rating != null && timeControl != null) {
                int colon = k.indexOf(':');
                String userId = colon > 0 ? k.substring(colon + 1) : k;
                FindMatchRequest findRequest = FindMatchRequest.builder()
                        .userId(userId)
                        .timeControl(timeControl)
                        .rating(rating)
                        .range(range)
                        .build();
                processPlayerMatchmaking(findRequest);
            } else {
                playerQueueTimes.remove(k);
                playerRanges.remove(k);
                playerRatings.remove(k);
                playerTimeControls.remove(k);
            }
        });
    }

    private static String key(String userId, String timeControl) {
        return timeControl + ":" + userId;
    }

    private void processPlayerMatchmaking(FindMatchRequest request) {
        String userId = request.getUserId();
        String timeControl = request.getTimeControl();
        Double rating = request.getRating();
        int range = request.getRange();

        Double currentRating = queueService.getPlayerRating(userId, timeControl);
        if (currentRating == null) {
            String k = key(userId, timeControl);
            playerQueueTimes.remove(k);
            playerRanges.remove(k);
            playerRatings.remove(k);
            playerTimeControls.remove(k);
            return;
        }

        QueuedPlayer opponent = queueService.findMatchForPlayer(request);
        if (opponent != null) {
            String matchId = UUID.randomUUID().toString();
            MatchmakingProperties.TimeControlParams params = properties.getTimeControls().get(timeControl);
            int initialTimeSeconds = params != null ? params.getInitialTimeSeconds() : 180;
            int incrementSeconds = params != null ? params.getIncrementSeconds() : 2;

            String whitePlayerId = userId;
            String blackPlayerId = opponent.getUserId();
            if (Math.random() >= 0.5) {
                whitePlayerId = opponent.getUserId();
                blackPlayerId = userId;
            }

            MatchFoundDto matchFoundDto = MatchFoundDto.builder()
                    .matchId(matchId)
                    .whitePlayerId(whitePlayerId)
                    .blackPlayerId(blackPlayerId)
                    .timeControl(timeControl)
                    .initialTimeSeconds(initialTimeSeconds)
                    .incrementSeconds(incrementSeconds)
                    .build();
            try {
                eventPublisher.publishMatchFound(matchFoundDto);
            } catch (Exception e) {
                log.error("Failed to publish MatchFound event for matchId={}", matchId, e);
            }

            String k1 = key(userId, timeControl);
            String k2 = key(opponent.getUserId(), timeControl);
            playerQueueTimes.remove(k1);
            playerRanges.remove(k1);
            playerRatings.remove(k1);
            playerTimeControls.remove(k1);
            playerQueueTimes.remove(k2);
            playerRanges.remove(k2);
            playerRatings.remove(k2);
            playerTimeControls.remove(k2);
        }
    }
}
