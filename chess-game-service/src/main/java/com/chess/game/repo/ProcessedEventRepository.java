package com.chess.game.repo;

import com.chess.game.repo.entity.ProcessedEventEntity;
import com.chess.game.repo.entity.ProcessedEventId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, ProcessedEventId> {
}

