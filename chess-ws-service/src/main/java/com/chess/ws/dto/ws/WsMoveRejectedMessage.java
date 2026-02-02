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
public class WsMoveRejectedMessage {
    @Builder.Default
    private String type = "MOVE_REJECTED";

    private UUID gameId;
    private String clientMoveId;
    private String reason;
}

