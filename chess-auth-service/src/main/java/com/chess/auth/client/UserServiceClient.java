package com.chess.auth.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Calls user-service to create profile synchronously on registration
 * so GET /users/me works immediately after login.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "user-service.sync-create.enabled", havingValue = "true", matchIfMissing = true)
public class UserServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public UserServiceClient(
            RestTemplate restTemplate,
            @Value("${user-service.base-url:http://localhost:8082}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    /**
     * Create user profile in user-service. Idempotent: if user already exists, no error.
     */
    public void createUserIfAbsent(UUID userId, String email) {
        String url = baseUrl + "/internal/users";
        try {
            restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(Map.of("userId", userId.toString(), "email", email),
                            jsonHeaders()),
                    String.class);
            log.debug("Created user profile in user-service: {}", userId);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 409) {
                log.debug("User already exists in user-service: {}, response={}", userId, e.getResponseBodyAsString());
                return;
            }
            log.warn("Failed to create user profile in user-service for userId={}: {} {}", userId, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to create user profile in user-service for userId={}: {}", userId, e.getMessage());
            // Don't fail registration; NATS event will eventually create the profile
        }
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
}
