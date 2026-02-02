package com.chess.user.repo;

import com.chess.user.repo.entity.ProcessedEventEntity;
import com.chess.user.repo.entity.ProcessedEventId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, ProcessedEventId> {
}

