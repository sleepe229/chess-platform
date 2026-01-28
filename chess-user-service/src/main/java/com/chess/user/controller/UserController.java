package com.chess.user.controller;

import com.chess.user.dto.RatingHistoryResponse;
import com.chess.user.dto.RatingsResponse;
import com.chess.user.dto.UpdateProfileRequest;
import com.chess.user.dto.UserProfilePublicResponse;
import com.chess.user.dto.UserProfileResponse;
import com.chess.common.security.SecurityUser;
import com.chess.user.service.RatingHistoryService;
import com.chess.user.service.RatingService;
import com.chess.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserService userService;
    private final RatingService ratingService;
    private final RatingHistoryService ratingHistoryService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfilePublicResponse> getUserProfile(@PathVariable UUID userId) {
        log.info("GET /users/{}", userId);
        UserProfilePublicResponse response = userService.getUserProfilePublic(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal SecurityUser user) {
        UUID userId = user.getUserId();
        log.info("GET /users/me - userId: {}", userId);
        UserProfileResponse response = userService.getUserProfile(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal SecurityUser user,
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = user.getUserId();
        log.info("PUT /users/me - userId: {}", userId);
        UserProfileResponse response = userService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/ratings")
    public ResponseEntity<RatingsResponse> getUserRatings(@PathVariable UUID userId) {
        log.info("GET /users/{}/ratings", userId);
        RatingsResponse response = RatingsResponse.builder()
                .userId(userId)
                .ratings(ratingService.getUserRatings(userId))
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/rating-history")
    public ResponseEntity<Page<RatingHistoryResponse>> getRatingHistory(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        int safePage = Math.max(0, page);
        log.info("GET /users/{}/rating-history - page: {}, size: {}", userId, safePage, safeSize);
        Page<RatingHistoryResponse> response = ratingHistoryService.getRatingHistory(userId, safePage, safeSize);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/rating-history/{timeControl}")
    public ResponseEntity<Page<RatingHistoryResponse>> getRatingHistoryByTimeControl(
            @PathVariable UUID userId,
            @PathVariable String timeControl,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        int safePage = Math.max(0, page);
        log.info("GET /users/{}/rating-history/{} - page: {}, size: {}",
                userId, timeControl, safePage, safeSize);
        Page<RatingHistoryResponse> response = ratingHistoryService
                .getRatingHistoryByTimeControl(userId, timeControl, safePage, safeSize);
        return ResponseEntity.ok(response);
    }
}
