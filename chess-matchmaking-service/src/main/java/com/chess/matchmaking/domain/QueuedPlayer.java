package com.chess.matchmaking.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueuedPlayer {

    private String userId;
    private String timeControl;
    private Double rating;
    private Double ratingDeviation;
    private Long queueTime;
}
