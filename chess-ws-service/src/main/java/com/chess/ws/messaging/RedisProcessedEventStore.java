package com.chess.ws.messaging;

import com.chess.common.messaging.ProcessedEventStore;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Redis-backed idempotency store for game events.
 * Used when ws.processed-event-store.type=redis so multiple WS replicas can share the same store.
 */
public class RedisProcessedEventStore implements ProcessedEventStore {

    private static final String KEY_PREFIX = "ws:processed:";
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    public RedisProcessedEventStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isProcessed(String consumer, String eventId) {
        String key = key(consumer, eventId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public void markProcessed(String consumer, String eventId) {
        String key = key(consumer, eventId);
        redisTemplate.opsForValue().set(key, "1", TTL);
    }

    private static String key(String consumer, String eventId) {
        return KEY_PREFIX + consumer + ":" + eventId;
    }
}
