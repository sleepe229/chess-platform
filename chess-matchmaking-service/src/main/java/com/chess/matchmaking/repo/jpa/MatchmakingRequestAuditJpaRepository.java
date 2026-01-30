package com.chess.matchmaking.repo.jpa;

import com.chess.matchmaking.repo.entity.MatchmakingRequestAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MatchmakingRequestAuditJpaRepository extends JpaRepository<MatchmakingRequestAudit, UUID> {
}

