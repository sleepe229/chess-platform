package com.chess.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public profile DTO for GET /users/{userId} — excludes email and bio.
 * Use UserProfileResponse for GET /users/me (authenticated user).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfilePublicResponse {

    private UUID id;
    private String username;
    private String avatarUrl;
    private String country;

    private Integer totalGames;
    private Integer totalWins;
    private Integer totalLosses;
    private Integer totalDraws;
    private Double winRate;

    private List<RatingDto> ratings;

    private Instant createdAt;
    private Instant updatedAt;
}
