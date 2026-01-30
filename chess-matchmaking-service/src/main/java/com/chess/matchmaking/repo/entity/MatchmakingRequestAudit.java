package com.chess.matchmaking.repo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "matchmaking_request_audit")
public class MatchmakingRequestAudit {

    @Id
    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "time_control_type", nullable = false, length = 16)
    private String timeControlType;

    @Column(name = "base_seconds", nullable = false)
    private int baseSeconds;

    @Column(name = "increment_seconds", nullable = false)
    private int incrementSeconds;

    @Column(name = "rated", nullable = false)
    private boolean rated;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "rating_deviation")
    private Double ratingDeviation;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "matched_game_id")
    private UUID matchedGameId;

    @Column(name = "cancel_reason", length = 32)
    private String cancelReason;

    @Column(name = "x_request_id", length = 64)
    private String xRequestId;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

