package com.chess.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(length = 500)
    private String bio;

    @Column(length = 100)
    private String country;

    @Column(name = "total_games", nullable = false)
    @Builder.Default
    private Integer totalGames = 0;

    @Column(name = "total_wins", nullable = false)
    @Builder.Default
    private Integer totalWins = 0;

    @Column(name = "total_losses", nullable = false)
    @Builder.Default
    private Integer totalLosses = 0;

    @Column(name = "total_draws", nullable = false)
    @Builder.Default
    private Integer totalDraws = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private Instant updatedAt;

    public void incrementGames() {
        this.totalGames++;
    }

    public void incrementWins() {
        this.totalWins++;
    }

    public void incrementLosses() {
        this.totalLosses++;
    }

    public void incrementDraws() {
        this.totalDraws++;
    }

    public double getWinRate() {
        if (totalGames == 0) return 0.0;
        return (double) totalWins / totalGames * 100;
    }
}
