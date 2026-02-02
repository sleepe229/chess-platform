package com.chess.auth.messaging;

import com.chess.events.auth.UserRegisteredEvent;
import com.chess.events.common.EventEnvelope;
import com.chess.events.constants.NatsSubjects;
import com.chess.events.util.EventBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.impl.Headers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class AuthEventPublisher {

    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private Connection natsConnection;

    @Autowired(required = false)
    private JetStream jetStream;

    public AuthEventPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void publishUserRegistered(UUID userId, String email) {
        try {
            if (natsConnection == null || natsConnection.getStatus() != Connection.Status.CONNECTED) {
                log.warn("NATS connection is not available. Event will not be published for userId: {}", userId);
                return;
            }

            UserRegisteredEvent payload = UserRegisteredEvent.builder()
                    .userId(userId.toString())
                    .email(email)
                    .build();
            EventEnvelope<UserRegisteredEvent> event = EventBuilder.envelope("UserRegistered", "auth-service", payload);

            String eventJson = objectMapper.writeValueAsString(event);
            Headers headers = new Headers();
            headers.put("Nats-Msg-Id", event.getEventId());
            if (event.getCorrelationId() != null) {
                headers.put("X-Correlation-Id", event.getCorrelationId());
            }

            if (jetStream != null) {
                jetStream.publish(NatsSubjects.AUTH_USER_REGISTERED, headers, eventJson.getBytes());
            } else {
                natsConnection.publish(NatsSubjects.AUTH_USER_REGISTERED, headers, eventJson.getBytes());
            }

            log.info("Published UserRegistered event for userId: {}", userId);
        } catch (Exception e) {
            log.error("Error publishing UserRegistered event for userId: {}", userId, e);
            throw new RuntimeException("Failed to publish UserRegistered event", e);
        }
    }
}
