package com.chess.game.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameTimeControl {
    private String type;
    private int baseSeconds;
    private int incrementSeconds;
}

