package com.chess.events.matchmaking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchFoundEvent {

    @NotBlank
    private String gameId;

    @NotBlank
    private String whitePlayerId;

    @NotBlank
    private String blackPlayerId;

    @NotBlank
    private String timeControlType;

    @NotNull
    private Integer baseSeconds;

    @NotNull
    private Integer incrementSeconds;

    private Boolean rated;
}
