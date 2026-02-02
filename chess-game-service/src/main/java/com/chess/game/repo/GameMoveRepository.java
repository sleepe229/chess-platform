package com.chess.game.repo;

import com.chess.game.repo.entity.GameMoveEntity;
import com.chess.game.repo.entity.GameMoveId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GameMoveRepository extends JpaRepository<GameMoveEntity, GameMoveId> {
    List<GameMoveEntity> findByGameIdOrderByPlyAsc(UUID gameId);
}

