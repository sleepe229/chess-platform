package com.chess.analytics.service;

import com.chess.analytics.domain.GameFact;
import com.chess.analytics.domain.GameProgress;
import com.chess.analytics.repo.GameFactRepository;
import com.chess.analytics.repo.GameProgressRepository;
import com.chess.events.game.GameFinishedEvent;
import com.chess.events.game.GameStartedEvent;
import com.chess.events.game.MoveMadeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameFactsService {

    private final GameFactRepository gameFactRepository;
    private final GameProgressRepository gameProgressRepository;

    @Transactional
    public void onGameStarted(GameStartedEvent e) {
        UUID gameId = UUID.fromString(e.getGameId());
        if (gameProgressRepository.existsById(gameId)) {
            return;
        }
        GameProgress progress = GameProgress.builder()
                .gameId(gameId)
                .moveCount(0)
                .updatedAt(parseInstant(e.getStartedAt()).orElse(Instant.now()))
                .build();
        gameProgressRepository.save(progress);
        log.debug("Game progress started: gameId={}", gameId);
    }

    @Transactional
    public void onMoveMade(MoveMadeEvent e) {
        UUID gameId = UUID.fromString(e.getGameId());
        gameProgressRepository.findById(gameId).ifPresent(p -> {
            p.setMoveCount(e.getMoveNumber());
            p.setUpdatedAt(Instant.now());
            gameProgressRepository.save(p);
        });
    }

    @Transactional
    public void onGameFinished(GameFinishedEvent e) {
        UUID gameId = UUID.fromString(e.getGameId());
        Integer moveCount = gameProgressRepository.findById(gameId)
                .map(GameProgress::getMoveCount)
                .orElse(null);
        Instant finishedAt = parseInstant(e.getFinishedAt()).orElse(Instant.now());
        UUID winnerId = e.getWinnerId() != null && !e.getWinnerId().isBlank()
                ? UUID.fromString(e.getWinnerId()) : null;

        GameFact fact = GameFact.builder()
                .gameId(gameId)
                .whitePlayerId(UUID.fromString(e.getWhitePlayerId()))
                .blackPlayerId(UUID.fromString(e.getBlackPlayerId()))
                .result(e.getResult())
                .finishReason(e.getFinishReason())
                .winnerId(winnerId)
                .finishedAt(finishedAt)
                .pgn(e.getPgn())
                .rated(e.getRated() != null && e.getRated())
                .timeControlType(e.getTimeControlType())
                .moveCount(moveCount)
                .createdAt(Instant.now())
                .build();
        gameFactRepository.save(fact);
        gameProgressRepository.deleteById(gameId);
        log.debug("Game fact saved: gameId={}", gameId);
    }

    private static Optional<Instant> parseInstant(String s) {
        if (s == null || s.isBlank()) return Optional.empty();
        try {
            return Optional.of(Instant.parse(s));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
}
