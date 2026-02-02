package com.chess.game.messaging;

import com.chess.events.constants.NatsSubjects;
import com.chess.events.matchmaking.MatchFoundEvent;
import com.chess.events.common.EventEnvelope;
import com.chess.game.service.GameService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chess.common.messaging.ProcessedEventStore;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.JetStream;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.PushSubscribeOptions;
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
public class MatchFoundListener {

    private final Connection natsConnection;
    private final JetStream jetStream;
    private final ObjectMapper objectMapper;
    private final GameService gameService;
    private final ProcessedEventStore processedEventStore;

    private Dispatcher dispatcher;
    private JetStreamSubscription subscription;

    private static final int MAX_DELIVER = 5;
    private static final String CONSUMER = "game-service-matchmaking-match-found";

    @PostConstruct
    public void init() {
        try {
            ConsumerConfiguration config = ConsumerConfiguration.builder()
                    .durable(CONSUMER)
                    .ackPolicy(AckPolicy.Explicit)
                    .ackWait(Duration.ofSeconds(30))
                    .maxDeliver(MAX_DELIVER)
                    .backoff(
                            Duration.ofSeconds(1),
                            Duration.ofSeconds(5),
                            Duration.ofSeconds(15),
                            Duration.ofSeconds(30),
                            Duration.ofSeconds(60)
                    )
                    .deliverPolicy(DeliverPolicy.All)
                    .replayPolicy(ReplayPolicy.Instant)
                    .build();

            PushSubscribeOptions opts = PushSubscribeOptions.builder()
                    .configuration(config)
                    .build();

            dispatcher = natsConnection.createDispatcher();
            subscription = jetStream.subscribe(
                    NatsSubjects.MATCHMAKING_MATCH_FOUND,
                    dispatcher,
                    this::handleMessage,
                    false,
                    opts
            );

            log.info("JetStream subscribed to {}", NatsSubjects.MATCHMAKING_MATCH_FOUND);
        } catch (Exception e) {
            log.error("Failed to initialize JetStream subscription to {}", NatsSubjects.MATCHMAKING_MATCH_FOUND, e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (subscription != null) {
            try {
                subscription.unsubscribe();
            } catch (Exception ignored) {
            }
        }
    }

    private void handleMessage(Message message) {
        String json = new String(message.getData(), StandardCharsets.UTF_8);
        try {
            EventEnvelope<MatchFoundEvent> envelope = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, MatchFoundEvent.class));
            MatchFoundEvent event = envelope.getPayload();
            if (event == null) {
                safeAck(message);
                return;
            }
            if (envelope.getEventId() != null && processedEventStore.isProcessed(CONSUMER, envelope.getEventId())) {
                safeAck(message);
                return;
            }
            gameService.onMatchFound(event);
            if (envelope.getEventId() != null) {
                processedEventStore.markProcessed(CONSUMER, envelope.getEventId());
            }
            message.ack();
        } catch (Exception e) {
            int delivered = 1;
            if (message.metaData() != null) {
                try {
                    delivered = Math.toIntExact(message.metaData().deliveredCount());
                } catch (ArithmeticException ignored) {
                    delivered = Integer.MAX_VALUE;
                }
            }
            log.warn("Failed to process MatchFound (deliveredCount={}): {}", delivered, e.getMessage(), e);

            if (delivered >= MAX_DELIVER) {
                try {
                    String dlqSubject = "domain.dlq.MatchFound";
                    jetStream.publish(dlqSubject, message.getData());
                    log.error("Sent MatchFound to DLQ subject={}", dlqSubject);
                } catch (Exception dlqEx) {
                    log.error("Failed to publish MatchFound to DLQ, ACKing to stop poison loop", dlqEx);
                } finally {
                    safeAck(message);
                }
            } else {
                try {
                    message.nak();
                } catch (Exception nakEx) {
                    log.warn("Failed to NAK message, ACKing to avoid stuck redelivery", nakEx);
                    safeAck(message);
                }
            }
        }
    }

    private void safeAck(Message message) {
        try {
            message.ack();
        } catch (Exception ignored) {
        }
    }
}

