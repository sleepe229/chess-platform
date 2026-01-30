package com.chess.matchmaking.repo;

import com.chess.matchmaking.repo.entity.MatchmakingRequestAudit;
import com.chess.matchmaking.repo.jpa.MatchmakingRequestAuditJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "matchmaking.audit.enabled", havingValue = "true")
public class JpaMatchmakingAuditRepository implements MatchmakingAuditRepository {

    private final MatchmakingRequestAuditJpaRepository jpaRepository;

    @Override
    @Transactional
    public void upsertQueued(UUID requestId, UUID userId, String timeControlType, int baseSeconds, int incrementSeconds,
                             boolean rated, Double rating, Double ratingDeviation, String xRequestId, String idempotencyKey) {
        Instant now = Instant.now();

        MatchmakingRequestAudit entity = jpaRepository.findById(requestId).orElseGet(MatchmakingRequestAudit::new);
        if (entity.getRequestId() == null) {
            entity.setRequestId(requestId);
            entity.setCreatedAt(now);
        }

        entity.setUserId(userId);
        entity.setTimeControlType(timeControlType);
        entity.setBaseSeconds(baseSeconds);
        entity.setIncrementSeconds(incrementSeconds);
        entity.setRated(rated);
        entity.setRating(rating);
        entity.setRatingDeviation(ratingDeviation);
        entity.setStatus("QUEUED");
        entity.setMatchedGameId(null);
        entity.setCancelReason(null);
        entity.setXRequestId(xRequestId);
        entity.setIdempotencyKey(idempotencyKey);
        entity.setUpdatedAt(now);

        jpaRepository.save(entity);
    }

    @Override
    @Transactional
    public void markMatched(UUID requestId, UUID gameId) {
        Instant now = Instant.now();

        MatchmakingRequestAudit entity = jpaRepository.findById(requestId).orElseGet(MatchmakingRequestAudit::new);
        if (entity.getRequestId() == null) {
            entity.setRequestId(requestId);
            entity.setCreatedAt(now);
        }

        entity.setStatus("MATCHED");
        entity.setMatchedGameId(gameId);
        entity.setCancelReason(null);
        entity.setUpdatedAt(now);

        jpaRepository.save(entity);
    }

    @Override
    @Transactional
    public void markCancelled(UUID requestId, String reason) {
        Instant now = Instant.now();

        MatchmakingRequestAudit entity = jpaRepository.findById(requestId).orElseGet(MatchmakingRequestAudit::new);
        if (entity.getRequestId() == null) {
            entity.setRequestId(requestId);
            entity.setCreatedAt(now);
        }

        entity.setStatus("CANCELLED");
        entity.setCancelReason(reason);
        entity.setUpdatedAt(now);

        jpaRepository.save(entity);
    }

    @Override
    @Transactional
    public void markExpired(UUID requestId) {
        Instant now = Instant.now();

        MatchmakingRequestAudit entity = jpaRepository.findById(requestId).orElseGet(MatchmakingRequestAudit::new);
        if (entity.getRequestId() == null) {
            entity.setRequestId(requestId);
            entity.setCreatedAt(now);
        }

        entity.setStatus("EXPIRED");
        entity.setCancelReason("timeout");
        entity.setUpdatedAt(now);

        jpaRepository.save(entity);
    }
}

