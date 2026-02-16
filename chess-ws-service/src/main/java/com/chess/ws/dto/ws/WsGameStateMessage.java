package com.chess.ws.dto.ws;

import com.chess.ws.dto.GameClocksMessage;
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
public class WsGameStateMessage {
    @Builder.Default
    private String type = "GAME_STATE";

    private UUID gameId;
    private UUID whiteId;
    private UUID blackId;
    private String fen;
    private List<WsMove> moves;
    private GameClocksMessage clocks;
    private String status;
    private String sideToMove;
    private UUID drawOfferedBy;
}

