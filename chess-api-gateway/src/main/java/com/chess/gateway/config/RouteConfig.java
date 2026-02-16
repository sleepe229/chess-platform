package com.chess.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Value("${AUTH_SERVICE_HOST:localhost}")
    private String authServiceHost;

    @Value("${AUTH_SERVICE_PORT:8081}")
    private int authServicePort;

    @Value("${USER_SERVICE_HOST:localhost}")
    private String userServiceHost;

    @Value("${USER_SERVICE_PORT:8082}")
    private int userServicePort;

    @Value("${MATCHMAKING_SERVICE_HOST:localhost}")
    private String matchmakingServiceHost;

    @Value("${MATCHMAKING_SERVICE_PORT:8083}")
    private int matchmakingServicePort;

    @Value("${GAME_SERVICE_HOST:localhost}")
    private String gameServiceHost;

    @Value("${GAME_SERVICE_PORT:8084}")
    private int gameServicePort;

    @Value("${WS_SERVICE_HOST:localhost}")
    private String wsServiceHost;

    @Value("${WS_SERVICE_PORT:8085}")
    private int wsServicePort;

    @Value("${ANALYTICS_SERVICE_HOST:localhost}")
    private String analyticsServiceHost;

    @Value("${ANALYTICS_SERVICE_PORT:8086}")
    private int analyticsServicePort;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service Routes - v1 API
                .route("auth-service-v1", r -> r
                        .path("/v1/auth/**")
                        .filters(f -> f.stripPrefix(1)) // Remove /v1 prefix
                        .uri(String.format("http://%s:%d", authServiceHost, authServicePort)))

                // Auth Service Routes - legacy (backward compatibility)
                .route("auth-service-legacy", r -> r
                        .path("/auth/**")
                        .uri(String.format("http://%s:%d", authServiceHost, authServicePort)))

                // OAuth2 callback (Google redirects to /login/oauth2/code/google)
                .route("auth-oauth2-callback", r -> r
                        .path("/login/oauth2/**")
                        .uri(String.format("http://%s:%d", authServiceHost, authServicePort)))

                // User Service Routes - v1 API
                .route("user-service-v1", r -> r
                        .path("/v1/users/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(String.format("http://%s:%d", userServiceHost, userServicePort)))

                // User Service Routes - legacy
                .route("user-service-legacy", r -> r
                        .path("/users/**")
                        .uri(String.format("http://%s:%d", userServiceHost, userServicePort)))

                // Matchmaking Service Routes - v1 API
                .route("matchmaking-service-v1", r -> r
                        .path("/v1/matchmaking/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(String.format("http://%s:%d", matchmakingServiceHost, matchmakingServicePort)))

                // Matchmaking Service Routes - legacy
                .route("matchmaking-service-legacy", r -> r
                        .path("/matchmaking/**")
                        .uri(String.format("http://%s:%d", matchmakingServiceHost, matchmakingServicePort)))

                // Game Service Routes - v1 API
                .route("game-service-v1", r -> r
                        .path("/v1/games/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(String.format("http://%s:%d", gameServiceHost, gameServicePort)))

                // Game Service Routes - legacy
                .route("game-service-legacy", r -> r
                        .path("/games/**")
                        .uri(String.format("http://%s:%d", gameServiceHost, gameServicePort)))

                // WebSocket Service Routes - v1 API
                .route("ws-service-v1", r -> r
                        .path("/v1/ws/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(String.format("http://%s:%d", wsServiceHost, wsServicePort)))

                // WebSocket Service Routes - legacy
                .route("ws-service-legacy", r -> r
                        .path("/ws/**")
                        .uri(String.format("http://%s:%d", wsServiceHost, wsServicePort)))

                // Analytics Service Routes - v1 API
                .route("analytics-service-v1", r -> r
                        .path("/v1/analysis/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(String.format("http://%s:%d", analyticsServiceHost, analyticsServicePort)))

                // Analytics Service Routes - legacy
                .route("analytics-service-legacy", r -> r
                        .path("/analysis/**")
                        .uri(String.format("http://%s:%d", analyticsServiceHost, analyticsServicePort)))

                .build();
    }
}
