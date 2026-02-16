package com.chess.analytics.repo;

import com.chess.analytics.domain.AnalysisJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, UUID> {

    List<AnalysisJob> findByGameIdOrderByCreatedAtDesc(UUID gameId, Pageable pageable);

    Page<AnalysisJob> findByRequestedByOrderByCreatedAtDesc(UUID requestedBy, Pageable pageable);

    Page<AnalysisJob> findByGameId(UUID gameId, Pageable pageable);

    Page<AnalysisJob> findByGameIdAndStatus(UUID gameId, String status, Pageable pageable);

    Page<AnalysisJob> findByRequestedByAndStatusOrderByCreatedAtDesc(UUID requestedBy, String status, Pageable pageable);

    Optional<AnalysisJob> findFirstByGameIdAndStatus(UUID gameId, String status);
}
