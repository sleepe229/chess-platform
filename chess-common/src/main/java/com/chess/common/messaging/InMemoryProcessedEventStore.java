package com.chess.common.messaging;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryProcessedEventStore implements ProcessedEventStore {

    private final Map<String, Long> expiresAtByKey = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final Clock clock;

    private volatile long calls;

    public InMemoryProcessedEventStore(Duration ttl) {
        this(ttl, Clock.systemUTC());
    }

    public InMemoryProcessedEventStore(Duration ttl, Clock clock) {
        this.ttl = ttl;
        this.clock = clock;
    }

    @Override
    public boolean isProcessed(String consumer, String eventId) {
        cleanupOccasionally();
        long now = clock.millis();
        Long exp = expiresAtByKey.get(key(consumer, eventId));
        if (exp == null) return false;
        if (exp <= now) {
            expiresAtByKey.remove(key(consumer, eventId));
            return false;
        }
        return true;
    }

    @Override
    public void markProcessed(String consumer, String eventId) {
        cleanupOccasionally();
        long now = clock.millis();
        expiresAtByKey.put(key(consumer, eventId), now + ttl.toMillis());
    }

    private String key(String consumer, String eventId) {
        return consumer + ":" + eventId;
    }

    private void cleanupOccasionally() {
        // Cheap periodic cleanup to prevent unbounded growth
        long c = ++calls;
        if (c % 10_000 != 0) {
            return;
        }
        long now = clock.millis();
        expiresAtByKey.entrySet().removeIf(e -> e.getValue() <= now);
    }
}

