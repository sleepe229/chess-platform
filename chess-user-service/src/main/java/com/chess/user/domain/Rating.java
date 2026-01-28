package com.chess.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ratings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "time_control"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "time_control", nullable = false, length = 50)
    private String timeControl;

    @Column(nullable = false)
    @Builder.Default
    private Double rating = 1500.0;

    @Column(name = "rating_deviation", nullable = false)
    @Builder.Default
    private Double ratingDeviation = 350.0;

    @Column(nullable = false)
    @Builder.Default
    private Double volatility = 0.06;

    @Column(name = "games_played")
    @Builder.Default
    private Integer gamesPlayed = 0;

    @Column(name = "peak_rating")
    @Builder.Default
    private Double peakRating = 1500.0;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private Instant updatedAt;

    public void updateRating(Double newRating, Double newRd, Double newVolatility) {
        this.rating = newRating;
        this.ratingDeviation = newRd;
        this.volatility = newVolatility;
        this.gamesPlayed++;

        if (newRating > this.peakRating) {
            this.peakRating = newRating;
        }
    }
}
