package com.chess.gateway.filter;

import com.chess.common.dto.ErrorResponse;
import com.chess.common.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class JwtAuthenticationGatewayFilter implements WebFilter {

    private final JwtTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/register",
            "/auth/login",
            "/auth/refresh",
            "/auth/oauth2",
            "/auth/login/oauth2",
            "/v1/auth/register",
            "/v1/auth/login",
            "/v1/auth/refresh",
            "/ws/",
            "/actuator"
    );

    public JwtAuthenticationGatewayFilter(
            JwtTokenProvider tokenProvider,
            @Autowired(required = false) ObjectMapper objectMapper
    ) {
        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Пропускаем публичные пути
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String jwt = extractJwtFromRequest(exchange.getRequest());

        // Проверяем наличие и валидность токена
        if (!StringUtils.hasText(jwt)) {
            log.warn("Missing JWT token for path: {}", path);
            return replyUnauthorized(exchange, "Missing or invalid token");
        }

        if (!tokenProvider.validateToken(jwt)) {
            log.warn("Invalid JWT token for path: {}", path);
            return replyUnauthorized(exchange, "Invalid or expired token");
        }

        try {
            // Извлекаем данные из токена (access token; roles может отсутствовать в старых токенах)
            UUID userId = tokenProvider.getUserIdFromToken(jwt);
            List<String> roles = tokenProvider.getRolesFromToken(jwt);
            if (roles == null) {
                roles = Collections.emptyList();
            }

            // Добавляем headers для downstream сервисов
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId.toString())
                    .header("X-User-Roles", String.join(",", roles))
                    .build();

            log.debug("Authenticated user: {} with roles: {}", userId, roles);

            var authorities = roles.stream().map(SimpleGrantedAuthority::new).toList();
            var authentication = new UsernamePasswordAuthenticationToken(userId.toString(), jwt, authorities);

            return chain.filter(exchange.mutate().request(mutatedRequest).build())
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));

        } catch (Exception ex) {
            log.error("Error processing JWT", ex);
            return replyUnauthorized(exchange, "Invalid token");
        }
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private String extractJwtFromRequest(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    private Mono<Void> replyUnauthorized(ServerWebExchange exchange, String message) {
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Request-Id");
        ErrorResponse error = ErrorResponse.builder()
                .error("UNAUTHORIZED")
                .message(message)
                .traceId(traceId)
                .details(Collections.emptyMap())
                .build();

        byte[] bodyBytes;
        try {
            bodyBytes = objectMapper.writeValueAsBytes(error);
        } catch (Exception e) {
            // safe fallback (still include required fields)
            String fallback = "{\"error\":\"UNAUTHORIZED\",\"message\":\"Unauthorized\",\"traceId\":\"" +
                    (traceId != null ? traceId : "") + "\",\"details\":{}}";
            bodyBytes = fallback.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bodyBytes))
        );
    }
}
