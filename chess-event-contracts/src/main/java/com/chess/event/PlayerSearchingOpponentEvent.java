package com.chess.event;

public record PlayerSearchingOpponentEvent(
                String playerId,
                Integer rating) {
}
