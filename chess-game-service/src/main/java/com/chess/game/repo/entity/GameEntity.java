package com.chess.game.repo.entity;

import com.chess.game.domain.GameStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "games")
public class GameEntity {

    @Id
    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "white_id", nullable = false)
    private UUID whiteId;

    @Column(name = "black_id", nullable = false)
    private UUID blackId;

    @Column(name = "time_control_type", nullable = false, length = 16)
    private String timeControlType;

    @Column(name = "base_seconds", nullable = false)
    private int baseSeconds;

    @Column(name = "increment_seconds", nullable = false)
    private int incrementSeconds;

    @Column(name = "rated", nullable = false)
    private boolean rated;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GameStatus status;

    @Column(name = "result", length = 8)
    private String result;

    @Column(name = "finish_reason", length = 32)
    private String finishReason;

    @Column(name = "pgn", columnDefinition = "text")
    private String pgn;

    @Column(name = "current_fen", columnDefinition = "text")
    private String currentFen;

    @Column(name = "white_ms")
    private Long whiteMs;

    @Column(name = "black_ms")
    private Long blackMs;

    @Column(name = "last_move_at")
    private Instant lastMoveAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        Class<?> objectEffectiveClass = o instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != objectEffectiveClass) {
            return false;
        }
        GameEntity that = (GameEntity) o;
        return getGameId() != null && Objects.equals(getGameId(), that.getGameId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}

