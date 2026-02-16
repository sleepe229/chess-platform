package com.chess.analytics.engine;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Converts PGN text to a list of (fen, uciMove, isWhite) for engine comparison. */
@Slf4j
public final class PgnReplay {

    private static final Pattern MOVE_NUMBER = Pattern.compile("^\\d+\\.$");
    private static final Pattern RESULT = Pattern.compile("^(1-0|0-1|1/2-1/2|\\*)$");

    private PgnReplay() {}

    public static List<PositionAndMove> pgnToPositionAndMoves(String pgn) {
        if (pgn == null || pgn.isBlank()) return List.of();
        List<String> sans = extractSanMoves(pgn);
        if (sans.isEmpty()) return List.of();

        List<PositionAndMove> out = new ArrayList<>();
        Board board = new Board();
        boolean whiteTurn = true;

        for (String san : sans) {
            String fen = board.getFen();
            Move move = findLegalMove(board, san);
            if (move == null) {
                log.warn("Could not find legal move for SAN: {} in PGN", san);
                break;
            }
            String uci = toUci(move);
            out.add(new PositionAndMove(fen, uci, whiteTurn));
            board.doMove(move, true);
            whiteTurn = !whiteTurn;
        }
        return out;
    }

    private static List<String> extractSanMoves(String pgn) {
        String body = pgn;
        int idx = body.indexOf("\n\n");
        if (idx >= 0) body = body.substring(idx + 2).trim();
        // Remove result at end
        body = body.replaceAll("\\s*(1-0|0-1|1/2-1/2|\\*)\\s*$", "").trim();
        if (body.isEmpty()) return List.of();

        List<String> moves = new ArrayList<>();
        for (String token : body.split("\\s+")) {
            if (MOVE_NUMBER.matcher(token).matches() || RESULT.matcher(token).matches()) continue;
            if (token.contains(".")) {
                String after = token.replaceFirst("^\\d+\\.", "").trim();
                if (!after.isEmpty()) moves.add(after);
            } else {
                moves.add(token);
            }
        }
        return moves;
    }

    private static String toUci(Move m) {
        String from = m.getFrom().name().toLowerCase();
        String to = m.getTo().name().toLowerCase();
        String promo = m.getPromotion() != null ? m.getPromotion().getFenSymbol().toLowerCase() : "";
        return from + to + promo;
    }

    private static Move findLegalMove(Board board, String san) {
        for (Move m : board.legalMoves()) {
            if (!board.doMove(m, true)) continue;
            String appliedSan = m.getSan();
            board.undoMove();
            if (appliedSan != null && appliedSan.equals(san)) return m;
        }
        return null;
    }

    public record PositionAndMove(String fen, String uciMove, boolean isWhite) {}
}
