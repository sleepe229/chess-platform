package com.chess.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingHistoryResponse {

    private UUID id;
    private String timeControl;
    private Double oldRating;
    private Double newRating;
    private Double ratingChange;
    private UUID gameId;
    private UUID opponentId;
    private Double opponentRating;
    private String result;
    private Instant createdAt;
}
