package com.chess.matchmaking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindMatchRequest {

    private String userId;
    private String timeControl;
    private Double rating;
    private int range;
}
