package com.chess.matchmaking.repo;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisMatchmakingEngine {

    private static final String REQ_KEY_PREFIX = "mm:req:";
    private static final String QUEUE_KEY_PREFIX = "mm:queue:";
    private static final String QUEUE_TS_KEY_PREFIX = "mm:queue_ts:";

    private final RedisTemplate<String, String> redisTemplate;
    @SuppressWarnings("rawtypes")
    private final RedisScript<List> tryMatchScript;

    public record MatchPair(String requestId1, String requestId2) {
    }

    public MatchPair enqueueAndTryMatch(String timeControlType, String requestId, double rating,
                                        int initialRange, int rangeIncrement, int maxRange, long expansionIntervalMs) {
        String queueKey = QUEUE_KEY_PREFIX + timeControlType;
        String queueTsKey = QUEUE_TS_KEY_PREFIX + timeControlType;

        List<String> keys = List.of(queueKey, queueTsKey);
        @SuppressWarnings("unchecked")
        List<String> pair = redisTemplate.execute(
                tryMatchScript,
                keys,
                requestId,
                String.valueOf(rating),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(initialRange),
                String.valueOf(rangeIncrement),
                String.valueOf(maxRange),
                String.valueOf(expansionIntervalMs),
                REQ_KEY_PREFIX,
                "50"
        );

        if (pair == null || pair.size() != 2) {
            return null;
        }
        return new MatchPair(pair.get(0), pair.get(1));
    }

    public void removeFromQueues(String timeControlType, String requestId) {
        String queueKey = QUEUE_KEY_PREFIX + timeControlType;
        String queueTsKey = QUEUE_TS_KEY_PREFIX + timeControlType;
        redisTemplate.opsForZSet().remove(queueKey, requestId);
        redisTemplate.opsForZSet().remove(queueTsKey, requestId);
    }

    public List<String> findExpired(String timeControlType, long expireBeforeMs, int limit) {
        String queueTsKey = QUEUE_TS_KEY_PREFIX + timeControlType;
        Set<String> ids = redisTemplate.opsForZSet()
                .rangeByScore(queueTsKey, 0, expireBeforeMs, 0, limit);
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream().toList();
    }
}

