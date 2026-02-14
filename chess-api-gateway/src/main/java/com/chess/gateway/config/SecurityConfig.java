package com.chess.gateway.config;

import com.chess.gateway.filter.JwtAuthenticationGatewayFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationGatewayFilter jwtAuthFilter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/auth/**", "/v1/auth/**").permitAll()
                        .pathMatchers("/ws/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
