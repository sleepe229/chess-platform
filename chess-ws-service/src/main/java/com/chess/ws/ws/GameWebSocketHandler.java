package com.chess.ws.ws;

import com.chess.ws.client.GameServiceClient;
import com.chess.ws.dto.GameStateMessage;
import com.chess.ws.dto.MoveCommand;
import com.chess.ws.dto.ws.WsGameStateMessage;
import com.chess.ws.dto.ws.WsMove;
import com.chess.ws.dto.ws.WsMoveAcceptedMessage;
import com.chess.ws.dto.ws.WsMoveRejectedMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final WsSessionRegistry registry;
    private final GameServiceClient gameServiceClient;
    private final UserConnectionLimiter limiter;
    private final GameStateCache cache;

    @Value("${ws.max-messages-per-second:10}")
    private int maxMessagesPerSecond;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID gameId = (UUID) session.getAttributes().get(JwtHandshakeInterceptor.ATTR_GAME_ID);
        registry.add(gameId, session);

        // init dedupe marker for broadcasts
        session.getAttributes().put("last_sent_ply", 0);

        sendGameState(session, gameId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (!rateLimitOk(session)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        UUID gameId = (UUID) session.getAttributes().get(JwtHandshakeInterceptor.ATTR_GAME_ID);
        UUID userId = (UUID) session.getAttributes().get(JwtHandshakeInterceptor.ATTR_USER_ID);
        String token = (String) session.getAttributes().get(JwtHandshakeInterceptor.ATTR_TOKEN);

        JsonNode root = objectMapper.readTree(message.getPayload());
        String type = root.hasNonNull("type") ? root.get("type").asText() : "";

        switch (type) {
            case "SYNC" -> {
                sendGameState(session, gameId, root.hasNonNull("lastSeenPly") ? root.get("lastSeenPly").asInt(0) : null);
            }
            case "MOVE" -> {
                String uci = root.hasNonNull("uci") ? root.get("uci").asText() : null;
                String clientMoveId = root.hasNonNull("clientMoveId") ? root.get("clientMoveId").asText() : null;
                if (uci == null || uci.isBlank()) {
                    send(session, WsMoveRejectedMessage.builder()
                            .gameId(gameId)
                            .clientMoveId(clientMoveId)
                            .reason("MISSING_UCI")
                            .build());
                    return;
                }

                try {
                    GameStateMessage state = gameServiceClient.move(gameId, token, new MoveCommand(uci, clientMoveId));
                    int ply = state.getMoves() != null ? state.getMoves().size() : 0;
                    session.getAttributes().put("last_sent_ply", ply);
                    send(session, WsMoveAcceptedMessage.builder()
                            .gameId(gameId)
                            .clientMoveId(clientMoveId)
                            .ply(ply)
                            .fen(state.getFen())
                            .clocks(state.getClocks())
                            .build());
                } catch (Exception e) {
                    String code = gameServiceClient.extractErrorCode(e);
                    send(session, WsMoveRejectedMessage.builder()
                            .gameId(gameId)
                            .clientMoveId(clientMoveId)
                            .reason(code)
                            .build());
                }
            }
            case "RESIGN" -> {
                try {
                    gameServiceClient.resign(gameId, token);
                    send(session, Map.of("type", "RESIGN_ACCEPTED", "gameId", gameId, "userId", userId));
                } catch (Exception e) {
                    send(session, Map.of("type", "RESIGN_REJECTED", "gameId", gameId, "reason", gameServiceClient.extractErrorCode(e)));
                }
            }
            case "OFFER_DRAW" -> {
                try {
                    gameServiceClient.offerDraw(gameId, token);
                    send(session, Map.of("type", "DRAW_OFFERED", "gameId", gameId, "userId", userId));
                } catch (Exception e) {
                    send(session, Map.of("type", "DRAW_OFFER_REJECTED", "gameId", gameId, "reason", gameServiceClient.extractErrorCode(e)));
                }
            }
            case "ACCEPT_DRAW" -> {
                try {
                    gameServiceClient.acceptDraw(gameId, token);
                    send(session, Map.of("type", "DRAW_ACCEPTED", "gameId", gameId, "userId", userId));
                } catch (Exception e) {
                    send(session, Map.of("type", "DRAW_ACCEPT_REJECTED", "gameId", gameId, "reason", gameServiceClient.extractErrorCode(e)));
                }
            }
            default -> {
                send(session, Map.of("type", "ERROR", "message", "Unknown message type"));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID userId = (UUID) session.getAttributes().get(JwtHandshakeInterceptor.ATTR_USER_ID);
        if (userId != null) {
            limiter.release(userId);
        }
        UUID gameId = (UUID) session.getAttributes().get(JwtHandshakeInterceptor.ATTR_GAME_ID);
        if (gameId != null) {
            registry.remove(gameId, session);
        }
    }

    private void sendGameState(WebSocketSession session, UUID gameId) throws Exception {
        sendGameState(session, gameId, null);
    }

    private void sendGameState(WebSocketSession session, UUID gameId, Integer lastSeenPly) throws Exception {
        String token = (String) session.getAttributes().get(JwtHandshakeInterceptor.ATTR_TOKEN);
        GameStateMessage state = gameServiceClient.getState(gameId, token);
        cache.put(gameId, state);

        List<WsMove> moves = null;
        if (state.getMoves() != null) {
            moves = state.getMoves().stream()
                    .filter(m -> lastSeenPly == null || lastSeenPly <= 0 || m.getPly() > lastSeenPly)
                    .map(m -> new WsMove(m.getPly(), m.getUci(), m.getSan()))
                    .toList();
        }

        send(session, WsGameStateMessage.builder()
                .gameId(state.getGameId())
                .whiteId(state.getWhiteId())
                .blackId(state.getBlackId())
                .fen(state.getFen())
                .moves(moves)
                .clocks(state.getClocks())
                .status(state.getStatus())
                .sideToMove(state.getSideToMove())
                .build());
    }

    private void send(WebSocketSession session, Object msg) throws Exception {
        if (!session.isOpen()) {
            return;
        }
        String json = objectMapper.writeValueAsString(msg);
        session.sendMessage(new TextMessage(json));
    }

    private boolean rateLimitOk(WebSocketSession session) {
        long nowSec = Instant.now().getEpochSecond();
        Long window = (Long) session.getAttributes().get("rl_window");
        Integer count = (Integer) session.getAttributes().get("rl_count");
        if (window == null || window != nowSec) {
            session.getAttributes().put("rl_window", nowSec);
            session.getAttributes().put("rl_count", 1);
            return true;
        }
        int next = (count != null ? count : 0) + 1;
        session.getAttributes().put("rl_count", next);
        return next <= maxMessagesPerSecond;
    }
}

