package com.chess.common.messaging;

/**
 * Consumer-side idempotency store for NATS JetStream events (TECH SPEC: processed_event_ids).
 *
 * Implementations may use DB (preferred) or cache.
 */
public interface ProcessedEventStore {
    boolean isProcessed(String consumer, String eventId);
    void markProcessed(String consumer, String eventId);
}

