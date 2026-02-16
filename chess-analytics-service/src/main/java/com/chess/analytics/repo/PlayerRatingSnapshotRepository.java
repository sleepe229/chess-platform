package com.chess.analytics.repo;

import com.chess.analytics.domain.PlayerRatingSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRatingSnapshotRepository extends JpaRepository<PlayerRatingSnapshot, Long> {

    Optional<PlayerRatingSnapshot> findFirstByPlayerIdAndTimeControlOrderByUpdatedAtDesc(UUID playerId, String timeControl);

    List<PlayerRatingSnapshot> findByPlayerIdOrderByUpdatedAtDesc(UUID playerId, org.springframework.data.domain.Pageable pageable);
}
