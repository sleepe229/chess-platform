package com.chess.analytics.messaging;

import com.chess.analytics.engine.AnalysisRunner;
import com.chess.analytics.repo.GameFactRepository;
import com.chess.common.messaging.ProcessedEventStore;
import com.chess.events.analytics.AnalysisRequestedEvent;
import com.chess.events.common.EventEnvelope;
import com.chess.events.constants.NatsSubjects;
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
import java.util.Optional;
import java.util.UUID;

/**
 * Subscribes to AnalysisRequested, runs Stockfish on the game PGN, and publishes
 * AnalysisCompleted or AnalysisFailed. Enable with analysis.engine.enabled=true
 * and ensure Stockfish is on PATH or set analysis.engine.path.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "analysis.engine.enabled", havingValue = "true", matchIfMissing = false)
public class AnalysisRequestedListener {

    private static final String CONSUMER = "analytics-service-analysis-requested";
    private static final String DURABLE = "analytics-requested-v1";

    private final Connection natsConnection;
    private final JetStream jetStream;
    private final ObjectMapper objectMapper;
    private final ProcessedEventStore processedEventStore;
    private final GameFactRepository gameFactRepository;
    private final AnalysisRunner analysisRunner;
    private final AnalyticsEventPublisher analyticsEventPublisher;

    private Dispatcher dispatcher;
    private JetStreamSubscription subscription;

    @PostConstruct
    public void init() {
        try {
            dispatcher = natsConnection.createDispatcher();
            subscription = jetStream.subscribe(NatsSubjects.ANALYTICS_REQUESTED, dispatcher, this::onMessage, false,
                    PushSubscribeOptions.builder().configuration(consumerConfig()).build());
            log.info("Subscribed to {} (engine analysis enabled)", NatsSubjects.ANALYTICS_REQUESTED);
        } catch (Exception e) {
            log.error("Failed to subscribe to AnalysisRequested", e);
        }
    }

    private ConsumerConfiguration consumerConfig() {
        return ConsumerConfiguration.builder()
                .durable(DURABLE)
                .ackPolicy(AckPolicy.Explicit)
                .ackWait(Duration.ofMinutes(5))
                .maxDeliver(3)
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

    private void onMessage(Message msg) {
        try {
            EventEnvelope<AnalysisRequestedEvent> envelope = objectMapper.readValue(
                    new String(msg.getData(), StandardCharsets.UTF_8),
                    objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, AnalysisRequestedEvent.class));
            AnalysisRequestedEvent payload = envelope.getPayload();
            if (payload == null) {
                msg.ack();
                return;
            }
            if (envelope.getEventId() != null && processedEventStore.isProcessed(CONSUMER, envelope.getEventId())) {
                msg.ack();
                return;
            }

            String jobId = payload.getAnalysisJobId();
            String gameIdStr = payload.getGameId();
            UUID gameId = UUID.fromString(gameIdStr);

            Optional<String> pgnOpt = gameFactRepository.findById(gameId).map(g -> g.getPgn());
            if (pgnOpt.isEmpty() || pgnOpt.get() == null || pgnOpt.get().isBlank()) {
                analyticsEventPublisher.publishAnalysisFailed(jobId, gameIdStr, "Game or PGN not found");
                if (envelope.getEventId() != null) processedEventStore.markProcessed(CONSUMER, envelope.getEventId());
                msg.ack();
                return;
            }

            AnalysisRunner.AnalysisResult result = analysisRunner.run(pgnOpt.get());
            analyticsEventPublisher.publishAnalysisCompleted(
                    jobId, gameIdStr,
                    result.totalMoves(), result.accuracyWhite(), result.accuracyBlack());

            if (envelope.getEventId() != null) processedEventStore.markProcessed(CONSUMER, envelope.getEventId());
            msg.ack();
        } catch (Exception e) {
            log.warn("Analysis job failed", e);
            try {
                EventEnvelope<AnalysisRequestedEvent> envelope = objectMapper.readValue(
                        new String(msg.getData(), StandardCharsets.UTF_8),
                        objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, AnalysisRequestedEvent.class));
                if (envelope.getPayload() != null) {
                    analyticsEventPublisher.publishAnalysisFailed(
                            envelope.getPayload().getAnalysisJobId(),
                            envelope.getPayload().getGameId(),
                            e.getMessage());
                }
            } catch (Exception ex) {
                log.debug("Could not publish AnalysisFailed", ex);
            }
            try {
                msg.nak();
            } catch (Exception ignored) {
            }
        }
    }
}
