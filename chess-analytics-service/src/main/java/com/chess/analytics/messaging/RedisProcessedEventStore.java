package com.chess.analytics.messaging;

import com.chess.common.messaging.ProcessedEventStore;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

public class RedisProcessedEventStore implements ProcessedEventStore {

    private static final String KEY_PREFIX = "analytics:processed:";
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    public RedisProcessedEventStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isProcessed(String consumer, String eventId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + consumer + ":" + eventId));
    }

    @Override
    public void markProcessed(String consumer, String eventId) {
        redisTemplate.opsForValue().set(KEY_PREFIX + consumer + ":" + eventId, "1", TTL);
    }
}
