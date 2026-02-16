package com.chess.analytics.engine;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Runs Stockfish (or any UCI engine) as subprocess and returns best move for a FEN position.
 * Engine path is configurable (e.g. stockfish, /usr/bin/stockfish, or path in Docker).
 */
@Slf4j
public class StockfishRunner implements AutoCloseable {

    private static final Pattern BESTMOVE = Pattern.compile("bestmove\\s+(\\S+)(?:\\s+ponder\\s+\\S+)?");
    private static final long DEFAULT_MOVETIME_MS = 500;

    private final String enginePath;
    private final long movetimeMs;
    private Process process;
    private OutputStream stdin;
    private BufferedReader stdout;

    public StockfishRunner(String enginePath, long movetimeMs) {
        this.enginePath = enginePath == null || enginePath.isBlank() ? "stockfish" : enginePath;
        this.movetimeMs = movetimeMs > 0 ? movetimeMs : DEFAULT_MOVETIME_MS;
    }

    public void start() throws IOException {
        ProcessBuilder pb = new ProcessBuilder(enginePath);
        pb.redirectErrorStream(true);
        process = pb.start();
        stdin = process.getOutputStream();
        stdout = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        send("uci");
        waitFor("uciok");
        send("isready");
        waitFor("readyok");
        log.debug("Stockfish started: {}", enginePath);
    }

    /** Returns UCI best move (e.g. e2e4) for the given FEN, or null if engine fails. */
    public String getBestMove(String fen) {
        if (process == null || !process.isAlive()) return null;
        try {
            send("position fen " + fen);
            send("go movetime " + movetimeMs);
            String best = waitForBestMove();
            return best;
        } catch (Exception e) {
            log.warn("getBestMove failed for fen", e);
            return null;
        }
    }

    private void send(String line) throws IOException {
        if (stdin == null) return;
        stdin.write((line + "\n").getBytes(StandardCharsets.UTF_8));
        stdin.flush();
    }

    private void waitFor(String token) throws IOException {
        String line;
        while ((line = stdout.readLine()) != null) {
            if (line.strip().toLowerCase().contains(token.toLowerCase())) return;
        }
    }

    private String waitForBestMove() throws IOException {
        String line;
        while ((line = stdout.readLine()) != null) {
            var m = BESTMOVE.matcher(line);
            if (m.find()) {
                String move = m.group(1);
                if (!"none".equalsIgnoreCase(move)) return move;
                return null;
            }
        }
        return null;
    }

    @Override
    public void close() {
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            try {
                process.waitFor(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
