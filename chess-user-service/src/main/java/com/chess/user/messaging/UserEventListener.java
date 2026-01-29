package com.chess.user.messaging;

import com.chess.events.auth.UserRegisteredEvent;
import com.chess.events.constants.NatsSubjects;
import com.chess.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;
    private final UserService userService;

    private Dispatcher dispatcher;

    @PostConstruct
    public void init() {
        dispatcher = natsConnection.createDispatcher(this::handleMessage);
        dispatcher.subscribe(NatsSubjects.AUTH_USER_REGISTERED);
        log.info("Subscribed to {}", NatsSubjects.AUTH_USER_REGISTERED);
    }

    @PreDestroy
    public void cleanup() {
        if (dispatcher != null) {
            dispatcher.unsubscribe(NatsSubjects.AUTH_USER_REGISTERED);
        }
    }

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private void handleMessage(Message message) {
        String json = new String(message.getData(), StandardCharsets.UTF_8);
        UserRegisteredEvent event;
        try {
            event = objectMapper.readValue(json, UserRegisteredEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize UserRegistered event, skipping", e);
            return;
        }

        log.info("Received UserRegistered event: userId={}, email={}",
                event.getUserId(), event.getEmail());

        UUID userId = UUID.fromString(event.getUserId());
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                userService.createUser(userId, event.getEmail());
                return;
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} failed for UserRegistered userId={}: {}",
                        attempt, MAX_RETRIES, userId, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * (long) Math.pow(2, attempt - 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Retry interrupted for userId={}", userId);
                        break;
                    }
                }
            }
        }
        log.error("Failed to process UserRegistered event after {} attempts: userId={}, email={}",
                MAX_RETRIES, event.getUserId(), event.getEmail(), lastException);
    }
}
