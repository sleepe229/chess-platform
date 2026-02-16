package com.chess.analytics.service;

import com.chess.analytics.domain.AnalysisJob;
import com.chess.analytics.domain.GameFact;
import com.chess.analytics.domain.PlayerRatingSnapshot;
import com.chess.analytics.dto.*;
import com.chess.analytics.repo.AnalysisJobRepository;
import com.chess.analytics.repo.GameFactRepository;
import com.chess.analytics.repo.PlayerRatingSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalysisQueryService {

    private final GameFactRepository gameFactRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final PlayerRatingSnapshotRepository playerRatingSnapshotRepository;

    @Transactional(readOnly = true)
    public Page<GameFactResponse> findGames(UUID playerId, Instant from, Instant to, Pageable pageable) {
        Page<GameFact> page;
        if (playerId != null) {
            page = gameFactRepository.findByWhitePlayerIdOrBlackPlayerIdOrderByFinishedAtDesc(playerId, playerId, pageable);
        } else if (from != null && to != null) {
            page = gameFactRepository.findByFinishedAtBetweenOrderByFinishedAtDesc(from, to, pageable);
        } else {
            page = gameFactRepository.findAll(org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(), pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "finishedAt")));
        }
        return page.map(this::toGameFactResponse);
    }

    @Transactional(readOnly = true)
    public Optional<GameFactResponse> findGameById(UUID gameId) {
        return gameFactRepository.findById(gameId).map(this::toGameFactResponse);
    }

    @Transactional(readOnly = true)
    public Page<AnalysisJobResponse> findJobs(UUID gameId, UUID requestedBy, String status, Pageable pageable) {
        Page<AnalysisJob> page;
        if (gameId != null) {
            page = (status != null && !status.isBlank())
                    ? analysisJobRepository.findByGameIdAndStatus(gameId, status, pageable)
                    : analysisJobRepository.findByGameId(gameId, pageable);
        } else if (requestedBy != null) {
            page = (status != null && !status.isBlank())
                    ? analysisJobRepository.findByRequestedByAndStatusOrderByCreatedAtDesc(requestedBy, status, pageable)
                    : analysisJobRepository.findByRequestedByOrderByCreatedAtDesc(requestedBy, pageable);
        } else {
            page = analysisJobRepository.findAll(org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(), pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
        }
        return page.map(this::toJobResponse);
    }

    @Transactional(readOnly = true)
    public Optional<AnalysisJobResponse> findJobById(UUID analysisJobId) {
        return analysisJobRepository.findById(analysisJobId).map(this::toJobResponse);
    }

    @Transactional(readOnly = true)
    public PlayerStatsResponse getPlayerStats(UUID playerId) {
        List<GameFact> games = gameFactRepository.findByWhitePlayerIdOrBlackPlayerIdOrderByFinishedAtDesc(
                playerId, playerId, Pageable.unpaged()).getContent();
        long wins = 0, draws = 0, losses = 0;
        for (GameFact g : games) {
            if (g.getWinnerId() == null) draws++;
            else if (g.getWinnerId().equals(playerId)) wins++;
            else losses++;
        }
        List<PlayerRatingSnapshot> snapshots = playerRatingSnapshotRepository.findByPlayerIdOrderByUpdatedAtDesc(
                playerId, Pageable.ofSize(20));
        Map<String, Double> ratingsByTimeControl = new LinkedHashMap<>();
        for (PlayerRatingSnapshot s : snapshots) {
            ratingsByTimeControl.putIfAbsent(s.getTimeControl(), s.getRating());
        }
        return PlayerStatsResponse.builder()
                .totalGames(games.size())
                .wins(wins)
                .draws(draws)
                .losses(losses)
                .ratingsByTimeControl(ratingsByTimeControl)
                .build();
    }

    private GameFactResponse toGameFactResponse(GameFact g) {
        return GameFactResponse.builder()
                .gameId(g.getGameId())
                .whitePlayerId(g.getWhitePlayerId())
                .blackPlayerId(g.getBlackPlayerId())
                .result(g.getResult())
                .finishReason(g.getFinishReason())
                .winnerId(g.getWinnerId())
                .finishedAt(g.getFinishedAt())
                .pgn(g.getPgn())
                .rated(g.getRated())
                .timeControlType(g.getTimeControlType())
                .moveCount(g.getMoveCount())
                .build();
    }

    private AnalysisJobResponse toJobResponse(AnalysisJob j) {
        return AnalysisJobResponse.builder()
                .analysisJobId(j.getAnalysisJobId())
                .gameId(j.getGameId())
                .requestedBy(j.getRequestedBy())
                .status(j.getStatus())
                .totalMoves(j.getTotalMoves())
                .accuracyWhite(j.getAccuracyWhite())
                .accuracyBlack(j.getAccuracyBlack())
                .errorMessage(j.getErrorMessage())
                .completedAt(j.getCompletedAt())
                .createdAt(j.getCreatedAt())
                .build();
    }
}
