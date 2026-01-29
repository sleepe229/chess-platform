package com.chess.matchmaking.service;

import com.chess.events.matchmaking.PlayerDequeuedEvent;
import com.chess.events.matchmaking.PlayerQueuedEvent;
import com.chess.matchmaking.config.MatchmakingProperties;
import com.chess.matchmaking.model.QueuedPlayer;
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

    public void onPlayerQueued(PlayerQueuedEvent event) {
        String userId = event.getUserId();
        String timeControl = event.getTimeControl();
        Double rating = event.getRating() != null ? event.getRating() : 0.0;
        Double ratingDeviation = event.getRatingDeviation();

        log.info("Adding player to queue: userId={}, timeControl={}, rating={}", userId, timeControl, rating);

        queueService.addPlayerToQueue(userId, timeControl, rating, ratingDeviation);

        String k = key(userId, timeControl);
        playerQueueTimes.put(k, System.currentTimeMillis());
        playerRanges.put(k, properties.getInitialRatingRange());
        playerRatings.put(k, rating);
        playerTimeControls.put(k, timeControl);

        int range = properties.getInitialRatingRange();
        processPlayerMatchmaking(userId, timeControl, rating, range);
    }

    public void onPlayerDequeued(PlayerDequeuedEvent event) {
        String userId = event.getUserId();
        String timeControl = event.getTimeControl();

        log.info("Removing player from queue: userId={}, timeControl={}, reason={}",
                userId, timeControl, event.getReason());

        queueService.removePlayerFromQueue(userId, timeControl);

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
                processPlayerMatchmaking(userId, timeControl, rating, range);
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

    private void processPlayerMatchmaking(String userId, String timeControl, Double rating, int range) {
        Double currentRating = queueService.getPlayerRating(userId, timeControl);
        if (currentRating == null) {
            String k = key(userId, timeControl);
            playerQueueTimes.remove(k);
            playerRanges.remove(k);
            playerRatings.remove(k);
            playerTimeControls.remove(k);
            return;
        }

        QueuedPlayer opponent = queueService.findMatchForPlayer(userId, timeControl, rating, range);
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

            try {
                eventPublisher.publishMatchFound(
                        matchId,
                        whitePlayerId,
                        blackPlayerId,
                        timeControl,
                        initialTimeSeconds,
                        incrementSeconds);
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
