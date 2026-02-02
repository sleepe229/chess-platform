package com.chess.game.util;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;

public final class ChessRules {

    private ChessRules() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Move parseUci(String uci, Side sideToMove) {
        return new Move(uci, sideToMove);
    }

    public static boolean applyMove(Board board, Move move) {
        // The second flag controls "full validation / add SAN" in chesslib
        // (empirically: doMove(move, true) computes SAN for the move)
        return board.doMove(move, true);
    }
}

