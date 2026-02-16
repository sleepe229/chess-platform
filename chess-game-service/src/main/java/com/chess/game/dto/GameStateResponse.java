package com.chess.game.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameStateResponse {
    private UUID gameId;
    private UUID whiteId;
    private UUID blackId;
    private String fen;
    private List<GameMoveResponse> moves;
    private GameClocksResponse clocks;
    private String status;
    private String sideToMove;
    private String result;
    private String finishReason;
    private UUID drawOfferedBy;
}

