package com.chess.game.controller;

import com.chess.common.security.SecurityUser;
import com.chess.game.dto.GameClocksResponse;
import com.chess.game.dto.GameMoveResponse;
import com.chess.game.dto.GameStateResponse;
import com.chess.game.dto.MoveRequest;
import com.chess.game.service.GameService;
import com.chess.game.state.GameMove;
import com.chess.game.state.GameState;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import com.github.bhlangonijr.chesslib.Board;

@Slf4j
@RestController
@RequestMapping({"/v1/games", "/games"})
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @GetMapping("/{id}/state")
    public ResponseEntity<GameStateResponse> state(@PathVariable UUID id) {
        GameState state = gameService.getState(id);
        return ResponseEntity.ok(toResponse(state));
    }

    @PostMapping("/{id}/move")
    public ResponseEntity<GameStateResponse> move(
            @PathVariable UUID id,
            @Valid @RequestBody MoveRequest request,
            @AuthenticationPrincipal SecurityUser user
    ) {
        UUID clientMoveId = request.getClientMoveId() != null && !request.getClientMoveId().isBlank()
                ? UUID.fromString(request.getClientMoveId())
                : null;
        GameState state = gameService.applyMove(id, user.getUserId(), request.getUci(), clientMoveId);
        return ResponseEntity.ok(toResponse(state));
    }

    @PostMapping("/{id}/resign")
    public ResponseEntity<GameStateResponse> resign(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser user
    ) {
        GameState state = gameService.resign(id, user.getUserId());
        return ResponseEntity.ok(toResponse(state));
    }

    @PostMapping("/{id}/offer-draw")
    public ResponseEntity<GameStateResponse> offerDraw(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser user
    ) {
        GameState state = gameService.offerDraw(id, user.getUserId());
        return ResponseEntity.ok(toResponse(state));
    }

    @PostMapping("/{id}/accept-draw")
    public ResponseEntity<GameStateResponse> acceptDraw(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser user
    ) {
        GameState state = gameService.acceptDraw(id, user.getUserId());
        return ResponseEntity.ok(toResponse(state));
    }

    private static GameStateResponse toResponse(GameState state) {
        List<GameMoveResponse> moves = state.getMoves() == null ? List.of() : state.getMoves().stream()
                .map(GameController::toMove)
                .toList();
        String sideToMove = "UNKNOWN";
        try {
            Board board = new Board();
            if (state.getFen() != null && !state.getFen().isBlank()) {
                board.loadFromFen(state.getFen());
                sideToMove = board.getSideToMove().name();
            }
        } catch (Exception ignored) {
        }

        return GameStateResponse.builder()
                .gameId(state.getGameId())
                .whiteId(state.getWhiteId())
                .blackId(state.getBlackId())
                .fen(state.getFen())
                .moves(moves)
                .clocks(state.getClocks() != null ? GameClocksResponse.builder()
                        .whiteMs(state.getClocks().getWhiteMs())
                        .blackMs(state.getClocks().getBlackMs())
                        .build() : null)
                .status(state.getStatus() != null ? state.getStatus().name() : null)
                .sideToMove(sideToMove)
                .result(state.getResult())
                .finishReason(state.getFinishReason())
                .build();
    }

    private static GameMoveResponse toMove(GameMove m) {
        return GameMoveResponse.builder()
                .ply(m.getPly())
                .uci(m.getUci())
                .san(m.getSan())
                .fenAfter(m.getFenAfter())
                .playedAt(m.getPlayedAt())
                .byUserId(m.getByUserId())
                .build();
    }
}

