package com.chess.analytics.service;

import com.chess.analytics.domain.PlayerRatingSnapshot;
import com.chess.analytics.repo.PlayerRatingSnapshotRepository;
import com.chess.events.users.RatingUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerRatingService {

    private final PlayerRatingSnapshotRepository repository;

    @Transactional
    public void onRatingUpdated(RatingUpdatedEvent e) {
        UUID gameId = e.getGameId() != null && !e.getGameId().isBlank() ? UUID.fromString(e.getGameId()) : null;
        PlayerRatingSnapshot snapshot = PlayerRatingSnapshot.builder()
                .playerId(UUID.fromString(e.getUserId()))
                .timeControl(e.getTimeControl())
                .rating(e.getNewRating())
                .gameId(gameId)
                .updatedAt(Instant.now())
                .build();
        repository.save(snapshot);
        log.debug("Rating snapshot saved: playerId={}, timeControl={}, rating={}", e.getUserId(), e.getTimeControl(), e.getNewRating());
    }
}
