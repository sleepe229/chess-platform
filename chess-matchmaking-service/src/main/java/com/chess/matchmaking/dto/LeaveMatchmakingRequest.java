package com.chess.matchmaking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LeaveMatchmakingRequest {
    @NotBlank
    private String requestId;
}

