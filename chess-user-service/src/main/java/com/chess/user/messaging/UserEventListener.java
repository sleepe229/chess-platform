package com.chess.user.messaging;

import com.chess.events.auth.UserRegisteredEvent;
import com.chess.events.common.EventEnvelope;
import com.chess.events.constants.NatsSubjects;
import com.chess.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chess.common.messaging.ProcessedEventStore;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamSubscription;
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
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "nats.enabled", havingValue = "true", matchIfMissing = true)
public class UserEventListener {

    private final Connection natsConnection;
    private final JetStream jetStream;
    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final ProcessedEventStore processedEventStore;

    private Dispatcher dispatcher;
    private JetStreamSubscription subscription;

    private static final int MAX_DELIVER = 5;
    private static final String CONSUMER = "user-service-auth-user-registered";

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
                    NatsSubjects.AUTH_USER_REGISTERED,
                    dispatcher,
                    this::handleMessage,
                    false,
                    opts
            );
            log.info("JetStream subscribed to {}", NatsSubjects.AUTH_USER_REGISTERED);
        } catch (Exception e) {
            // Service should still be able to start even if NATS/JetStream is temporarily unavailable
            log.error("Failed to initialize JetStream subscription to {}", NatsSubjects.AUTH_USER_REGISTERED, e);
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
        if (dispatcher != null) {
            try {
                dispatcher.unsubscribe(NatsSubjects.AUTH_USER_REGISTERED);
            } catch (Exception ignored) {
            }
        }
    }

    private void handleMessage(Message message) {
        String json = new String(message.getData(), StandardCharsets.UTF_8);
        EventEnvelope<UserRegisteredEvent> envelope;
        try {
            envelope = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, UserRegisteredEvent.class));
        } catch (Exception e) {
            log.error("Failed to deserialize UserRegistered event, skipping", e);
            safeAck(message);
            return;
        }

        UserRegisteredEvent event = envelope.getPayload();
        if (event == null) {
            log.error("UserRegistered envelope has null payload, skipping");
            safeAck(message);
            return;
        }

        if (envelope.getEventId() != null && processedEventStore.isProcessed(CONSUMER, envelope.getEventId())) {
            safeAck(message);
            return;
        }

        log.info("Received UserRegistered event: userId={}, email={}",
                event.getUserId(), event.getEmail());

        try {
            UUID userId = UUID.fromString(event.getUserId());
            userService.createUser(userId, event.getEmail());
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
            log.warn("Failed to process UserRegistered (deliveredCount={}): {}", delivered, e.getMessage(), e);

            if (delivered >= MAX_DELIVER) {
                try {
                    String dlqSubject = "domain.dlq." + (envelope.getEventType() != null ? envelope.getEventType() : "UserRegistered");
                    jetStream.publish(dlqSubject, message.getData());
                    log.error("Sent to DLQ subject={} eventId={}", dlqSubject, envelope.getEventId());
                } catch (Exception dlqEx) {
                    log.error("Failed to publish to DLQ, will ACK to stop poison loop", dlqEx);
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
