package com.chess.analytics.messaging;

import com.chess.events.analytics.AnalysisRequestedEvent;
import com.chess.events.constants.NatsSubjects;
import com.chess.events.util.EventBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.JetStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "nats.enabled", havingValue = "true", matchIfMissing = true)
public class AnalyticsEventPublisher {

    private static final String PRODUCER = "analytics-service";

    private final JetStream jetStream;
    private final ObjectMapper objectMapper;

    public void publishAnalysisRequested(String analysisJobId, String gameId, String requestedBy) {
        if (jetStream == null) {
            log.warn("JetStream not available, AnalysisRequested event not published");
            return;
        }
        try {
            AnalysisRequestedEvent payload = AnalysisRequestedEvent.builder()
                    .analysisJobId(analysisJobId)
                    .gameId(gameId)
                    .requestedBy(requestedBy)
                    .build();
            var envelope = EventBuilder.envelope("AnalysisRequested", PRODUCER, payload);
            byte[] bytes = objectMapper.writeValueAsString(envelope).getBytes(StandardCharsets.UTF_8);
            jetStream.publish(NatsSubjects.ANALYTICS_REQUESTED, bytes);
            log.debug("Published AnalysisRequested: jobId={}, gameId={}", analysisJobId, gameId);
        } catch (Exception e) {
            log.error("Failed to publish AnalysisRequested", e);
            throw new RuntimeException("Failed to publish analysis request", e);
        }
    }
}
