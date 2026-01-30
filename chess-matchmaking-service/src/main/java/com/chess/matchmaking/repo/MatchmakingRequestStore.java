package com.chess.matchmaking.repo;

import java.util.UUID;

public interface MatchmakingRequestStore {

    record StoredRequest(
            String requestId,
            String userId,
            String status,
            String gameId,
            String timeControlType,
            String baseSeconds,
            String incrementSeconds,
            String rated,
            String rating,
            String ratingDeviation,
            String queuedAtMs
    ) {
    }

    String createOrGetActiveRequest(
            UUID userId,
            int baseSeconds,
            int incrementSeconds,
            boolean rated,
            String idempotencyKey,
            String requestIdHeader
    );

    void enrichQueuedRequest(
            String requestId,
            String timeControlType,
            double rating,
            double ratingDeviation
    );

    void markMatched(String requestId, String gameId);

    void markExpired(String requestId);

    StoredRequest getRequest(String requestId);

    void cancelRequest(UUID userId, String requestId, String idempotencyKey, String requestIdHeader);
}

