package com.chess.game.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameMove {
    private int ply;
    private String uci;
    private String san;
    private String fenAfter;
    private Instant playedAt;
    private UUID byUserId;
}

