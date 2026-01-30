package com.chess.matchmaking.controller;

import com.chess.common.security.SecurityUser;
import com.chess.matchmaking.dto.JoinMatchmakingRequest;
import com.chess.matchmaking.dto.JoinMatchmakingResponse;
import com.chess.matchmaking.dto.LeaveMatchmakingRequest;
import com.chess.matchmaking.dto.MatchmakingStatus;
import com.chess.matchmaking.dto.MatchmakingStatusResponse;
import com.chess.matchmaking.service.MatchmakingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/v1/matchmaking", "/matchmaking"})
@RequiredArgsConstructor
public class MatchmakingController {

    private final MatchmakingService matchmakingService;

    @PostMapping("/join")
    public ResponseEntity<JoinMatchmakingResponse> join(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Request-Id", required = false) String requestIdHeader,
            @Valid @RequestBody JoinMatchmakingRequest request,
            @AuthenticationPrincipal SecurityUser user
    ) {
        UUID userId = user.getUserId();
        log.info("POST /matchmaking/join - userId: {}, baseSeconds: {}, incrementSeconds: {}, rated: {}",
                userId, request.getBaseSeconds(), request.getIncrementSeconds(), request.getRated());

        String requestId = matchmakingService.join(
                userId,
                request.getBaseSeconds(),
                request.getIncrementSeconds(),
                request.getRated(),
                idempotencyKey,
                requestIdHeader
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new JoinMatchmakingResponse(requestId, MatchmakingStatus.QUEUED.name()));
    }

    @PostMapping("/leave")
    public ResponseEntity<Void> leave(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Request-Id", required = false) String requestIdHeader,
            @Valid @RequestBody LeaveMatchmakingRequest request,
            @AuthenticationPrincipal SecurityUser user
    ) {
        UUID userId = user.getUserId();
        log.info("POST /matchmaking/leave - userId: {}, requestId: {}", userId, request.getRequestId());
        matchmakingService.leave(userId, request.getRequestId(), idempotencyKey, requestIdHeader);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status/{requestId}")
    public ResponseEntity<MatchmakingStatusResponse> status(
            @PathVariable String requestId,
            @AuthenticationPrincipal SecurityUser user
    ) {
        UUID userId = user.getUserId();
        log.info("GET /matchmaking/status/{} - userId: {}", requestId, userId);
        return ResponseEntity.ok(matchmakingService.status(userId, requestId));
    }
}

