package com.chess.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service Routes
                .route("auth-service", r -> r
                        .path("/auth/**")
                        .uri("http://chess-auth-service:8081"))

                // User Service Routes
                .route("user-service", r -> r
                        .path("/users/**")
                        .uri("http://chess-user-service:8082"))

                // Matchmaking Service Routes
                .route("matchmaking-service", r -> r
                        .path("/matchmaking/**")
                        .uri("http://chess-matchmaking-service:8083"))

                // Game Service Routes
                .route("game-service", r -> r
                        .path("/games/**")
                        .uri("http://chess-game-service:8084"))

                // Analytics Service Routes
                .route("analytics-service", r -> r
                        .path("/analysis/**")
                        .uri("http://chess-analytics-service:8086"))

                .build();
    }
}
