package com.chess.ws.messaging;

import com.chess.events.constants.NatsSubjects;
import com.chess.events.common.EventEnvelope;
import com.chess.events.game.GameFinishedEvent;
import com.chess.events.game.GameStartedEvent;
import com.chess.events.game.MoveMadeEvent;
import com.chess.events.game.TimeExpiredEvent;
import com.chess.common.messaging.ProcessedEventStore;
import com.chess.ws.client.GameServiceClient;
import com.chess.ws.dto.GameStateMessage;
import com.chess.ws.dto.GameMoveMessage;
import com.chess.ws.dto.GameClocksMessage;
import com.chess.ws.dto.ws.WsGameFinishedMessage;
import com.chess.ws.dto.ws.WsGameStateMessage;
import com.chess.ws.dto.ws.WsMove;
import com.chess.ws.dto.ws.WsMoveAcceptedMessage;
import com.chess.ws.ws.GameStateCache;
import com.chess.ws.ws.WsSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.JetStream;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.PushSubscribeOptions;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import io.nats.client.api.ReplayPolicy;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "nats.enabled", havingValue = "true", matchIfMissing = true)
public class GameEventsListener {

    private final Connection natsConnection;
    private final JetStream jetStream;
    private final ObjectMapper objectMapper;
    private final WsSessionRegistry registry;
    private final GameServiceClient gameServiceClient;
    private final GameStateCache cache;
    private final ProcessedEventStore processedEventStore;

    private Dispatcher dispatcher;
    private JetStreamSubscription subStarted;
    private JetStreamSubscription subMove;
    private JetStreamSubscription subFinished;
    private JetStreamSubscription subTimeExpired;

    private static final String CONSUMER = "ws-service-domain-game";

    @PostConstruct
    public void init() {
        try {
            dispatcher = natsConnection.createDispatcher();

            ConsumerConfiguration config = ConsumerConfiguration.builder()
                    .durable(CONSUMER)
                    .ackPolicy(AckPolicy.Explicit)
                    .ackWait(Duration.ofSeconds(30))
                    .maxDeliver(5)
                    .backoff(
                            Duration.ofSeconds(1),
                            Duration.ofSeconds(5),
                            Duration.ofSeconds(15),
                            Duration.ofSeconds(30),
                            Duration.ofSeconds(60)
                    )
                    .deliverPolicy(DeliverPolicy.All)
                    .replayPolicy(ReplayPolicy.Instant)
                    .build();
            PushSubscribeOptions opts = PushSubscribeOptions.builder().configuration(config).build();

            subStarted = jetStream.subscribe(NatsSubjects.GAME_STARTED, dispatcher, this::onStarted, false, opts);
            subMove = jetStream.subscribe(NatsSubjects.GAME_MOVE_MADE, dispatcher, this::onMove, false, opts);
            subFinished = jetStream.subscribe(NatsSubjects.GAME_FINISHED, dispatcher, this::onFinished, false, opts);
            subTimeExpired = jetStream.subscribe(NatsSubjects.GAME_TIME_EXPIRED, dispatcher, this::onTimeExpired, false, opts);

            log.info("JetStream subscribed to game events: {}, {}, {}", NatsSubjects.GAME_STARTED, NatsSubjects.GAME_MOVE_MADE, NatsSubjects.GAME_FINISHED);
        } catch (Exception e) {
            log.error("Failed to initialize JetStream subscriptions", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        tryUnsub(subStarted);
        tryUnsub(subMove);
        tryUnsub(subFinished);
        tryUnsub(subTimeExpired);
    }

    private void onStarted(Message msg) {
        try {
            EventEnvelope<GameStartedEvent> env = objectMapper.readValue(new String(msg.getData(), StandardCharsets.UTF_8),
                    objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, GameStartedEvent.class));
            if (env.getEventId() != null && processedEventStore.isProcessed(CONSUMER, env.getEventId())) {
                msg.ack();
                return;
            }
            GameStartedEvent e = env.getPayload();
            if (e == null) {
                msg.ack();
                return;
            }
            UUID gameId = UUID.fromString(e.getGameId());

            // Spec: on GameStarted send GAME_STATE. We can fetch state using any connected participant token.
            GameStateMessage state = fetchStateUsingAnySessionToken(gameId);
            if (state != null) {
                cache.put(gameId, state);
                broadcast(gameId, toWsGameState(state, null));
            }
            if (env.getEventId() != null) {
                processedEventStore.markProcessed(CONSUMER, env.getEventId());
            }
            msg.ack();
        } catch (Exception ex) {
            log.warn("Failed to handle GameStarted", ex);
            safeNak(msg);
        }
    }

    private void onMove(Message msg) {
        try {
            EventEnvelope<MoveMadeEvent> env = objectMapper.readValue(new String(msg.getData(), StandardCharsets.UTF_8),
                    objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, MoveMadeEvent.class));
            if (env.getEventId() != null && processedEventStore.isProcessed(CONSUMER, env.getEventId())) {
                msg.ack();
                return;
            }
            MoveMadeEvent e = env.getPayload();
            if (e == null) {
                msg.ack();
                return;
            }
            UUID gameId = UUID.fromString(e.getGameId());
            String uci = e.getFrom() + e.getTo() + (e.getPromotion() != null ? e.getPromotion() : "");

            // update cache
            cache.get(gameId).ifPresent(st -> {
                st.setFen(e.getFen());
                st.setClocks(new GameClocksMessage(e.getWhiteTimeLeftMs(), e.getBlackTimeLeftMs()));
                if (st.getMoves() != null) {
                    st.getMoves().add(new GameMoveMessage(
                            e.getMoveNumber(),
                            uci,
                            e.getSan(),
                            e.getFen(),
                            null,
                            UUID.fromString(e.getPlayerId())
                    ));
                }
            });

            broadcast(gameId, WsMoveAcceptedMessage.builder()
                    .gameId(gameId)
                    .clientMoveId(null)
                    .ply(e.getMoveNumber())
                    .fen(e.getFen())
                    .clocks(new GameClocksMessage((long) e.getWhiteTimeLeftMs(), (long) e.getBlackTimeLeftMs()))
                    .build());
            if (env.getEventId() != null) {
                processedEventStore.markProcessed(CONSUMER, env.getEventId());
            }
            msg.ack();
        } catch (Exception ex) {
            log.warn("Failed to handle MoveMade", ex);
            safeNak(msg);
        }
    }

