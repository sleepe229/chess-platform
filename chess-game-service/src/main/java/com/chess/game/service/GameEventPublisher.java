package com.chess.game.service;

import com.chess.events.common.EventEnvelope;
import com.chess.events.game.GameCreatedEvent;
import com.chess.events.game.GameFinishedEvent;
import com.chess.events.game.GameStartedEvent;
import com.chess.events.game.MoveMadeEvent;
import com.chess.events.game.TimeExpiredEvent;
import com.chess.events.util.EventBuilder;
import com.chess.game.domain.FinishReason;
import com.chess.game.state.GameMove;
import com.chess.game.state.GameState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import io.nats.client.JetStream;
import io.nats.client.impl.Headers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static com.chess.events.constants.NatsSubjects.*;

@Slf4j
@Component
public class GameEventPublisher {

    private static final String PRODUCER = "game-service";

    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private JetStream jetStream;

    public GameEventPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void publishGameCreated(GameState state) {
        GameCreatedEvent payload = GameCreatedEvent.builder()
                .gameId(state.getGameId().toString())
                .whitePlayerId(state.getWhiteId().toString())
                .blackPlayerId(state.getBlackId().toString())
                .timeControl(state.getTimeControl().getType())
                .initialTimeSeconds(state.getTimeControl().getBaseSeconds())
                .incrementSeconds(state.getTimeControl().getIncrementSeconds())
                .build();
        EventEnvelope<GameCreatedEvent> e = EventBuilder.envelope("GameCreated", PRODUCER, payload);
        publishWithHeaders(GAME_CREATED, e);
    }

    public void publishGameStarted(GameState state) {
        GameStartedEvent payload = GameStartedEvent.builder()
                .gameId(state.getGameId().toString())
                .startedAt(state.getStartedAt() != null ? state.getStartedAt().toString() : Instant.now().toString())
                .build();
        EventEnvelope<GameStartedEvent> e = EventBuilder.envelope("GameStarted", PRODUCER, payload);
        publishWithHeaders(GAME_STARTED, e);
    }

    public void publishMoveMade(GameState state, GameMove gm, Board board, Side sideMoved) {
        MoveMadeEvent payload = MoveMadeEvent.builder()
                .gameId(state.getGameId().toString())
                .moveNumber(gm.getPly())
                .playerId(gm.getByUserId().toString())
                .color(sideMoved == Side.WHITE ? "WHITE" : "BLACK")
                .from(gm.getUci().substring(0, 2))
                .to(gm.getUci().substring(2, 4))
                .promotion(gm.getUci().length() > 4 ? gm.getUci().substring(4) : null)
                .san(gm.getSan() != null ? gm.getSan() : gm.getUci())
                .fen(gm.getFenAfter())
                .whiteTimeLeftMs((int) Math.max(0, state.getClocks().getWhiteMs()))
                .blackTimeLeftMs((int) Math.max(0, state.getClocks().getBlackMs()))
                .isCheck(board.isKingAttacked())
                .isCheckmate(board.isMated())
                .isStalemate(board.isStaleMate())
                .isDraw(board.isDraw())
                .build();
        EventEnvelope<MoveMadeEvent> e = EventBuilder.envelope("MoveMade", PRODUCER, payload);
        publishWithHeaders(GAME_MOVE_MADE, e);
    }

    public void publishTimeExpired(GameState state, java.util.UUID timedOutUser) {
        String color = timedOutUser.equals(state.getWhiteId()) ? "WHITE" : "BLACK";
        TimeExpiredEvent payload = TimeExpiredEvent.builder()
                .gameId(state.getGameId().toString())
                .playerId(timedOutUser.toString())
                .color(color)
                .build();
        EventEnvelope<TimeExpiredEvent> e = EventBuilder.envelope("TimeExpired", PRODUCER, payload);
        publishWithHeaders(GAME_TIME_EXPIRED, e);
    }

    public void publishGameFinished(GameState state) {
        GameFinishedEvent payload = GameFinishedEvent.builder()
                .gameId(state.getGameId().toString())
                .whitePlayerId(state.getWhiteId().toString())
                .blackPlayerId(state.getBlackId().toString())
                .result(state.getResult())
                .finishReason(state.getFinishReason() != null ? state.getFinishReason() : FinishReason.ABORTED.name())
                .winnerId(state.getWinnerId() != null ? state.getWinnerId().toString() : null)
                .finishedAt(state.getFinishedAt() != null ? state.getFinishedAt().toString() : Instant.now().toString())
                .pgn(PgnBuilder.buildPgn(state))
                .rated(state.isRated())
                .timeControlType(state.getTimeControl().getType())
                .build();
        EventEnvelope<GameFinishedEvent> e = EventBuilder.envelope("GameFinished", PRODUCER, payload);
        publishWithHeaders(GAME_FINISHED, e);
    }

    private void publishWithHeaders(String subject, Object event) {
        try {
            if (jetStream == null) {
                log.debug("JetStream is not configured; skipping publish to {}", subject);
                return;
            }

            String json = objectMapper.writeValueAsString(event);

            String eventId = null;
            String correlationId = null;
            if (event instanceof EventEnvelope<?> ee) {
                eventId = ee.getEventId();
                correlationId = ee.getCorrelationId();
            }

            Headers headers = new Headers();
            if (eventId != null) {
                headers.put("Nats-Msg-Id", eventId);
            }
            if (correlationId != null) {
                headers.put("X-Correlation-Id", correlationId);
            }

            jetStream.publish(subject, headers, json.getBytes());
        } catch (Exception e) {
            log.error("Failed to publish event to subject={}", subject, e);
        }
    }
}
