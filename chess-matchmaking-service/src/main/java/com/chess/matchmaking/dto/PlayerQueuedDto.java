package com.chess.matchmaking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerQueuedDto {

    private String userId;
    private String timeControl;
    private Double rating;
    private Double ratingDeviation;
    private Boolean rated;
}
