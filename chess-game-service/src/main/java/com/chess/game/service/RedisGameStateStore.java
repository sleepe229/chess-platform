package com.chess.game.service;

import com.chess.game.state.GameState;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisGameStateStore implements GameStateStore {

    private static final String GAME_KEY_PREFIX = "game:";
    private static final String LOCK_KEY_PREFIX = "lock:game:";
    private static final String CLIENT_MOVE_PREFIX = "game:clientMove:";
    private static final String TIMEOUT_ZSET_KEY = "game:timeoutIndex";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<GameState> get(UUID gameId) {
        String key = GAME_KEY_PREFIX + gameId;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, GameState.class));
        } catch (Exception e) {
            log.error("Failed to deserialize GameState from redis for gameId={}", gameId, e);
            return Optional.empty();
        }
    }

    @Override
    public void put(GameState state, Duration ttl) {
        String key = GAME_KEY_PREFIX + state.getGameId();
        try {
            String json = objectMapper.writeValueAsString(state);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize GameState", e);
        }
    }

    @Override
    public boolean tryLock(UUID gameId, Duration ttl) {
        String key = LOCK_KEY_PREFIX + gameId;
        String token = UUID.randomUUID().toString();
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        return ok != null && ok;
    }

    @Override
    public void unlock(UUID gameId) {
        String key = LOCK_KEY_PREFIX + gameId;
        redisTemplate.delete(key);
    }

    @Override
    public Optional<String> getClientMoveResult(UUID gameId, UUID clientMoveId) {
        String key = CLIENT_MOVE_PREFIX + gameId + ":" + clientMoveId;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(json);
    }

    @Override
    public void rememberClientMoveResult(UUID gameId, UUID clientMoveId, String resultJson, Duration ttl) {
        String key = CLIENT_MOVE_PREFIX + gameId + ":" + clientMoveId;
        redisTemplate.opsForValue().set(key, resultJson, ttl);
    }

    @Override
    public void upsertTimeoutDeadline(UUID gameId, long deadlineEpochMs) {
        redisTemplate.opsForZSet().add(TIMEOUT_ZSET_KEY, gameId.toString(), deadlineEpochMs);
    }

    @Override
    public void removeTimeoutDeadline(UUID gameId) {
        redisTemplate.opsForZSet().remove(TIMEOUT_ZSET_KEY, gameId.toString());
    }

    @Override
    public List<UUID> pollExpiredTimeouts(long nowEpochMs, int limit) {
        Set<String> ids = redisTemplate.opsForZSet()
                .rangeByScore(TIMEOUT_ZSET_KEY, 0, nowEpochMs, 0, limit);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().map(UUID::fromString).toList();
    }
}

