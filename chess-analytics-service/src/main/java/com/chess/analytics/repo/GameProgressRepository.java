package com.chess.analytics.repo;

import com.chess.analytics.domain.GameProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GameProgressRepository extends JpaRepository<GameProgress, UUID> {
}
