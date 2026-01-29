package com.chess.user.repo;

import com.chess.user.domain.RatingHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RatingHistoryRepository extends JpaRepository<RatingHistory, UUID> {

    Page<RatingHistory> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<RatingHistory> findByUserIdAndTimeControlOrderByCreatedAtDesc(UUID userId, String timeControl);

    Page<RatingHistory> findByUserIdAndTimeControlOrderByCreatedAtDesc(
            UUID userId, String timeControl, Pageable pageable);
}
