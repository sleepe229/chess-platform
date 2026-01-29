package com.chess.matchmaking.messaging;

import com.chess.events.constants.NatsSubjects;
import com.chess.events.matchmaking.MatchFoundEvent;
import com.chess.events.util.EventBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MatchmakingEventPublisher {

    private static final String PRODUCER = "matchmaking-service";

    @Autowired(required = false)
    private Connection natsConnection;
    private final ObjectMapper objectMapper;

    public MatchmakingEventPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void publishMatchFound(
            String matchId,
            String whitePlayerId,
            String blackPlayerId,
            String timeControl,
            int initialTimeSeconds,
            int incrementSeconds) {
        try {
            if (natsConnection == null || natsConnection.getStatus() != Connection.Status.CONNECTED) {
                log.warn("NATS connection is not available. MatchFound event will not be published for matchId: {}",
                        matchId);
                return;
            }

            MatchFoundEvent event = MatchFoundEvent.builder()
                    .matchId(matchId)
                    .whitePlayerId(whitePlayerId)
                    .blackPlayerId(blackPlayerId)
                    .timeControl(timeControl)
                    .initialTimeSeconds(initialTimeSeconds)
                    .incrementSeconds(incrementSeconds)
                    .build();
            EventBuilder.enrichEvent(event, PRODUCER);

            String eventJson = objectMapper.writeValueAsString(event);
            natsConnection.publish(NatsSubjects.MATCHMAKING_MATCH_FOUND, eventJson.getBytes());

            log.info("Published MatchFound event: matchId={}, white={}, black={}, timeControl={}",
                    matchId, whitePlayerId, blackPlayerId, timeControl);
        } catch (Exception e) {
            log.error("Error publishing MatchFound event for matchId: {}", matchId, e);
            throw new RuntimeException("Failed to publish MatchFound event", e);
        }
    }
}
