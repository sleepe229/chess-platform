package com.chess.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AnalysisJobResponse {
    private UUID analysisJobId;
    private UUID gameId;
    private UUID requestedBy;
    private String status;
    private Integer totalMoves;
    private Integer accuracyWhite;
    private Integer accuracyBlack;
    private String errorMessage;
    private Instant completedAt;
    private Instant createdAt;
}
