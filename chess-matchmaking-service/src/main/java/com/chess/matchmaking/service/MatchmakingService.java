package com.chess.matchmaking.service;

import com.chess.common.exception.ForbiddenException;
import com.chess.common.exception.NotFoundException;
import com.chess.matchmaking.client.UserRatingsClient;
import com.chess.matchmaking.config.MatchmakingProperties;
import com.chess.matchmaking.domain.TimeControlClassifier;
import com.chess.matchmaking.domain.TimeControlType;
import com.chess.matchmaking.dto.MatchFoundDto;
import com.chess.matchmaking.dto.MatchmakingStatus;
import com.chess.matchmaking.dto.MatchmakingStatusResponse;
import com.chess.matchmaking.messaging.MatchmakingEventPublisher;
import com.chess.matchmaking.repo.MatchmakingAuditRepository;
import com.chess.matchmaking.repo.MatchmakingRequestStore;
import com.chess.matchmaking.repo.RedisMatchmakingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchmakingService {

    private final MatchmakingRequestStore requestStore;
    private final TimeControlClassifier timeControlClassifier;
    private final UserRatingsClient userRatingsClient;
    private final RedisMatchmakingEngine matchmakingEngine;
    private final MatchmakingProperties properties;
    private final MatchmakingEventPublisher eventPublisher;
    private final ObjectProvider<MatchmakingAuditRepository> auditRepositoryProvider;

    public String join(UUID userId, int baseSeconds, int incrementSeconds, boolean rated, String idempotencyKey, String requestIdHeader) {
        String requestId = requestStore.createOrGetActiveRequest(userId, baseSeconds, incrementSeconds, rated, idempotencyKey, requestIdHeader);

        MatchmakingRequestStore.StoredRequest stored = requestStore.getRequest(requestId);
        if (stored == null) {
            throw new NotFoundException("Matchmaking request not found after creation");
        }

        String status = stored.status() != null ? stored.status() : MatchmakingStatus.QUEUED.name();
        if (!MatchmakingStatus.QUEUED.name().equals(status)) {
            return requestId;
        }

        TimeControlType timeControlType = timeControlClassifier.classify(baseSeconds, incrementSeconds);

        boolean needsEnrichment = stored.timeControlType() == null || stored.rating() == null || stored.ratingDeviation() == null;
        if (needsEnrichment) {
            UserRatingsClient.RatingInfo ratingInfo = userRatingsClient.fetchRating(userId, timeControlType.name());
            requestStore.enrichQueuedRequest(requestId, timeControlType.name(), ratingInfo.rating(), ratingInfo.ratingDeviation());
        }

        MatchmakingRequestStore.StoredRequest enriched = requestStore.getRequest(requestId);
        double rating = enriched != null && enriched.rating() != null ? Double.parseDouble(enriched.rating()) : 0.0;
        Double ratingDeviation = enriched != null && enriched.ratingDeviation() != null ? Double.valueOf(enriched.ratingDeviation()) : null;

        // Publish queued (idempotent via deterministic Nats-Msg-Id)
        eventPublisher.publishPlayerQueued(requestId, userId, timeControlType.name(), baseSeconds, incrementSeconds, rated);

        MatchmakingAuditRepository audit = auditRepositoryProvider.getIfAvailable();
        if (audit != null) {
            try {
                audit.upsertQueued(
                        UUID.fromString(requestId),
                        userId,
                        timeControlType.name(),
                        baseSeconds,
                        incrementSeconds,
                        rated,
                        rating,
                        ratingDeviation,
                        requestIdHeader,
                        idempotencyKey
                );
            } catch (Exception ignored) {
                // audit is best-effort; matchmaking must stay available
            }
        }

        RedisMatchmakingEngine.MatchPair pair = matchmakingEngine.enqueueAndTryMatch(
                timeControlType.name(),
                requestId,
                rating,
                properties.getInitialRatingRange(),
                properties.getRatingRangeIncrement(),
                properties.getMaxRatingRange(),
                properties.getRangeExpansionIntervalSeconds() * 1000L
        );

        if (pair != null) {
            String gameId = UUID.randomUUID().toString();
            requestStore.markMatched(pair.requestId1(), gameId);
            requestStore.markMatched(pair.requestId2(), gameId);

            MatchmakingRequestStore.StoredRequest r1 = requestStore.getRequest(pair.requestId1());
            MatchmakingRequestStore.StoredRequest r2 = requestStore.getRequest(pair.requestId2());
            if (r1 != null && r2 != null) {
                boolean whiteFirst = (System.nanoTime() & 1) == 0;
                String white = whiteFirst ? r1.userId() : r2.userId();
                String black = whiteFirst ? r2.userId() : r1.userId();

                // Dequeued due to match
                try {
                    eventPublisher.publishPlayerDequeued(pair.requestId1(), UUID.fromString(r1.userId()), "MATCHED");
                    eventPublisher.publishPlayerDequeued(pair.requestId2(), UUID.fromString(r2.userId()), "MATCHED");
                } catch (Exception ignored) {
                    // best-effort
                }

                eventPublisher.publishMatchFound(MatchFoundDto.builder()
                        .matchId(gameId)
                        .whitePlayerId(white)
                        .blackPlayerId(black)
                        .timeControl(timeControlType.name())
                        .initialTimeSeconds(baseSeconds)
                        .incrementSeconds(incrementSeconds)
                        .rated(rated)
                        .build());
            }

            if (audit != null) {
                try {
                    UUID gameUuid = UUID.fromString(gameId);
                    audit.markMatched(UUID.fromString(pair.requestId1()), gameUuid);
                    audit.markMatched(UUID.fromString(pair.requestId2()), gameUuid);
                } catch (Exception ignored) {
                }
            }
        }

        return requestId;
    }

    public void leave(UUID userId, String requestId, String idempotencyKey, String requestIdHeader) {
        MatchmakingRequestStore.StoredRequest req = requestStore.getRequest(requestId);
        if (req == null) {
            throw new NotFoundException("Matchmaking request not found");
        }
        if (!userId.toString().equals(req.userId())) {
            throw new ForbiddenException("Request does not belong to user");
        }
        if (req.timeControlType() != null && !req.timeControlType().isBlank()) {
            matchmakingEngine.removeFromQueues(req.timeControlType(), requestId);
        }
        requestStore.cancelRequest(userId, requestId, idempotencyKey, requestIdHeader);

        // Dequeued due to user cancellation (best-effort)
        try {
            eventPublisher.publishPlayerDequeued(requestId, userId, "CANCELLED");
        } catch (Exception ignored) {
        }

        MatchmakingAuditRepository audit = auditRepositoryProvider.getIfAvailable();
        if (audit != null) {
            try {
                audit.markCancelled(UUID.fromString(requestId), "cancelled");
            } catch (Exception ignored) {
            }
        }
    }

    public MatchmakingStatusResponse status(UUID userId, String requestId) {
        MatchmakingRequestStore.StoredRequest req = requestStore.getRequest(requestId);
        if (req == null) {
            throw new NotFoundException("Matchmaking request not found");
        }
        if (!userId.toString().equals(req.userId())) {
            throw new ForbiddenException("Request does not belong to user");
        }
        String status = req.status() != null ? req.status() : MatchmakingStatus.QUEUED.name();
        return new MatchmakingStatusResponse(requestId, status, req.gameId());
    }
}
