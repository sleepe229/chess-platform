package com.chess.matchmaking.service;

import com.chess.matchmaking.config.MatchmakingProperties;
import com.chess.matchmaking.domain.TimeControlType;
import com.chess.matchmaking.dto.MatchmakingStatus;
import com.chess.matchmaking.repo.MatchmakingAuditRepository;
import com.chess.matchmaking.repo.MatchmakingRequestStore;
import com.chess.matchmaking.repo.RedisMatchmakingEngine;
import com.chess.matchmaking.messaging.MatchmakingEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchmakingExpiryJob {

    private final RedisMatchmakingEngine matchmakingEngine;
    private final MatchmakingRequestStore requestStore;
    private final MatchmakingProperties properties;
    private final MatchmakingEventPublisher eventPublisher;
    private final ObjectProvider<MatchmakingAuditRepository> auditRepositoryProvider;

    @Scheduled(fixedDelayString = "${matchmaking.expire-scan-interval-ms:5000}")
    public void expireQueued() {
        long expireBeforeMs = Instant.now().minusSeconds(properties.getQueueTimeoutSeconds()).toEpochMilli();

        for (TimeControlType tct : TimeControlType.values()) {
            List<String> expired = matchmakingEngine.findExpired(tct.name(), expireBeforeMs, 200);
            if (expired.isEmpty()) {
                continue;
            }
            for (String requestId : expired) {
                MatchmakingRequestStore.StoredRequest req = requestStore.getRequest(requestId);
                if (req == null) {
                    // orphan in ZSET; just cleanup
                    matchmakingEngine.removeFromQueues(tct.name(), requestId);
                    continue;
                }

                String status = req.status() != null ? req.status() : MatchmakingStatus.QUEUED.name();
                if (!MatchmakingStatus.QUEUED.name().equals(status)) {
                    matchmakingEngine.removeFromQueues(tct.name(), requestId);
                    continue;
                }

                matchmakingEngine.removeFromQueues(tct.name(), requestId);
                requestStore.markExpired(requestId);

                try {
                    eventPublisher.publishPlayerDequeued(requestId, UUID.fromString(req.userId()), "EXPIRED");
                } catch (Exception ignored) {
                }

                MatchmakingAuditRepository audit = auditRepositoryProvider.getIfAvailable();
                if (audit != null) {
                    try {
                        audit.markExpired(UUID.fromString(requestId));
                    } catch (Exception e) {
                        log.warn("Failed to write expiry audit for requestId={}", requestId, e);
                    }
                }
            }
        }
    }
}

