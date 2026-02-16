package com.chess.analytics.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AnalysisRunner {

    @Value("${analysis.engine.path:stockfish}")
    private String enginePath;

    @Value("${analysis.engine.movetime-ms:400}")
    private long movetimeMs;

    /** Runs engine on each position from PGN and returns accuracy (0-100) for white and black. */
    public AnalysisResult run(String pgn) {
        List<PgnReplay.PositionAndMove> positions = PgnReplay.pgnToPositionAndMoves(pgn);
        if (positions.isEmpty()) {
            return new AnalysisResult(0, 0, 0);
        }

        int whiteCorrect = 0, whiteTotal = 0, blackCorrect = 0, blackTotal = 0;

        try (StockfishRunner engine = new StockfishRunner(enginePath, movetimeMs)) {
            engine.start();
            for (PgnReplay.PositionAndMove p : positions) {
                String best = engine.getBestMove(p.fen());
                if (best == null) continue;
                boolean correct = best.equalsIgnoreCase(p.uciMove());
                if (p.isWhite()) {
                    whiteTotal++;
                    if (correct) whiteCorrect++;
                } else {
                    blackTotal++;
                    if (correct) blackCorrect++;
                }
            }
        } catch (Exception e) {
            log.error("Engine analysis failed", e);
            throw new RuntimeException("Engine analysis failed", e);
        }

        int accWhite = whiteTotal == 0 ? 0 : (whiteCorrect * 100) / whiteTotal;
        int accBlack = blackTotal == 0 ? 0 : (blackCorrect * 100) / blackTotal;
        return new AnalysisResult(positions.size(), accWhite, accBlack);
    }

    public record AnalysisResult(int totalMoves, int accuracyWhite, int accuracyBlack) {}
}
