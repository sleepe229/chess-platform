package com.chess.events.game;

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
public class GameFinishedEvent {

    @NotBlank
    private String gameId;

    @NotBlank
    private String whitePlayerId;

    @NotBlank
    private String blackPlayerId;

    @NotBlank
    private String result;

    @NotBlank
    private String finishReason;

    private String winnerId;

    @NotBlank
    private String finishedAt;

    @NotBlank
    private String pgn;

    @NotNull
    private Boolean rated;

    @NotBlank
    private String timeControlType;
}
