package com.chess.ws.ws;

import com.chess.ws.dto.GameStateMessage;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameStateCache {
    private final ConcurrentHashMap<UUID, GameStateMessage> cache = new ConcurrentHashMap<>();

    public Optional<GameStateMessage> get(UUID gameId) {
        return Optional.ofNullable(cache.get(gameId));
    }

    public void put(UUID gameId, GameStateMessage state) {
        cache.put(gameId, state);
    }

    public void remove(UUID gameId) {
        cache.remove(gameId);
    }
}

