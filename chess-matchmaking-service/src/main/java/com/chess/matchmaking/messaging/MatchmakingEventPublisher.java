package com.chess.matchmaking.messaging;

import com.chess.events.common.EventEnvelope;
import com.chess.events.constants.NatsSubjects;
import com.chess.events.matchmaking.MatchFoundEvent;
import com.chess.events.matchmaking.PlayerDequeuedEvent;
import com.chess.events.matchmaking.PlayerQueuedEvent;
import com.chess.events.util.EventBuilder;
import com.chess.matchmaking.dto.MatchFoundDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.impl.Headers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
public class MatchmakingEventPublisher {

    private static final String PRODUCER = "matchmaking-service";

    @Autowired(required = false)
    private Connection natsConnection;
    @Autowired(required = false)
    private JetStream jetStream;
    private final ObjectMapper objectMapper;

    public MatchmakingEventPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void publishMatchFound(MatchFoundDto dto) {
        try {
            if (natsConnection == null || natsConnection.getStatus() != Connection.Status.CONNECTED) {
                log.warn("NATS connection is not available. MatchFound event will not be published for matchId: {}",
                        dto.getMatchId());
                return;
            }

            MatchFoundEvent payload = MatchFoundEvent.builder()
                    .gameId(dto.getMatchId())
                    .whitePlayerId(dto.getWhitePlayerId())
                    .blackPlayerId(dto.getBlackPlayerId())
                    .timeControlType(dto.getTimeControl())
                    .baseSeconds(dto.getInitialTimeSeconds())
                    .incrementSeconds(dto.getIncrementSeconds())
                    .rated(dto.getRated())
                    .build();
            EventEnvelope<MatchFoundEvent> event = EventBuilder.envelope("MatchFound", PRODUCER, payload);

            String eventJson = objectMapper.writeValueAsString(event);
            Headers headers = new Headers();
            headers.put("Nats-Msg-Id", event.getEventId());
            if (event.getCorrelationId() != null) {
                headers.put("X-Correlation-Id", event.getCorrelationId());
            }

            if (jetStream != null) {
                jetStream.publish(NatsSubjects.MATCHMAKING_MATCH_FOUND, headers, eventJson.getBytes(StandardCharsets.UTF_8));
            } else {
                natsConnection.publish(NatsSubjects.MATCHMAKING_MATCH_FOUND, headers, eventJson.getBytes(StandardCharsets.UTF_8));
            }

            log.info("Published MatchFound event: matchId={}, white={}, black={}, timeControl={}",
                    dto.getMatchId(), dto.getWhitePlayerId(), dto.getBlackPlayerId(), dto.getTimeControl());
        } catch (Exception e) {
            log.error("Error publishing MatchFound event for matchId: {}", dto.getMatchId(), e);
            throw new RuntimeException("Failed to publish MatchFound event", e);
        }
    }

    @Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void publishPlayerQueued(String requestId, UUID userId, String timeControlType, int baseSeconds, int incrementSeconds, boolean rated) {
        try {
            if (natsConnection == null || natsConnection.getStatus() != Connection.Status.CONNECTED) {
                log.warn("NATS connection is not available. PlayerQueued event will not be published for requestId: {}", requestId);
                return;
            }

            PlayerQueuedEvent payload = PlayerQueuedEvent.builder()
                    .requestId(requestId)
                    .userId(userId.toString())
                    .timeControlType(timeControlType)
                    .baseSeconds(baseSeconds)
                    .incrementSeconds(incrementSeconds)
                    .rated(rated)
                    .build();

            String eventId = deterministicEventId("PlayerQueued", requestId);
            EventEnvelope<PlayerQueuedEvent> event = EventBuilder.envelope(eventId, "PlayerQueued", PRODUCER, payload);

            publish(NatsSubjects.MATCHMAKING_PLAYER_QUEUED, event);
        } catch (Exception e) {
            log.error("Error publishing PlayerQueued event requestId={}", requestId, e);
            throw new RuntimeException("Failed to publish PlayerQueued event", e);
        }
    }

    @Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void publishPlayerDequeued(String requestId, UUID userId, String reason) {
        try {
            if (natsConnection == null || natsConnection.getStatus() != Connection.Status.CONNECTED) {
                log.warn("NATS connection is not available. PlayerDequeued event will not be published for requestId: {}", requestId);
                return;
            }

            PlayerDequeuedEvent payload = PlayerDequeuedEvent.builder()
                    .requestId(requestId)
                    .userId(userId.toString())
                    .reason(reason)
                    .build();

            String eventId = deterministicEventId("PlayerDequeued", requestId);
            EventEnvelope<PlayerDequeuedEvent> event = EventBuilder.envelope(eventId, "PlayerDequeued", PRODUCER, payload);

            publish(NatsSubjects.MATCHMAKING_PLAYER_DEQUEUED, event);
        } catch (Exception e) {
            log.error("Error publishing PlayerDequeued event requestId={}", requestId, e);
            throw new RuntimeException("Failed to publish PlayerDequeued event", e);
        }
    }

    private void publish(String subject, Object event) throws Exception {
        String eventJson = objectMapper.writeValueAsString(event);

        String eventId = null;
        String correlationId = null;
        if (event instanceof EventEnvelope<?> ee) {
            eventId = ee.getEventId();
            correlationId = ee.getCorrelationId();
        }

        Headers headers = new Headers();
        if (eventId != null) {
            headers.put("Nats-Msg-Id", eventId);
        }
        if (correlationId != null) {
            headers.put("X-Correlation-Id", correlationId);
        }

        if (jetStream != null) {
            jetStream.publish(subject, headers, eventJson.getBytes(StandardCharsets.UTF_8));
        } else {
            natsConnection.publish(subject, headers, eventJson.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String deterministicEventId(String eventType, String seed) {
        return UUID.nameUUIDFromBytes((eventType + ":" + seed).getBytes(StandardCharsets.UTF_8)).toString();
    }
}
