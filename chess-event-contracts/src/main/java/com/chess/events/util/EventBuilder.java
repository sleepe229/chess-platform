package com.chess.events.util;

import com.chess.events.common.EventEnvelope;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.UUID;

public class EventBuilder {

    public static <T> EventEnvelope<T> envelope(String eventType, String producer, T payload) {
        return envelope(null, eventType, producer, payload);
    }

    public static <T> EventEnvelope<T> envelope(String eventId, String eventType, String producer, T payload) {
        String correlationId = MDC.get("traceId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        return EventEnvelope.<T>builder()
                .eventId(eventId != null && !eventId.isBlank() ? eventId : UUID.randomUUID().toString())
                .eventType(eventType)
                .eventVersion(1)
                .producer(producer)
                .occurredAt(Instant.now().toString())
                .correlationId(correlationId)
                .payload(payload)
                .build();
    }
}
