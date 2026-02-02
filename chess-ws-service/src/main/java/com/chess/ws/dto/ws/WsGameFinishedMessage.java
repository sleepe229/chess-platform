package com.chess.ws.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsGameFinishedMessage {
    @Builder.Default
    private String type = "GAME_FINISHED";

    private UUID gameId;
    private String result;
    private String reason;
}

