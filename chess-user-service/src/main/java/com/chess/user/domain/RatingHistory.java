package com.chess.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rating_history", indexes = {
        @Index(name = "idx_rating_history_user_time", columnList = "user_id, time_control, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "time_control", nullable = false, length = 50)
    private String timeControl;

    @Column(name = "old_rating", nullable = false)
    private Double oldRating;

    @Column(name = "new_rating", nullable = false)
    private Double newRating;

    @Column(name = "rating_change", nullable = false)
    private Double ratingChange;

    @Column(name = "old_rd", nullable = false)
    private Double oldRd;

    @Column(name = "new_rd", nullable = false)
    private Double newRd;

    @Column(name = "game_id")
    private UUID gameId;

    @Column(name = "opponent_id")
    private UUID opponentId;

    @Column(name = "opponent_rating")
    private Double opponentRating;

    @Column(length = 20)
    private String result;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;
}
