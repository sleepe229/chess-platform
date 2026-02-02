package com.chess.game.state;

import com.chess.game.domain.GameStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameState {
    private UUID gameId;
    private UUID whiteId;
    private UUID blackId;

    private String fen;
    private GameClocks clocks;
    private GameTimeControl timeControl;
    private boolean rated;

    private GameStatus status;

    @Builder.Default
    private List<GameMove> moves = new ArrayList<>();

    private String result;
    private String finishReason;
    private UUID winnerId;

    private UUID drawOfferedBy;

    private Instant startedAt;
    private Instant finishedAt;
}

