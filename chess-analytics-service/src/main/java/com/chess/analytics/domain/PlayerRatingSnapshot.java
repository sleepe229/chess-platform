package com.chess.analytics.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "player_rating_snapshots", schema = "chess_analytics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerRatingSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "time_control", nullable = false, length = 32)
    private String timeControl;

    @Column(nullable = false)
    private Double rating;

    @Column(name = "game_id")
    private UUID gameId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
