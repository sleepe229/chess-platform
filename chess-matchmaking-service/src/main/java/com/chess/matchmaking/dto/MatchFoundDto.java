package com.chess.matchmaking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchFoundDto {

    private String matchId;
    private String whitePlayerId;
    private String blackPlayerId;
    private String timeControl;
    private int initialTimeSeconds;
    private int incrementSeconds;
}
