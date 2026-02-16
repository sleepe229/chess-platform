package com.chess.analytics.messaging;

import com.chess.common.messaging.ProcessedEventStore;
import com.chess.events.common.EventEnvelope;
import com.chess.events.constants.NatsSubjects;
import com.chess.events.users.RatingUpdatedEvent;
import com.chess.analytics.service.PlayerRatingService;
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
public class UserEventsListener {

    private static final String CONSUMER = "analytics-service-domain-users";
    private static final String DURABLE = "analytics-user-rating-updated-v1";

    private final Connection natsConnection;
    private final JetStream jetStream;
    private final ObjectMapper objectMapper;
    private final ProcessedEventStore processedEventStore;
    private final PlayerRatingService playerRatingService;

    private Dispatcher dispatcher;
    private JetStreamSubscription subscription;

    @PostConstruct
    public void init() {
        try {
            dispatcher = natsConnection.createDispatcher();
            subscription = jetStream.subscribe(NatsSubjects.USER_RATING_UPDATED, dispatcher, this::onRatingUpdated, false,
                    PushSubscribeOptions.builder().configuration(consumerConfig()).build());
            log.info("Subscribed to user events: {}", NatsSubjects.USER_RATING_UPDATED);
        } catch (Exception e) {
            log.error("Failed to subscribe to user events", e);
        }
    }

    private ConsumerConfiguration consumerConfig() {
        return ConsumerConfiguration.builder()
                .durable(DURABLE)
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
        if (subscription != null) {
            try {
                subscription.unsubscribe();
            } catch (Exception ignored) {
            }
        }
    }

    private void onRatingUpdated(Message msg) {
        try {
            EventEnvelope<RatingUpdatedEvent> env = objectMapper.readValue(new String(msg.getData(), StandardCharsets.UTF_8),
                    objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, RatingUpdatedEvent.class));
            if (env.getEventId() != null && processedEventStore.isProcessed(CONSUMER, env.getEventId())) {
                msg.ack();
                return;
            }
            if (env.getPayload() != null) {
                playerRatingService.onRatingUpdated(env.getPayload());
            }
            if (env.getEventId() != null) processedEventStore.markProcessed(CONSUMER, env.getEventId());
            msg.ack();
        } catch (Exception ex) {
            log.warn("Failed to handle RatingUpdated", ex);
            try {
                msg.nak();
            } catch (Exception ignored) {
            }
        }
    }
}
