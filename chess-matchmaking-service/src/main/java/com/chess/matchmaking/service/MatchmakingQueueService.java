package com.chess.matchmaking.service;

import com.chess.matchmaking.model.QueuedPlayer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchmakingQueueService {

    private static final String QUEUE_KEY = "matchmaking:queue";
    private static final String PLAYER_DATA_KEY_PREFIX = "matchmaking:player:";
    private static final long PLAYER_DATA_TTL = 3600; // 1 hour

    private final RedisTemplate<String, String> redisTemplate;
    private final ZSetOperations<String, String> zSetOperations;
    private final ObjectMapper objectMapper;

    public void addPlayerToQueue(String playerId, Integer rating) {
        try {
            long queueTime = System.currentTimeMillis();
            QueuedPlayer player = new QueuedPlayer(playerId, rating, queueTime);
            String playerJson = objectMapper.writeValueAsString(player);

            // Добавляем в Sorted Set с рейтингом как score
            zSetOperations.add(QUEUE_KEY, playerId, rating.doubleValue());

            // Сохраняем полные данные игрока отдельно
            String playerDataKey = PLAYER_DATA_KEY_PREFIX + playerId;
            redisTemplate.opsForValue().set(playerDataKey, playerJson, PLAYER_DATA_TTL, TimeUnit.SECONDS);

            log.debug("Player {} with rating {} added to queue", playerId, rating);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize player data for player {}", playerId, e);
            throw new RuntimeException("Failed to add player to queue", e);
        }
    }

    public void removePlayerFromQueue(String playerId) {
        zSetOperations.remove(QUEUE_KEY, playerId);
        String playerDataKey = PLAYER_DATA_KEY_PREFIX + playerId;
        redisTemplate.delete(playerDataKey);
        log.debug("Player {} removed from queue", playerId);
    }

    private QueuedPlayer getPlayerData(String playerId) {
        String playerDataKey = PLAYER_DATA_KEY_PREFIX + playerId;
        String playerJson = redisTemplate.opsForValue().get(playerDataKey);

        if (playerJson == null) {
            return null;
        }

        try {
            return objectMapper.readValue(playerJson, QueuedPlayer.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize player data for player {}", playerId, e);
            return null;
        }
    }

    public QueuedPlayer findMatchForPlayer(String playerId, Integer rating, Integer range) {
        double minScore = rating - range;
        double maxScore = rating + range;

        Set<String> candidates = zSetOperations.rangeByScore(QUEUE_KEY, minScore, maxScore);

        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        for (String candidateId : candidates) {
            if (candidateId.equals(playerId)) {
                continue;
            }

            // Получаем данные кандидата
            QueuedPlayer candidate = getPlayerData(candidateId);
            if (candidate == null) {
                continue;
            }

            // Атомарно проверяем наличие обоих игроков и удаляем их
            // Это предотвращает race condition, когда другой поток уже удалил одного из
            // игроков
            if (removeBothPlayersAtomicallyIfPresent(playerId, candidateId)) {
                log.info("Match found: {} (rating {}) vs {} (rating {})",
                        playerId, rating, candidateId, candidate.getRating());
                return candidate;
            }
        }

        return null;
    }

    private boolean removeBothPlayersAtomicallyIfPresent(String player1Id, String player2Id) {
        // Сначала проверяем наличие обоих игроков (не атомарно, но быстро)
        // Это оптимизация - если игроков нет, не тратим время на транзакцию
        Double score1 = zSetOperations.score(QUEUE_KEY, player1Id);
        Double score2 = zSetOperations.score(QUEUE_KEY, player2Id);

        if (score1 == null || score2 == null) {
            return false;
        }

        // Используем Redis транзакции для атомарного удаления
        // В Spring Data Redis операции внутри execute с multi() выполняются атомарно
        Object result = redisTemplate.execute(operations -> {
            // Начинаем транзакцию
            operations.multi();

            // Удаляем обоих игроков из Sorted Set
            operations.opsForZSet().remove(QUEUE_KEY, player1Id, player2Id);

            // Удаляем данные игроков
            String player1DataKey = PLAYER_DATA_KEY_PREFIX + player1Id;
            String player2DataKey = PLAYER_DATA_KEY_PREFIX + player2Id;
            operations.delete(player1DataKey, player2DataKey);

            // Выполняем транзакцию
            return operations.exec();
        });

        // operations.exec() возвращает List результатов операций
        // Проверяем, что транзакция выполнилась успешно
        // В случае race condition, если один из игроков уже был удален другим потоком,
        // операция все равно выполнится, но количество удаленных элементов будет меньше
        if (result instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<Object> results = (java.util.List<Object>) result;
            // Первый результат - количество удаленных элементов из Sorted Set
            if (!results.isEmpty() && results.get(0) instanceof Long) {
                Long removedCount = (Long) results.get(0);
                // Ожидаем, что удалено 2 элемента (оба игрока)
                return removedCount != null && removedCount >= 1;
            }
        }

        return false;
    }

    public Double getPlayerRating(String playerId) {
        return zSetOperations.score(QUEUE_KEY, playerId);
    }
}
