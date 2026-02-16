package com.chess.analytics.messaging;

import com.chess.common.messaging.ProcessedEventStore;
import com.chess.events.common.EventEnvelope;
import com.chess.events.constants.NatsSubjects;
import com.chess.events.game.GameFinishedEvent;
import com.chess.events.game.GameStartedEvent;
import com.chess.events.game.MoveMadeEvent;
import com.chess.events.game.TimeExpiredEvent;
import com.chess.analytics.service.GameFactsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.*;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import io.nats.client.api.ReplayPolicy;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "nats.enabled", havingValue = "true", matchIfMissing = true)
public class GameEventsListener {

    private static final String CONSUMER = "analytics-service-domain-game";
    private static final String DURABLE_PREFIX = "analytics-game-";
    private static final String DURABLE_STARTED = DURABLE_PREFIX + "started-v1";
    private static final String DURABLE_MOVE = DURABLE_PREFIX + "move-v1";
    private static final String DURABLE_FINISHED = DURABLE_PREFIX + "finished-v1";
    private static final String DURABLE_TIME_EXPIRED = DURABLE_PREFIX + "time-expired-v1";

    private final Connection natsConnection;
    private final JetStream jetStream;
    private final ObjectMapper objectMapper;
    private final ProcessedEventStore processedEventStore;
    private final GameFactsService gameFactsService;

    private Dispatcher dispatcher;
    private JetStreamSubscription subStarted;
    private JetStreamSubscription subMove;
    private JetStreamSubscription subFinished;
    private JetStreamSubscription subTimeExpired;

    @PostConstruct
    public void init() {
        try {
            dispatcher = natsConnection.createDispatcher();
            subStarted = jetStream.subscribe(NatsSubjects.GAME_STARTED, dispatcher, this::onStarted, false,
                    PushSubscribeOptions.builder().configuration(consumerConfig(DURABLE_STARTED)).build());
            subMove = jetStream.subscribe(NatsSubjects.GAME_MOVE_MADE, dispatcher, this::onMove, false,
                    PushSubscribeOptions.builder().configuration(consumerConfig(DURABLE_MOVE)).build());
            subFinished = jetStream.subscribe(NatsSubjects.GAME_FINISHED, dispatcher, this::onFinished, false,
                    PushSubscribeOptions.builder().configuration(consumerConfig(DURABLE_FINISHED)).build());
            subTimeExpired = jetStream.subscribe(NatsSubjects.GAME_TIME_EXPIRED, dispatcher, this::onTimeExpired, false,
                    PushSubscribeOptions.builder().configuration(consumerConfig(DURABLE_TIME_EXPIRED)).build());
            log.info("Subscribed to game events: {}, {}, {}, {}", NatsSubjects.GAME_STARTED, NatsSubjects.GAME_MOVE_MADE, NatsSubjects.GAME_FINISHED, NatsSubjects.GAME_TIME_EXPIRED);
        } catch (Exception e) {
            log.error("Failed to subscribe to game events", e);
        }
    }

    private ConsumerConfiguration consumerConfig(String durable) {
        return ConsumerConfiguration.builder()
                .durable(durable)
                .ackPolicy(AckPolicy.Explicit)
                .ackWait(Duration.ofSeconds(30))
                .maxDeliver(5)
                .backoff(Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(15), Duration.ofSeconds(30), Duration.ofSeconds(60))
                .deliverPolicy(DeliverPolicy.All)
                .replayPolicy(ReplayPolicy.Instant)
                .build();
    }

    @PreDestroy
    public void cleanup() {
        tryUnsub(subStarted);
        tryUnsub(subMove);
        tryUnsub(subFinished);
        tryUnsub(subTimeExpired);
    }

    private void onStarted(Message msg) {
        try {
            EventEnvelope<GameStartedEvent> env = objectMapper.readValue(new String(msg.getData(), StandardCharsets.UTF_8),
                    objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, GameStartedEvent.class));
            if (env.getEventId() != null && processedEventStore.isProcessed(CONSUMER, env.getEventId())) {
                msg.ack();
                return;
            }
            if (env.getPayload() != null) {
                gameFactsService.onGameStarted(env.getPayload());
            }
            if (env.getEventId() != null) processedEventStore.markProcessed(CONSUMER, env.getEventId());
            msg.ack();
        } catch (Exception ex) {
            log.warn("Failed to handle GameStarted", ex);
            safeNak(msg);
        }
    }

    private void onMove(Message msg) {
        try {
            EventEnvelope<MoveMadeEvent> env = objectMapper.readValue(new String(msg.getData(), StandardCharsets.UTF_8),
                    objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, MoveMadeEvent.class));
            if (env.getEventId() != null && processedEventStore.isProcessed(CONSUMER, env.getEventId())) {
                msg.ack();
                return;
            }
            if (env.getPayload() != null) {
                gameFactsService.onMoveMade(env.getPayload());
            }
            if (env.getEventId() != null) processedEventStore.markProcessed(CONSUMER, env.getEventId());
            msg.ack();
        } catch (Exception ex) {
            log.warn("Failed to handle MoveMade", ex);
            safeNak(msg);
        }
    }

    private void onFinished(Message msg) {
        try {
            EventEnvelope<GameFinishedEvent> env = objectMapper.readValue(new String(msg.getData(), StandardCharsets.UTF_8),
                    objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, GameFinishedEvent.class));
            if (env.getEventId() != null && processedEventStore.isProcessed(CONSUMER, env.getEventId())) {
                msg.ack();
                return;
            }
            if (env.getPayload() != null) {
                gameFactsService.onGameFinished(env.getPayload());
            }
            if (env.getEventId() != null) processedEventStore.markProcessed(CONSUMER, env.getEventId());
            msg.ack();
        } catch (Exception ex) {
            log.warn("Failed to handle GameFinished", ex);
            safeNak(msg);
        }
    }

    private void onTimeExpired(Message msg) {
        try {
            EventEnvelope<TimeExpiredEvent> env = objectMapper.readValue(new String(msg.getData(), StandardCharsets.UTF_8),
                    objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, TimeExpiredEvent.class));
            if (env.getEventId() != null && processedEventStore.isProcessed(CONSUMER, env.getEventId())) {
                msg.ack();
                return;
            }
            // No read-model update; GameFinished will carry the result.
            if (env.getEventId() != null) processedEventStore.markProcessed(CONSUMER, env.getEventId());
            msg.ack();
        } catch (Exception ex) {
            log.warn("Failed to handle TimeExpired", ex);
            safeNak(msg);
        }
    }

    private void tryUnsub(JetStreamSubscription sub) {
        if (sub == null) return;
        try {
            sub.unsubscribe();
        } catch (Exception ignored) {
        }
    }

    private void safeNak(Message msg) {
        try {
            msg.nak();
        } catch (Exception ignored) {
        }
    }
}
