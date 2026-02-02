package com.chess.user.messaging;

import com.chess.events.common.EventEnvelope;
import com.chess.events.constants.NatsSubjects;
import com.chess.events.users.RatingUpdatedEvent;
import com.chess.events.util.EventBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.impl.Headers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
public class UserEventPublisher {

    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private Connection natsConnection;

    @Autowired(required = false)
    private JetStream jetStream;

    public UserEventPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void publishRatingUpdated(
            UUID userId,
            String timeControl,
            double oldRating,
            double newRating,
            double oldRd,
            double newRd,
            UUID gameId) {

        try {
            if (natsConnection == null || natsConnection.getStatus() != Connection.Status.CONNECTED) {
                log.warn("NATS connection is not available. RatingUpdated event will not be published for userId={}", userId);
                return;
            }

            RatingUpdatedEvent payload = RatingUpdatedEvent.builder()
                    .userId(userId.toString())
                    .timeControl(timeControl)
                    .oldRating(oldRating)
                    .newRating(newRating)
                    .oldRd(oldRd)
                    .newRd(newRd)
                    .gameId(gameId != null ? gameId.toString() : null)
                    .build();

            EventEnvelope<RatingUpdatedEvent> event = EventBuilder.envelope("RatingUpdated", "user-service", payload);

            String eventJson = objectMapper.writeValueAsString(event);
            Headers headers = new Headers();
            headers.put("Nats-Msg-Id", event.getEventId());
            if (event.getCorrelationId() != null) {
                headers.put("X-Correlation-Id", event.getCorrelationId());
            }

            if (jetStream != null) {
                jetStream.publish(NatsSubjects.USER_RATING_UPDATED, headers, eventJson.getBytes(StandardCharsets.UTF_8));
            } else {
                natsConnection.publish(NatsSubjects.USER_RATING_UPDATED, headers, eventJson.getBytes(StandardCharsets.UTF_8));
            }

            log.info("Published RatingUpdated event: userId={}, timeControl={}, newRating={}",
                    userId, timeControl, newRating);

        } catch (Exception e) {
            log.error("Error publishing RatingUpdated event", e);
        }
    }
}
