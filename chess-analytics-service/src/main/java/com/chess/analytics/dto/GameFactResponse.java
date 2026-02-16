package com.chess.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class GameFactResponse {
    private UUID gameId;
    private UUID whitePlayerId;
    private UUID blackPlayerId;
    private String result;
    private String finishReason;
    private UUID winnerId;
    private Instant finishedAt;
    private String pgn;
    private Boolean rated;
    private String timeControlType;
    private Integer moveCount;
}
