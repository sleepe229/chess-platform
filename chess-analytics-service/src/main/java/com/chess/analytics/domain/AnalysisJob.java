package com.chess.analytics.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analysis_jobs", schema = "chess_analytics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisJob {

    @Id
    @Column(name = "analysis_job_id")
    private UUID analysisJobId;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "total_moves")
    private Integer totalMoves;

    @Column(name = "accuracy_white")
    private Integer accuracyWhite;

    @Column(name = "accuracy_black")
    private Integer accuracyBlack;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
}
