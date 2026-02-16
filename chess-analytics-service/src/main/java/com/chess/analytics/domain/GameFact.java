package com.chess.analytics.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_facts", schema = "chess_analytics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameFact {

    @Id
    @Column(name = "game_id")
    private UUID gameId;

    @Column(name = "white_player_id", nullable = false)
    private UUID whitePlayerId;

    @Column(name = "black_player_id", nullable = false)
    private UUID blackPlayerId;

    @Column(nullable = false, length = 32)
    private String result;

    @Column(name = "finish_reason", nullable = false, length = 64)
    private String finishReason;

    @Column(name = "winner_id")
    private UUID winnerId;

    @Column(name = "finished_at", nullable = false)
    private Instant finishedAt;

    @Column(columnDefinition = "TEXT")
    private String pgn;

    @Column(nullable = false)
    private Boolean rated;

    @Column(name = "time_control_type", nullable = false, length = 32)
    private String timeControlType;

    @Column(name = "move_count")
    private Integer moveCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
