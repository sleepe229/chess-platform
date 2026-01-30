package com.chess.matchmaking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MatchmakingStatusResponse {
    private String requestId;
    private String status; // QUEUED|MATCHED|CANCELLED|EXPIRED
    private String gameId; // nullable
}

