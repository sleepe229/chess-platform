package com.chess.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingDto {

    private String timeControl;
    private Double rating;
    private Double ratingDeviation;
    private Integer gamesPlayed;
    private Double peakRating;
    private Instant updatedAt;
}
