package com.chess.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private UUID id;
    private String username;
    private String email;
    private String avatarUrl;
    private String bio;
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
