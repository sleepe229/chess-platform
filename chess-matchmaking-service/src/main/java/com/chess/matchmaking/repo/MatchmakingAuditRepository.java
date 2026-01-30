package com.chess.matchmaking.repo;

import java.util.UUID;

public interface MatchmakingAuditRepository {

    void upsertQueued(
            UUID requestId,
            UUID userId,
            String timeControlType,
            int baseSeconds,
            int incrementSeconds,
            boolean rated,
            Double rating,
            Double ratingDeviation,
            String xRequestId,
            String idempotencyKey
    );

    void markMatched(UUID requestId, UUID gameId);

    void markCancelled(UUID requestId, String reason);

    void markExpired(UUID requestId);
}

