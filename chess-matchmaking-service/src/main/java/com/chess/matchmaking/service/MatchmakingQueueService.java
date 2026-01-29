package com.chess.matchmaking.service;

import com.chess.matchmaking.model.QueuedPlayer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchmakingQueueService {

    private static final String QUEUE_KEY_PREFIX = "matchmaking:queue:";
    private static final String PLAYER_DATA_KEY_PREFIX = "matchmaking:player:";
    private static final long PLAYER_DATA_TTL = 3600;

    private final RedisTemplate<String, String> redisTemplate;
    private final ZSetOperations<String, String> zSetOperations;
    private final ObjectMapper objectMapper;
    @SuppressWarnings("rawtypes")
    private final RedisScript<List> findAndRemovePairScript;

    public void addPlayerToQueue(String userId, String timeControl, Double rating, Double ratingDeviation) {
        try {
            long queueTime = System.currentTimeMillis();
            QueuedPlayer player = QueuedPlayer.builder()
                    .userId(userId)
                    .timeControl(timeControl)
                    .rating(rating)
                    .ratingDeviation(ratingDeviation != null ? ratingDeviation : 0.0)
                    .queueTime(queueTime)
                    .build();
            String playerJson = objectMapper.writeValueAsString(player);

            String queueKey = queueKey(timeControl);
            zSetOperations.add(queueKey, userId, rating);

            String playerDataKey = playerDataKey(timeControl, userId);
            redisTemplate.opsForValue().set(playerDataKey, playerJson, PLAYER_DATA_TTL, TimeUnit.SECONDS);

            log.debug("Player {} with rating {} added to queue for timeControl {}", userId, rating, timeControl);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize player data for userId {}", userId, e);
            throw new RuntimeException("Failed to add player to queue", e);
        }
    }

    public void removePlayerFromQueue(String userId, String timeControl) {
        String queueKey = queueKey(timeControl);
        zSetOperations.remove(queueKey, userId);
        String playerDataKey = playerDataKey(timeControl, userId);
        redisTemplate.delete(playerDataKey);
        log.debug("Player {} removed from queue for timeControl {}", userId, timeControl);
    }

    /**
     * Атомарно ищет соперника в диапазоне рейтинга и удаляет обоих из пула
     * (Lua-скрипт).
     * Один round-trip к Redis, полная атомарность без гонок.
     */
    @SuppressWarnings("unchecked")
    public QueuedPlayer findMatchForPlayer(String userId, String timeControl, Double rating, int range) {
        String queueKey = queueKey(timeControl);
        List<String> keys = Collections.singletonList(queueKey);

        List<String> pair = redisTemplate.execute(
                findAndRemovePairScript,
                keys,
                userId,
                String.valueOf(rating),
                String.valueOf(range));

        if (pair == null || pair.size() != 2) {
            return null;
        }

        String opponentId = userId.equals(pair.get(0)) ? pair.get(1) : pair.get(0);
        QueuedPlayer opponent = getPlayerData(timeControl, opponentId);
        if (opponent == null) {
            log.warn("Opponent {} data not found after script match, building minimal QueuedPlayer", opponentId);
            opponent = QueuedPlayer.builder()
                    .userId(opponentId)
                    .timeControl(timeControl)
                    .rating(0.0)
                    .ratingDeviation(0.0)
                    .queueTime(0L)
                    .build();
        }

        redisTemplate.delete(List.of(
                playerDataKey(timeControl, userId),
                playerDataKey(timeControl, opponentId)));

        log.info("Match found (Lua): {} (rating {}) vs {} for timeControl {}",
                userId, rating, opponentId, timeControl);
        return opponent;
    }

    public Double getPlayerRating(String userId, String timeControl) {
        return zSetOperations.score(queueKey(timeControl), userId);
    }

    private String queueKey(String timeControl) {
        return QUEUE_KEY_PREFIX + timeControl;
    }

    private String playerDataKey(String timeControl, String userId) {
        return PLAYER_DATA_KEY_PREFIX + timeControl + ":" + userId;
    }

    private QueuedPlayer getPlayerData(String timeControl, String userId) {
        String key = playerDataKey(timeControl, userId);
        String playerJson = redisTemplate.opsForValue().get(key);

        if (playerJson == null) {
            return null;
        }

        try {
            return objectMapper.readValue(playerJson, QueuedPlayer.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize player data for userId {}", userId, e);
            return null;
        }
    }
}
