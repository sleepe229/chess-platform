package com.chess.user.repo;

import com.chess.user.domain.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {

    Optional<Rating> findByUserIdAndTimeControl(UUID userId, String timeControl);

    List<Rating> findByUserId(UUID userId);

    List<Rating> findByTimeControlOrderByRatingDesc(String timeControl);
}
