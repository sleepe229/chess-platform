package com.chess.analytics.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * In-memory progress for a game until GameFinished: accumulates move_count.
 */
@Entity
@Table(name = "game_progress", schema = "chess_analytics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameProgress {

    @Id
    @Column(name = "game_id")
    private UUID gameId;

    @Column(name = "move_count", nullable = false)
    private int moveCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
