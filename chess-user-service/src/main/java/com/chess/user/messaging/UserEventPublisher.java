package com.chess.user.messaging;

import com.chess.events.constants.NatsSubjects;
import com.chess.events.user.RatingUpdatedEvent;
import com.chess.events.util.EventBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventPublisher {

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;

    public void publishRatingUpdated(
            UUID userId,
            String timeControl,
            double oldRating,
            double newRating,
            double oldRd,
            double newRd,
            UUID gameId) {

        try {
            RatingUpdatedEvent event = RatingUpdatedEvent.builder()
                    .userId(userId.toString())
                    .timeControl(timeControl)
                    .oldRating(oldRating)
                    .newRating(newRating)
                    .oldRd(oldRd)
                    .newRd(newRd)
                    .gameId(gameId != null ? gameId.toString() : null)
                    .build();

            EventBuilder.enrichEvent(event, "user-service");

            String eventJson = objectMapper.writeValueAsString(event);
            natsConnection.publish(NatsSubjects.USER_RATING_UPDATED, eventJson.getBytes());

            log.info("Published RatingUpdated event: userId={}, timeControl={}, newRating={}",
                    userId, timeControl, newRating);

        } catch (Exception e) {
            log.error("Error publishing RatingUpdated event", e);
        }
    }
}
