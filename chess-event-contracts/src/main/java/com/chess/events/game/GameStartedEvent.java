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
public class GameStartedEvent {

    @NotBlank
    private String gameId;

    @NotBlank
    private String startedAt;
}
