package com.chess.game.service;

import com.chess.game.state.GameState;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface GameStateStore {
    Optional<GameState> get(UUID gameId);
    void put(GameState state, Duration ttl);
    boolean tryLock(UUID gameId, Duration ttl);
    void unlock(UUID gameId);
    Optional<String> getClientMoveResult(UUID gameId, UUID clientMoveId);
    void rememberClientMoveResult(UUID gameId, UUID clientMoveId, String resultJson, Duration ttl);

    void upsertTimeoutDeadline(UUID gameId, long deadlineEpochMs);
    void removeTimeoutDeadline(UUID gameId);
    java.util.List<UUID> pollExpiredTimeouts(long nowEpochMs, int limit);
}

