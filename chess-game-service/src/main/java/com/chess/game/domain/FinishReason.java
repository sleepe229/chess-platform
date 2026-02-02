package com.chess.game.domain;

public enum FinishReason {
    CHECKMATE,
    RESIGN,
    TIMEOUT,
    DRAW_AGREEMENT,
    STALEMATE,
    INSUFFICIENT_MATERIAL,
    THREEFOLD_REPETITION,
    FIFTY_MOVE_RULE,
    ABORTED
}

