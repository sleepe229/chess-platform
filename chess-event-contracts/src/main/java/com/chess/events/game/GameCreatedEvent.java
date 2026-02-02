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
public class GameCreatedEvent {

    @NotBlank
    private String gameId;

    @NotBlank
    private String whitePlayerId;

    @NotBlank
    private String blackPlayerId;

    @NotBlank
    private String timeControl;

    @NotNull
    private Integer initialTimeSeconds;

    @NotNull
    private Integer incrementSeconds;
}
