package com.chess.ws.dto.ws;

import com.chess.ws.dto.GameClocksMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsMoveAcceptedMessage {
    @Builder.Default
    private String type = "MOVE_ACCEPTED";

    private UUID gameId;
    private String clientMoveId;
    private Integer ply;
    private String fen;
    private GameClocksMessage clocks;
}

