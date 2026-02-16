package com.chess.game.service;

import com.chess.common.exception.ForbiddenException;
import com.chess.common.exception.NotFoundException;
import com.chess.common.exception.ValidationException;
import com.chess.events.matchmaking.MatchFoundEvent;
import com.chess.game.domain.FinishReason;
import com.chess.game.domain.GameStatus;
import com.chess.game.repo.GameMoveRepository;
import com.chess.game.repo.GameRepository;
import com.chess.game.repo.entity.GameEntity;
import com.chess.game.repo.entity.GameMoveEntity;
import com.chess.game.state.GameClocks;
import com.chess.game.state.GameMove;
import com.chess.game.state.GameState;
import com.chess.game.state.GameTimeControl;
import com.chess.game.util.ChessRules;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final GameMoveRepository gameMoveRepository;
    private final GameStateStore stateStore;
    private final ObjectMapper objectMapper;
    private final GameEventPublisher gameEventPublisher;

    @Value("${game.active-ttl-seconds:7200}")
    private long activeTtlSeconds;

    public GameState getState(UUID gameId) {
        Optional<GameState> cached = stateStore.get(gameId);
        if (cached.isPresent()) {
            return cached.get();
        }

        // Fallback: build state from DB snapshot (and moves)
        GameEntity game = gameRepository.findById(gameId).orElseThrow(() -> new NotFoundException("Game not found"));
        List<GameMoveEntity> moves = gameMoveRepository.findByGameIdOrderByPlyAsc(gameId);

        String fen = game.getCurrentFen();
        if (fen == null || fen.isBlank()) {
            fen = moves.isEmpty() ? new Board().getFen() : moves.get(moves.size() - 1).getFenAfter();
        }

        GameState state = GameState.builder()
                .gameId(gameId)
                .whiteId(game.getWhiteId())
                .blackId(game.getBlackId())
                .fen(fen)
                .timeControl(GameTimeControl.builder()
                        .type(game.getTimeControlType())
                        .baseSeconds(game.getBaseSeconds())
                        .incrementSeconds(game.getIncrementSeconds())
                        .build())
                .rated(game.isRated())
                .status(game.getStatus())
                .result(game.getResult())
                .finishReason(game.getFinishReason())
                .startedAt(game.getStartedAt())
                .finishedAt(game.getFinishedAt())
                .build();

        if (game.getWhiteMs() != null && game.getBlackMs() != null) {
            state.setClocks(GameClocks.builder()
                    .whiteMs(game.getWhiteMs())
                    .blackMs(game.getBlackMs())
                    .lastMoveAt(game.getLastMoveAt())
                    .build());
        } else {
            // fallback: assume full time control if clocks missing
            long baseMs = (long) game.getBaseSeconds() * 1000L;
            state.setClocks(GameClocks.builder()
                    .whiteMs(baseMs)
                    .blackMs(baseMs)
                    .lastMoveAt(game.getStartedAt() != null ? game.getStartedAt() : game.getCreatedAt())
                    .build());
        }

        state.setMoves(moves.stream().map(m -> GameMove.builder()
                .ply(m.getPly())
                .uci(m.getUci())
                .san(m.getSan())
                .fenAfter(m.getFenAfter())
                .playedAt(m.getPlayedAt())
                .byUserId(m.getByUserId())
                .build()).toList());

        setSideToMoveFromFen(state);

        stateStore.put(state, Duration.ofSeconds(activeTtlSeconds));
        updateTimeoutIndex(state);
        return state;
    }

    private void setSideToMoveFromFen(GameState state) {
        if (state.getFen() == null || state.getFen().isBlank()) return;
        try {
            Board board = new Board();
            board.loadFromFen(state.getFen());
            state.setSideToMove(board.getSideToMove().name());
        } catch (Exception e) {
            log.warn("Failed to parse FEN for sideToMove, gameId={}", state.getGameId(), e);
        }
    }

    public void onMatchFound(MatchFoundEvent event) {
        UUID gameId = UUID.fromString(event.getGameId());
        if (gameRepository.existsById(gameId)) {
            // idempotent consumer behavior on redelivery
            log.info("Game already exists for gameId={}, skipping MatchFound", gameId);
            return;
        }
        UUID whiteId = UUID.fromString(event.getWhitePlayerId());
        UUID blackId = UUID.fromString(event.getBlackPlayerId());

        Instant now = Instant.now();
        long baseMs = (long) event.getBaseSeconds() * 1000L;
        String startFen = new Board().getFen();

        GameEntity entity = GameEntity.builder()
                .gameId(gameId)
                .whiteId(whiteId)
                .blackId(blackId)
                .timeControlType(event.getTimeControlType())
                .baseSeconds(event.getBaseSeconds())
                .incrementSeconds(event.getIncrementSeconds())
                .rated(event.getRated() != null && event.getRated())
                .status(GameStatus.RUNNING)
                .createdAt(now)
                .startedAt(now)
                .currentFen(startFen)
                .whiteMs(baseMs)
                .blackMs(baseMs)
                .lastMoveAt(now)
                .build();
        gameRepository.save(entity);

        Board startBoard = new Board();
        GameState state = GameState.builder()
                .gameId(gameId)
                .whiteId(whiteId)
                .blackId(blackId)
                .fen(startFen)
                .sideToMove(startBoard.getSideToMove().name())
                .clocks(GameClocks.builder()
                        .whiteMs(baseMs)
                        .blackMs(baseMs)
                        .lastMoveAt(now)
                        .build())
                .timeControl(GameTimeControl.builder()
                        .type(event.getTimeControlType())
                        .baseSeconds(event.getBaseSeconds())
                        .incrementSeconds(event.getIncrementSeconds())
                        .build())
                .rated(event.getRated() != null && event.getRated())
                .status(GameStatus.RUNNING)
                .startedAt(entity.getStartedAt())
                .build();
        stateStore.put(state, Duration.ofSeconds(activeTtlSeconds));
        updateTimeoutIndex(state);

        gameEventPublisher.publishGameCreated(state);
        gameEventPublisher.publishGameStarted(state);
    }

    public GameState applyMove(UUID gameId, UUID userId, String uci, UUID clientMoveId) {
        if (clientMoveId != null) {
            Optional<String> cached = stateStore.getClientMoveResult(gameId, clientMoveId);
            if (cached.isPresent()) {
                try {
                    return objectMapper.readValue(cached.get(), GameState.class);
                } catch (Exception ignore) {
                }
            }
        }

        Duration lockTtl = Duration.ofSeconds(2);
        if (!stateStore.tryLock(gameId, lockTtl)) {
            throw new ValidationException("Game is busy, retry");
        }

        try {
            GameState state = stateStore.get(gameId)
                    .orElseThrow(() -> new NotFoundException("Game state not found (not started yet?)"));

            ensureParticipant(state, userId);
            if (state.getStatus() != GameStatus.RUNNING) {
                throw new ValidationException("Game is not running");
            }

            Board board = new Board();
            String fen = state.getFen();
            if (fen != null && !fen.isBlank()) {
                board.loadFromFen(fen);
            }

            Side sideToMove = board.getSideToMove();
            UUID expected = sideToMove == Side.WHITE ? state.getWhiteId() : state.getBlackId();
            if (!expected.equals(userId)) {
                throw new ForbiddenException("Not your turn");
            }

            // apply clock consumption for side to move BEFORE applying move
            GameClocks clocks = state.getClocks();
            Instant now = Instant.now();
            long elapsed = clocks.getLastMoveAt() != null ? Math.max(0L, Duration.between(clocks.getLastMoveAt(), now).toMillis()) : 0L;
            if (sideToMove == Side.WHITE) {
                clocks.setWhiteMs(clocks.getWhiteMs() - elapsed);
            } else {
                clocks.setBlackMs(clocks.getBlackMs() - elapsed);
            }

            if (clocks.getWhiteMs() <= 0 || clocks.getBlackMs() <= 0) {
                // time expired before move, finish
                return finishOnTimeout(state, sideToMove == Side.WHITE ? state.getWhiteId() : state.getBlackId());
            }

            Move move = ChessRules.parseUci(uci, sideToMove);
            boolean ok = ChessRules.applyMove(board, move);
            if (!ok) {
                throw new ValidationException("Illegal move");
            }

            // increment for player who moved
            long incrementMs = (long) state.getTimeControl().getIncrementSeconds() * 1000L;
            if (sideToMove == Side.WHITE) {
                clocks.setWhiteMs(clocks.getWhiteMs() + incrementMs);
            } else {
                clocks.setBlackMs(clocks.getBlackMs() + incrementMs);
            }
            clocks.setLastMoveAt(now);

            int ply = state.getMoves().size() + 1;
            GameMove gm = GameMove.builder()
                    .ply(ply)
                    .uci(uci)
                    .san(move.getSan() != null ? move.getSan() : uci)
                    .fenAfter(board.getFen())
                    .playedAt(now)
                    .byUserId(userId)
                    .build();
            state.getMoves().add(gm);
            state.setFen(board.getFen());
            state.setSideToMove(board.getSideToMove().name());

            // Persist move and runtime snapshot
            GameEntity entity = gameRepository.findById(gameId).orElseThrow(() -> new NotFoundException("Game not found"));
            entity.setCurrentFen(state.getFen());
            entity.setWhiteMs(state.getClocks().getWhiteMs());
            entity.setBlackMs(state.getClocks().getBlackMs());
            entity.setLastMoveAt(state.getClocks().getLastMoveAt());
            gameRepository.save(entity);

            gameMoveRepository.save(GameMoveEntity.builder()
                    .gameId(gameId)
                    .ply(ply)
                    .uci(uci)
                    .san(gm.getSan())
                    .fenAfter(gm.getFenAfter())
                    .playedAt(now)
                    .byUserId(userId)
                    .whiteMsAfter(state.getClocks().getWhiteMs())
                    .blackMsAfter(state.getClocks().getBlackMs())
                    .build());

            // clear draw offer on move
            state.setDrawOfferedBy(null);

            // check game end
            boolean finishedNow = false;
            if (board.isMated()) {
                String result = sideToMove == Side.WHITE ? "1-0" : "0-1";
                finish(state, result, FinishReason.CHECKMATE);
                finishedNow = true;
            } else if (board.isStaleMate()) {
                finish(state, "1/2-1/2", FinishReason.STALEMATE);
                finishedNow = true;
            } else if (board.isInsufficientMaterial()) {
                finish(state, "1/2-1/2", FinishReason.INSUFFICIENT_MATERIAL);
                finishedNow = true;
            } else if (board.isRepetition()) {
                finish(state, "1/2-1/2", FinishReason.THREEFOLD_REPETITION);
                finishedNow = true;
            } else if (board.isDraw()) {
                // generic draw state (50-move etc)
                finish(state, "1/2-1/2", FinishReason.FIFTY_MOVE_RULE);
                finishedNow = true;
            }

            gameEventPublisher.publishMoveMade(state, gm, board, sideToMove);

            if (finishedNow) {
                stateStore.put(state, Duration.ofMinutes(30));
                stateStore.removeTimeoutDeadline(gameId);
                gameEventPublisher.publishGameFinished(state);
            } else {
                stateStore.put(state, Duration.ofSeconds(activeTtlSeconds));
                updateTimeoutIndex(state);
            }

            if (clientMoveId != null) {
                stateStore.rememberClientMoveResult(gameId, clientMoveId, objectMapper.writeValueAsString(state), Duration.ofHours(24));
            }

            return state;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            stateStore.unlock(gameId);
        }
    }

    public GameState resign(UUID gameId, UUID userId) {
        Duration lockTtl = Duration.ofSeconds(2);
        if (!stateStore.tryLock(gameId, lockTtl)) {
            throw new ValidationException("Game is busy, retry");
        }
        try {
            GameState state = stateStore.get(gameId).orElseThrow(() -> new NotFoundException("Game not found"));
            ensureParticipant(state, userId);
            if (state.getStatus() != GameStatus.RUNNING) {
                return state;
            }
            UUID winner = userId.equals(state.getWhiteId()) ? state.getBlackId() : state.getWhiteId();
            String result = winner.equals(state.getWhiteId()) ? "1-0" : "0-1";
            finish(state, result, FinishReason.RESIGN);
            stateStore.put(state, Duration.ofMinutes(30));
            stateStore.removeTimeoutDeadline(gameId);
            gameEventPublisher.publishGameFinished(state);
            return state;
        } finally {
            stateStore.unlock(gameId);
        }
    }

    public GameState offerDraw(UUID gameId, UUID userId) {
        GameState state = stateStore.get(gameId).orElseThrow(() -> new NotFoundException("Game not found"));
        ensureParticipant(state, userId);
        state.setDrawOfferedBy(userId);
        stateStore.put(state, Duration.ofSeconds(activeTtlSeconds));
        return state;
    }

    public GameState acceptDraw(UUID gameId, UUID userId) {
        Duration lockTtl = Duration.ofSeconds(2);
        if (!stateStore.tryLock(gameId, lockTtl)) {
            throw new ValidationException("Game is busy, retry");
        }
        try {
            GameState state = stateStore.get(gameId).orElseThrow(() -> new NotFoundException("Game not found"));
            ensureParticipant(state, userId);
            if (state.getDrawOfferedBy() == null || state.getDrawOfferedBy().equals(userId)) {
                throw new ValidationException("No draw offer to accept");
            }
            finish(state, "1/2-1/2", FinishReason.DRAW_AGREEMENT);
            stateStore.put(state, Duration.ofMinutes(30));
            stateStore.removeTimeoutDeadline(gameId);
            gameEventPublisher.publishGameFinished(state);
            return state;
        } finally {
            stateStore.unlock(gameId);
        }
    }

    public void scanTimeouts(int batchLimit) {
        long now = Instant.now().toEpochMilli();
        List<UUID> expired = stateStore.pollExpiredTimeouts(now, batchLimit);
        for (UUID gameId : expired) {
            if (!stateStore.tryLock(gameId, Duration.ofSeconds(2))) {
                continue;
            }
            try {
                GameState state = stateStore.get(gameId).orElseGet(() -> getState(gameId));
                if (state.getStatus() != GameStatus.RUNNING) {
                    stateStore.removeTimeoutDeadline(gameId);
                    continue;
                }

                Board board = new Board();
                if (state.getFen() != null && !state.getFen().isBlank()) {
                    board.loadFromFen(state.getFen());
                }
                Side sideToMove = board.getSideToMove();

                // Recompute remaining time as of now
                GameClocks clocks = state.getClocks();
                Instant nowTs = Instant.now();
                long elapsed = clocks.getLastMoveAt() != null ? Math.max(0L, Duration.between(clocks.getLastMoveAt(), nowTs).toMillis()) : 0L;
                long whiteMs = clocks.getWhiteMs();
                long blackMs = clocks.getBlackMs();
                if (sideToMove == Side.WHITE) {
                    whiteMs -= elapsed;
                } else {
                    blackMs -= elapsed;
                }
                clocks.setWhiteMs(whiteMs);
                clocks.setBlackMs(blackMs);

                if (whiteMs <= 0 || blackMs <= 0) {
                    UUID timedOut = sideToMove == Side.WHITE ? state.getWhiteId() : state.getBlackId();
                    finishOnTimeout(state, timedOut);
                    stateStore.put(state, Duration.ofMinutes(30));
                    stateStore.removeTimeoutDeadline(gameId);
                    // persist snapshot
                    GameEntity entity = gameRepository.findById(gameId).orElseThrow(() -> new NotFoundException("Game not found"));
                    entity.setCurrentFen(state.getFen());
                    entity.setWhiteMs(state.getClocks().getWhiteMs());
                    entity.setBlackMs(state.getClocks().getBlackMs());
                    entity.setLastMoveAt(state.getClocks().getLastMoveAt());
                    gameRepository.save(entity);
                } else {
                    // still alive: update deadline
                    clocks.setLastMoveAt(nowTs);
                    stateStore.put(state, Duration.ofSeconds(activeTtlSeconds));
                    updateTimeoutIndex(state);
                }
            } catch (Exception e) {
                log.warn("Timeout processing failed for gameId={}", gameId, e);
            } finally {
                stateStore.unlock(gameId);
            }
        }
    }

    private GameState finishOnTimeout(GameState state, UUID timedOutUserId) {
        UUID winner = timedOutUserId.equals(state.getWhiteId()) ? state.getBlackId() : state.getWhiteId();
        String result = winner.equals(state.getWhiteId()) ? "1-0" : "0-1";
        finish(state, result, FinishReason.TIMEOUT);
        gameEventPublisher.publishTimeExpired(state, timedOutUserId);
        gameEventPublisher.publishGameFinished(state);
        return state;
    }

    private void finish(GameState state, String result, FinishReason reason) {
        if (state.getStatus() == GameStatus.FINISHED) {
            return;
        }
        state.setStatus(GameStatus.FINISHED);
        state.setResult(result);
        state.setFinishReason(reason.name());
        state.setFinishedAt(Instant.now());
        if ("1-0".equals(result)) {
            state.setWinnerId(state.getWhiteId());
        } else if ("0-1".equals(result)) {
            state.setWinnerId(state.getBlackId());
        } else {
            state.setWinnerId(null);
        }

        // persist to DB
        GameEntity entity = gameRepository.findById(state.getGameId())
                .orElseThrow(() -> new NotFoundException("Game not found"));
        entity.setStatus(GameStatus.FINISHED);
        entity.setResult(result);
        entity.setFinishReason(reason.name());
        entity.setFinishedAt(state.getFinishedAt());
        entity.setPgn(PgnBuilder.buildPgn(state));
        entity.setCurrentFen(state.getFen());
        if (state.getClocks() != null) {
            entity.setWhiteMs(state.getClocks().getWhiteMs());
            entity.setBlackMs(state.getClocks().getBlackMs());
            entity.setLastMoveAt(state.getClocks().getLastMoveAt());
        }
        gameRepository.save(entity);
    }

    private void updateTimeoutIndex(GameState state) {
        try {
            if (state.getStatus() != GameStatus.RUNNING || state.getClocks() == null) {
                stateStore.removeTimeoutDeadline(state.getGameId());
                return;
            }
            Board board = new Board();
            if (state.getFen() != null && !state.getFen().isBlank()) {
                board.loadFromFen(state.getFen());
            }
            Side stm = board.getSideToMove();
            long remaining = stm == Side.WHITE ? state.getClocks().getWhiteMs() : state.getClocks().getBlackMs();
            Instant last = state.getClocks().getLastMoveAt() != null ? state.getClocks().getLastMoveAt() : Instant.now();
            long deadline = last.toEpochMilli() + Math.max(0L, remaining);
            stateStore.upsertTimeoutDeadline(state.getGameId(), deadline);
        } catch (Exception e) {
            log.warn("Failed to update timeout index for gameId={}", state.getGameId(), e);
        }
    }

    public void ensureParticipant(GameState state, UUID userId) {
        if (!userId.equals(state.getWhiteId()) && !userId.equals(state.getBlackId())) {
            throw new ForbiddenException("User is not a participant of this game");
        }
    }
}

