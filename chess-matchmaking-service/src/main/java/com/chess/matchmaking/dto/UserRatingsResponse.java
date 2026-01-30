package com.chess.matchmaking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UserRatingsResponse {
    private UUID userId;
    private List<RatingDto> ratings;

    @Data
    public static class RatingDto {
        @JsonProperty("type")
        private String timeControl;

        private Double rating;

        @JsonProperty("deviation")
        private Double ratingDeviation;
    }
}

