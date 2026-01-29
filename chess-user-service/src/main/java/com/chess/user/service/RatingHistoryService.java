package com.chess.user.service;

import com.chess.user.domain.RatingHistory;
import com.chess.user.dto.RatingHistoryResponse;
import com.chess.user.repo.RatingHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingHistoryService {

    private final RatingHistoryRepository ratingHistoryRepository;

    @Transactional(readOnly = true)
    public Page<RatingHistoryResponse> getRatingHistory(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RatingHistory> historyPage = ratingHistoryRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return historyPage.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<RatingHistoryResponse> getRatingHistoryByTimeControl(
            UUID userId, String timeControl, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<RatingHistory> historyPage = ratingHistoryRepository
                .findByUserIdAndTimeControlOrderByCreatedAtDesc(userId, timeControl, pageable);

        return historyPage.map(this::toResponse);
    }

    private RatingHistoryResponse toResponse(RatingHistory history) {
        return RatingHistoryResponse.builder()
                .id(history.getId())
                .timeControl(history.getTimeControl())
                .oldRating(history.getOldRating())
                .newRating(history.getNewRating())
                .ratingChange(history.getRatingChange())
                .gameId(history.getGameId())
                .opponentId(history.getOpponentId())
                .opponentRating(history.getOpponentRating())
                .result(history.getResult())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
