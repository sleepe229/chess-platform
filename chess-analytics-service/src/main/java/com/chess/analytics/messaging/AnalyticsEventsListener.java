package com.chess.analytics.messaging;

import com.chess.common.messaging.ProcessedEventStore;
import com.chess.events.common.EventEnvelope;
import com.chess.events.constants.NatsSubjects;
import com.chess.events.analytics.AnalysisCompletedEvent;
import com.chess.events.analytics.AnalysisFailedEvent;
import com.chess.analytics.service.AnalysisJobService;
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
public class AnalyticsEventsListener {

    private static final String CONSUMER = "analytics-service-domain-analytics";
    private static final String DURABLE_COMPLETED = "analytics-job-completed-v1";
    private static final String DURABLE_FAILED = "analytics-job-failed-v1";

    private final Connection natsConnection;
    private final JetStream jetStream;
    private final ObjectMapper objectMapper;
    private final ProcessedEventStore processedEventStore;
    private final AnalysisJobService analysisJobService;

    private Dispatcher dispatcher;
    private JetStreamSubscription subCompleted;
    private JetStreamSubscription subFailed;

    @PostConstruct
    public void init() {
        try {
            dispatcher = natsConnection.createDispatcher();
            subCompleted = jetStream.subscribe(NatsSubjects.ANALYTICS_COMPLETED, dispatcher, this::onCompleted, false,
                    PushSubscribeOptions.builder().configuration(consumerConfig(DURABLE_COMPLETED)).build());
            subFailed = jetStream.subscribe(NatsSubjects.ANALYTICS_FAILED, dispatcher, this::onFailed, false,
                    PushSubscribeOptions.builder().configuration(consumerConfig(DURABLE_FAILED)).build());
            log.info("Subscribed to analytics events: {}, {}", NatsSubjects.ANALYTICS_COMPLETED, NatsSubjects.ANALYTICS_FAILED);
        } catch (Exception e) {
            log.error("Failed to subscribe to analytics events", e);
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
        tryUnsub(subCompleted);
        tryUnsub(subFailed);
    }

    private void onCompleted(Message msg) {
        try {
            EventEnvelope<AnalysisCompletedEvent> env = objectMapper.readValue(new String(msg.getData(), StandardCharsets.UTF_8),
                    objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, AnalysisCompletedEvent.class));
            if (env.getEventId() != null && processedEventStore.isProcessed(CONSUMER, env.getEventId())) {
                msg.ack();
                return;
            }
            if (env.getPayload() != null) {
                analysisJobService.onAnalysisCompleted(env.getPayload());
            }
            if (env.getEventId() != null) processedEventStore.markProcessed(CONSUMER, env.getEventId());
            msg.ack();
        } catch (Exception ex) {
            log.warn("Failed to handle AnalysisCompleted", ex);
            safeNak(msg);
        }
    }

    private void onFailed(Message msg) {
        try {
            EventEnvelope<AnalysisFailedEvent> env = objectMapper.readValue(new String(msg.getData(), StandardCharsets.UTF_8),
                    objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, AnalysisFailedEvent.class));
            if (env.getEventId() != null && processedEventStore.isProcessed(CONSUMER, env.getEventId())) {
                msg.ack();
                return;
            }
            if (env.getPayload() != null) {
                analysisJobService.onAnalysisFailed(env.getPayload());
            }
            if (env.getEventId() != null) processedEventStore.markProcessed(CONSUMER, env.getEventId());
            msg.ack();
        } catch (Exception ex) {
            log.warn("Failed to handle AnalysisFailed", ex);
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
