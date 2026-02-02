package com.chess.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameClocksMessage {
    private long whiteMs;
    private long blackMs;
}

