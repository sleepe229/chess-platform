package com.chess.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameMoveMessage {
    private int ply;
    private String uci;
    private String san;
    private String fenAfter;
    private Instant playedAt;
    private UUID byUserId;
}

