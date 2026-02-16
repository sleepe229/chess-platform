package com.chess.analytics.repo;

import com.chess.analytics.domain.GameFact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface GameFactRepository extends JpaRepository<GameFact, UUID> {

    Page<GameFact> findByWhitePlayerIdOrBlackPlayerIdOrderByFinishedAtDesc(
            UUID whitePlayerId, UUID blackPlayerId, Pageable pageable);

    Page<GameFact> findByFinishedAtBetweenOrderByFinishedAtDesc(
            Instant from, Instant to, Pageable pageable);
}
