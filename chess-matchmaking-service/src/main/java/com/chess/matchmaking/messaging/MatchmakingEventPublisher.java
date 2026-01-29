package com.chess.matchmaking.messaging;

import com.chess.events.constants.NatsSubjects;
import com.chess.events.matchmaking.MatchFoundEvent;
import com.chess.events.util.EventBuilder;
import com.chess.matchmaking.dto.MatchFoundDto;
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
    public void publishMatchFound(MatchFoundDto dto) {
        try {
            if (natsConnection == null || natsConnection.getStatus() != Connection.Status.CONNECTED) {
                log.warn("NATS connection is not available. MatchFound event will not be published for matchId: {}",
                        dto.getMatchId());
                return;
            }

            MatchFoundEvent event = MatchFoundEvent.builder()
                    .matchId(dto.getMatchId())
                    .whitePlayerId(dto.getWhitePlayerId())
                    .blackPlayerId(dto.getBlackPlayerId())
                    .timeControl(dto.getTimeControl())
                    .initialTimeSeconds(dto.getInitialTimeSeconds())
                    .incrementSeconds(dto.getIncrementSeconds())
                    .build();
            EventBuilder.enrichEvent(event, PRODUCER);

            String eventJson = objectMapper.writeValueAsString(event);
            natsConnection.publish(NatsSubjects.MATCHMAKING_MATCH_FOUND, eventJson.getBytes());

            log.info("Published MatchFound event: matchId={}, white={}, black={}, timeControl={}",
                    dto.getMatchId(), dto.getWhitePlayerId(), dto.getBlackPlayerId(), dto.getTimeControl());
        } catch (Exception e) {
            log.error("Error publishing MatchFound event for matchId: {}", dto.getMatchId(), e);
            throw new RuntimeException("Failed to publish MatchFound event", e);
        }
    }
}
