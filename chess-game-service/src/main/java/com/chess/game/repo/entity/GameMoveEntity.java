package com.chess.game.repo.entity;

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
@Table(name = "game_moves")
@IdClass(GameMoveId.class)
public class GameMoveEntity {

    @Id
    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Id
    @Column(name = "ply", nullable = false)
    private Integer ply;

    @Column(name = "uci", nullable = false, length = 8)
    private String uci;

    @Column(name = "san", length = 16)
    private String san;

    @Column(name = "fen_after", nullable = false, columnDefinition = "text")
    private String fenAfter;

    @Column(name = "played_at", nullable = false)
    private Instant playedAt;

    @Column(name = "by_user_id", nullable = false)
    private UUID byUserId;

    @Column(name = "white_ms_after")
    private Long whiteMsAfter;

    @Column(name = "black_ms_after")
    private Long blackMsAfter;

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
        GameMoveEntity that = (GameMoveEntity) o;
        return getGameId() != null && Objects.equals(getGameId(), that.getGameId())
                && getPly() != null && Objects.equals(getPly(), that.getPly());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(gameId, ply);
    }
}

