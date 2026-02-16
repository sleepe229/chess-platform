package com.chess.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class PlayerStatsResponse {
    private long totalGames;
    private long wins;
    private long draws;
    private long losses;
    /** timeControl -> rating (latest snapshot) */
    private Map<String, Double> ratingsByTimeControl;
}
