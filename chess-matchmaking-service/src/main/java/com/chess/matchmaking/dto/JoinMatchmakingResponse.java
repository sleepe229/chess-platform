package com.chess.matchmaking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JoinMatchmakingResponse {
    private String requestId;
    private String status; // QUEUED
}

