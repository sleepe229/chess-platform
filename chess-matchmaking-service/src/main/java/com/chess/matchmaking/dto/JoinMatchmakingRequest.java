package com.chess.matchmaking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JoinMatchmakingRequest {

    @NotNull
    @Min(10)
    @Max(7200)
    private Integer baseSeconds;

    @NotNull
    @Min(0)
    @Max(60)
    private Integer incrementSeconds;

    @NotNull
    private Boolean rated;
}

