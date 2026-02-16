package com.chess.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameStateMessage {
    private UUID gameId;
    private UUID whiteId;
    private UUID blackId;
    private String fen;
    private List<GameMoveMessage> moves;
    private GameClocksMessage clocks;
    private String status;
    private String sideToMove;
    private String result;
    private String finishReason;
    private UUID drawOfferedBy;
}

