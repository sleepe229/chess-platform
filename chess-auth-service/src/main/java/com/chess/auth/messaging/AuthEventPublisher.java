package com.chess.auth.messaging;

import com.chess.events.constants.NatsSubjects;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventPublisher {

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;

    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void publishUserRegistered(UUID userId, String email) {
        try {
            // Check if connection is still valid
            if (natsConnection == null || natsConnection.getStatus() != Connection.Status.CONNECTED) {
                log.warn("NATS connection is not available. Event will not be published for userId: {}", userId);
                return;
            }

            Map<String, Object> event = new HashMap<>();
            event.put("eventId", UUID.randomUUID().toString());
            event.put("eventType", "UserRegistered");
            event.put("eventVersion", 1);
            event.put("producer", "auth-service");
            event.put("occurredAt", Instant.now().toString());

            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId.toString());
            payload.put("email", email);
            event.put("payload", payload);

            String eventJson = objectMapper.writeValueAsString(event);
            natsConnection.publish(NatsSubjects.AUTH_USER_REGISTERED, eventJson.getBytes());

            log.info("Published UserRegistered event for userId: {}", userId);
        } catch (Exception e) {
            log.error("Error publishing UserRegistered event for userId: {}", userId, e);
            // Re-throw to trigger retry mechanism
            throw new RuntimeException("Failed to publish UserRegistered event", e);
        }
    }
}
