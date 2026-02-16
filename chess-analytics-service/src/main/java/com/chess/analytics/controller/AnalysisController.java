package com.chess.analytics.controller;

import com.chess.analytics.dto.*;
import com.chess.analytics.service.AnalysisJobService;
import com.chess.analytics.service.AnalysisQueryService;
import com.chess.common.exception.ForbiddenException;
import com.chess.common.exception.NotFoundException;
import com.chess.common.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisQueryService queryService;
    private final AnalysisJobService analysisJobService;

    @GetMapping("/games")
    public ResponseEntity<Page<GameFactResponse>> listGames(
            @RequestParam(required = false) UUID playerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal SecurityUser user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (playerId != null && !playerId.equals(user.getUserId())) {
            throw new ForbiddenException("Can only list own games");
        }
        UUID effectivePlayerId = playerId != null ? playerId : user.getUserId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<GameFactResponse> result = queryService.findGames(effectivePlayerId, from, to, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/games/{gameId}")
    public ResponseEntity<GameFactResponse> getGame(
            @PathVariable UUID gameId,
            @AuthenticationPrincipal SecurityUser user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        GameFactResponse game = queryService.findGameById(gameId)
                .orElseThrow(() -> new NotFoundException("Game not found"));
        if (!user.getUserId().equals(game.getWhitePlayerId()) && !user.getUserId().equals(game.getBlackPlayerId())) {
            throw new ForbiddenException("Not a participant of this game");
        }
        return ResponseEntity.ok(game);
    }

    @GetMapping("/games/{gameId}/summary")
    public ResponseEntity<GameFactResponse> getGameSummary(
            @PathVariable UUID gameId,
            @AuthenticationPrincipal SecurityUser user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        GameFactResponse game = queryService.findGameById(gameId)
                .orElseThrow(() -> new NotFoundException("Game not found"));
        if (!user.getUserId().equals(game.getWhitePlayerId()) && !user.getUserId().equals(game.getBlackPlayerId())) {
            throw new ForbiddenException("Not a participant of this game");
        }
        return ResponseEntity.ok(game);
    }

    @GetMapping("/jobs")
    public ResponseEntity<Page<AnalysisJobResponse>> listJobs(
            @RequestParam(required = false) UUID gameId,
            @RequestParam(required = false) UUID requestedBy,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<AnalysisJobResponse> result = queryService.findJobs(gameId, requestedBy, status, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/jobs/{analysisJobId}")
    public ResponseEntity<AnalysisJobResponse> getJob(@PathVariable UUID analysisJobId) {
        return queryService.findJobById(analysisJobId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/games/{gameId}/analyze")
    public ResponseEntity<AnalysisJobResponse> requestAnalysis(
            @PathVariable UUID gameId,
            @RequestBody(required = false) AnalyzeRequest body,
            @AuthenticationPrincipal SecurityUser user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var job = analysisJobService.requestAnalysis(gameId, user.getUserId());
        AnalysisJobResponse response = AnalysisJobResponse.builder()
                .analysisJobId(job.getAnalysisJobId())
                .gameId(job.getGameId())
                .requestedBy(job.getRequestedBy())
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                .build();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/players/{playerId}/stats")
    public ResponseEntity<PlayerStatsResponse> getPlayerStats(
            @PathVariable UUID playerId,
            @AuthenticationPrincipal SecurityUser user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!playerId.equals(user.getUserId())) {
            throw new ForbiddenException("Can only view own stats");
        }
        PlayerStatsResponse stats = queryService.getPlayerStats(playerId);
        return ResponseEntity.ok(stats);
    }
}
