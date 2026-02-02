package com.chess.events.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Event Bus envelope (per TECHNICAL SPECIFICATION):
 * { eventId, eventType, eventVersion, producer, occurredAt, correlationId, payload }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventEnvelope<T> implements Serializable {
    private String eventId;
    private String eventType;
    private Integer eventVersion;
    private String producer;
    private String occurredAt;
    private String correlationId;
    private T payload;
}

