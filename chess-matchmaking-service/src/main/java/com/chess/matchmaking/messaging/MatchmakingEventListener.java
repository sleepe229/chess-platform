package com.chess.matchmaking.messaging;

import com.chess.events.common.DomainEvent;
import com.chess.events.matchmaking.PlayerDequeuedEvent;
import com.chess.events.matchmaking.PlayerQueuedEvent;
import com.chess.matchmaking.dto.PlayerDequeuedDto;
import com.chess.matchmaking.dto.PlayerQueuedDto;
import com.chess.matchmaking.service.MatchmakingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class MatchmakingEventListener {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    @Autowired(required = false)
    private Connection natsConnection;
    private final ObjectMapper objectMapper;
    private final MatchmakingService matchmakingService;

    public MatchmakingEventListener(ObjectMapper objectMapper, MatchmakingService matchmakingService) {
        this.objectMapper = objectMapper;
        this.matchmakingService = matchmakingService;
    }

    private Dispatcher dispatcher;

    @PostConstruct
    public void init() {
        if (natsConnection == null) {
            log.warn("NATS connection is null, matchmaking listener will not start");
            return;
        }
        dispatcher = natsConnection.createDispatcher(this::handleMessage);
        dispatcher.subscribe(com.chess.events.constants.NatsSubjects.MATCHMAKING_PLAYER_QUEUED);
        dispatcher.subscribe(com.chess.events.constants.NatsSubjects.MATCHMAKING_PLAYER_DEQUEUED);
        log.info("Subscribed to {} and {}",
                com.chess.events.constants.NatsSubjects.MATCHMAKING_PLAYER_QUEUED,
                com.chess.events.constants.NatsSubjects.MATCHMAKING_PLAYER_DEQUEUED);
    }

    @PreDestroy
    public void cleanup() {
        if (dispatcher != null) {
            dispatcher.unsubscribe(com.chess.events.constants.NatsSubjects.MATCHMAKING_PLAYER_QUEUED);
            dispatcher.unsubscribe(com.chess.events.constants.NatsSubjects.MATCHMAKING_PLAYER_DEQUEUED);
        }
    }

    private void handleMessage(Message message) {
        String json = new String(message.getData(), StandardCharsets.UTF_8);
        DomainEvent event;
        try {
            event = objectMapper.readValue(json, DomainEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize matchmaking event, skipping", e);
            return;
        }

        if (event instanceof PlayerQueuedEvent e) {
            handlePlayerQueued(e);
        } else if (event instanceof PlayerDequeuedEvent e) {
            handlePlayerDequeued(e);
        } else {
            log.debug("Ignoring event type: {}", event.getEventType());
        }
    }

    private void handlePlayerQueued(PlayerQueuedEvent event) {
        log.info("Received PlayerQueued event: userId={}, timeControl={}, rating={}",
                event.getUserId(), event.getTimeControl(), event.getRating());

        PlayerQueuedDto dto = PlayerQueuedDto.builder()
                .userId(event.getUserId())
                .timeControl(event.getTimeControl())
                .rating(event.getRating())
                .ratingDeviation(event.getRatingDeviation())
                .build();

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                matchmakingService.onPlayerQueued(dto);
                return;
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} failed for PlayerQueued userId={}: {}",
                        attempt, MAX_RETRIES, event.getUserId(), e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * (long) Math.pow(2, attempt - 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Retry interrupted for userId={}", event.getUserId());
                        break;
                    }
                }
            }
        }
        log.error("Failed to process PlayerQueued event after {} attempts: userId={}",
                MAX_RETRIES, event.getUserId(), lastException);
    }

    private void handlePlayerDequeued(PlayerDequeuedEvent event) {
        log.info("Received PlayerDequeued event: userId={}, timeControl={}, reason={}",
                event.getUserId(), event.getTimeControl(), event.getReason());

        PlayerDequeuedDto dto = PlayerDequeuedDto.builder()
                .userId(event.getUserId())
                .timeControl(event.getTimeControl())
                .reason(event.getReason())
                .build();

        try {
            matchmakingService.onPlayerDequeued(dto);
        } catch (Exception e) {
            log.error("Error processing PlayerDequeued event for userId={}", event.getUserId(), e);
        }
    }
}
