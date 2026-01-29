package com.chess.matchmaking.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueuedPlayer {
    private String playerId;
    private Integer rating;
    private Long queueTime; // timestamp when player joined queue
}
