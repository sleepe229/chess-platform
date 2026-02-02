package com.chess.events.game;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeExpiredEvent {

    @NotBlank
    private String gameId;

    @NotBlank
    private String playerId;

    @NotBlank
    private String color;
}
