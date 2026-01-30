package com.chess.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("type")
    private String timeControl;

    private Double rating;

    @JsonProperty("deviation")
    private Double ratingDeviation;

    private Double volatility;

    @JsonProperty("games_played")
    private Integer gamesPlayed;

    private Double peakRating;
    private Instant updatedAt;
}
