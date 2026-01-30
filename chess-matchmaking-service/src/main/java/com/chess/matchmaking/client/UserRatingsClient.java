package com.chess.matchmaking.client;

import com.chess.common.exception.NotFoundException;
import com.chess.matchmaking.dto.UserRatingsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRatingsClient {

    private final RestClient restClient;

    public record RatingInfo(String timeControlType, double rating, double ratingDeviation) {
    }

    public RatingInfo fetchRating(UUID userId, String timeControlType) {
        UserRatingsResponse response = restClient.get()
                .uri("/users/{userId}/ratings", userId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new RuntimeException("User-service returned status " + res.getStatusCode());
                })
                .body(UserRatingsResponse.class);

        if (response == null || response.getRatings() == null) {
            throw new RuntimeException("User-service returned empty ratings response");
        }

        return response.getRatings().stream()
                .filter(r -> timeControlType.equalsIgnoreCase(r.getTimeControl()))
                .findFirst()
                .map(r -> new RatingInfo(
                        timeControlType,
                        r.getRating() != null ? r.getRating() : 0.0,
                        r.getRatingDeviation() != null ? r.getRatingDeviation() : 0.0
                ))
                .orElseThrow(() -> new NotFoundException("Rating for timeControlType=" + timeControlType + " not found"));
    }

    @Configuration
    public static class Config {
        @Bean
        RestClient restClient(
                @Value("${user-service.base-url:http://localhost:8082}") String baseUrl
        ) {
            return RestClient.builder()
                    .baseUrl(baseUrl)
                    .build();
        }
    }
}

