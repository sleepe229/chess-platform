package com.chess.ws.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WsSessionRegistry {

    private final ConcurrentHashMap<UUID, Set<WebSocketSession>> sessionsByGame = new ConcurrentHashMap<>();

    public void add(UUID gameId, WebSocketSession session) {
        sessionsByGame.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("WS session added: gameId={}, sessionId={}", gameId, session.getId());
    }

    public void remove(UUID gameId, WebSocketSession session) {
        Set<WebSocketSession> set = sessionsByGame.get(gameId);
        if (set == null) {
            return;
        }
        set.remove(session);
        if (set.isEmpty()) {
            sessionsByGame.remove(gameId);
        }
        log.debug("WS session removed: gameId={}, sessionId={}", gameId, session.getId());
    }

    public Set<WebSocketSession> get(UUID gameId) {
        return sessionsByGame.getOrDefault(gameId, Set.of());
    }
}

