package com.chess.game.service;

import com.chess.game.state.GameMove;
import com.chess.game.state.GameState;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
public final class PgnBuilder {

    private PgnBuilder() {
    }

    public static String buildPgn(GameState state) {
        try {
            String result = state.getResult() != null ? state.getResult() : "*";
            String date = LocalDate.now(ZoneOffset.UTC).toString().replace("-", ".");
            String timeControl = state.getTimeControl() != null
                    ? state.getTimeControl().getBaseSeconds() + "+" + state.getTimeControl().getIncrementSeconds()
                    : "";

            StringBuilder pgn = new StringBuilder();
            pgn.append("[Event \"Chess Online\"]\n");
            pgn.append("[Site \"?\"]\n");
            pgn.append("[Date \"").append(date).append("\"]\n");
            pgn.append("[White \"").append(state.getWhiteId()).append("\"]\n");
            pgn.append("[Black \"").append(state.getBlackId()).append("\"]\n");
            pgn.append("[Result \"").append(result).append("\"]\n");
            if (!timeControl.isBlank()) {
                pgn.append("[TimeControl \"").append(timeControl).append("\"]\n");
            }
            pgn.append("\n");

            List<GameMove> moves = state.getMoves() != null ? state.getMoves() : List.of();
            StringBuilder sb = new StringBuilder();
            int ply = 1;
            for (GameMove m : moves) {
                if (ply % 2 == 1) {
                    sb.append(((ply + 1) / 2)).append(". ");
                }
                sb.append(m.getSan() != null ? m.getSan() : m.getUci()).append(' ');
                ply++;
            }
            pgn.append(sb.toString().trim());
            if (!pgn.toString().endsWith(" ")) {
                pgn.append(' ');
            }
            pgn.append(result);
            return pgn.toString().trim();
        } catch (Exception e) {
            log.warn("Failed to build PGN for gameId={}, falling back to UCI", state.getGameId(), e);
            StringBuilder sb = new StringBuilder();
            for (GameMove m : (state.getMoves() != null ? state.getMoves() : List.<GameMove>of())) {
                sb.append(m.getUci()).append(' ');
            }
            return sb.toString().trim();
        }
    }
}