    private void onTimeExpired(Message msg) {
        try {
            EventEnvelope<TimeExpiredEvent> env = objectMapper.readValue(new String(msg.getData(), StandardCharsets.UTF_8),
                    objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, TimeExpiredEvent.class));
            if (env.getEventId() != null && processedEventStore.isProcessed(CONSUMER, env.getEventId())) {
                msg.ack();
                return;
            }
            TimeExpiredEvent e = env.getPayload();
            if (e == null) {
                msg.ack();
                return;
            }
            UUID gameId = UUID.fromString(e.getGameId());
            broadcast(gameId, Map.of("type", "TIME_EXPIRED", "gameId", gameId, "playerId", e.getPlayerId(), "color", e.getColor()));
            if (env.getEventId() != null) {
                processedEventStore.markProcessed(CONSUMER, env.getEventId());
            }
            msg.ack();
        } catch (Exception ex) {
            log.warn("Failed to handle TimeExpired", ex);
            safeNak(msg);
        }
    }

    private void onFinished(Message msg) {
        try {
            EventEnvelope<GameFinishedEvent> env = objectMapper.readValue(new String(msg.getData(), StandardCharsets.UTF_8),
                    objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, GameFinishedEvent.class));
            if (env.getEventId() != null && processedEventStore.isProcessed(CONSUMER, env.getEventId())) {
                msg.ack();
                return;
            }
            GameFinishedEvent e = env.getPayload();
            if (e == null) {
                msg.ack();
                return;
            }
            UUID gameId = UUID.fromString(e.getGameId());
            cache.remove(gameId);
            broadcast(gameId, WsGameFinishedMessage.builder()
                    .gameId(gameId)
                    .result(e.getResult())
                    .reason(e.getFinishReason())
                    .build());
            if (env.getEventId() != null) {
                processedEventStore.markProcessed(CONSUMER, env.getEventId());
            }
            msg.ack();
        } catch (Exception ex) {
            log.warn("Failed to handle GameFinished", ex);
            safeNak(msg);
        }
    }

    private void broadcast(UUID gameId, Object message) {
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.warn("Failed to serialize WS message", e);
            return;
        }

        TextMessage tm = new TextMessage(json);
        for (WebSocketSession session : registry.get(gameId)) {
            try {
                if (session.isOpen()) {
                    // Deduplicate for the sender session: if it already received MOVE_ACCEPTED for this ply via direct response,
                    // skip the broadcast with same ply.
                    if (message instanceof WsMoveAcceptedMessage mam && mam.getPly() != null) {
                        Object lastObj = session.getAttributes().get("last_sent_ply");
                        int last = lastObj instanceof Integer li ? li : 0;
                        if (mam.getPly() <= last) {
                            continue;
                        }
                    }
                    session.sendMessage(tm);
                }
            } catch (Exception e) {
                log.debug("Failed to send WS message to sessionId={}", session.getId(), e);
            }
        }
    }

    private GameStateMessage fetchStateUsingAnySessionToken(UUID gameId) {
        for (WebSocketSession session : registry.get(gameId)) {
            try {
                if (!session.isOpen()) {
                    continue;
                }
                Object tokenObj = session.getAttributes().get(com.chess.ws.ws.JwtHandshakeInterceptor.ATTR_TOKEN);
                if (tokenObj instanceof String token && !token.isBlank()) {
                    return gameServiceClient.getState(gameId, token);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private WsGameStateMessage toWsGameState(GameStateMessage state, Integer lastSeenPly) {
        List<WsMove> moves = null;
        if (state.getMoves() != null) {
            moves = state.getMoves().stream()
                    .filter(m -> lastSeenPly == null || lastSeenPly <= 0 || m.getPly() > lastSeenPly)
                    .map(m -> new WsMove(m.getPly(), m.getUci(), m.getSan()))
                    .toList();
        }
        return WsGameStateMessage.builder()
                .gameId(state.getGameId())
                .whiteId(state.getWhiteId())
                .blackId(state.getBlackId())
                .fen(state.getFen())
                .moves(moves)
                .clocks(state.getClocks())
                .status(state.getStatus())
                .sideToMove(state.getSideToMove())
                .build();
    }

    private void tryUnsub(JetStreamSubscription sub) {
        if (sub == null) return;
        try {
            sub.unsubscribe();
        } catch (Exception ignored) {
        }
    }

    private void safeNak(Message msg) {
        try {
            msg.nak();
        } catch (Exception ignored) {
        }
    }
}

