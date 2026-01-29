package com.chess.event;

public record MatchFoundEvent(
                String player1Id,
                Integer player1Rating,
                String player2Id,
                Integer player2Rating) {
}
