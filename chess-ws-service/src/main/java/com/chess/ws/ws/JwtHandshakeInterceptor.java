package com.chess.ws.ws;

import com.chess.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_GAME_ID = "gameId";
    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_TOKEN = "token";
    public static final String ATTR_TRACE_ID = "traceId";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            // Extract token from query (?token=...) or Authorization: Bearer ...
            String token = extractToken(request);
            if (!StringUtils.hasText(token) || !jwtTokenProvider.validateToken(token)) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            String traceId = request.getHeaders().getFirst("X-Request-Id");
            if (!StringUtils.hasText(traceId)) {
                traceId = UUID.randomUUID().toString();
            }
            attributes.put(ATTR_TRACE_ID, traceId);

            UUID userId = jwtTokenProvider.getUserIdFromToken(token);
            attributes.put(ATTR_USER_ID, userId);
            attributes.put(ATTR_TOKEN, token);

            UUID gameId = extractGameIdFromPath(request.getURI().getPath());
            attributes.put(ATTR_GAME_ID, gameId);

            return true;
        } catch (Exception e) {
            log.warn("WS handshake rejected: {}", e.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private String extractToken(ServerHttpRequest request) {
        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }

        MultiValueMap<String, String> params = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        String token = params.getFirst("token");
        if (StringUtils.hasText(token)) {
            return token;
        }
        return null;
    }

    private UUID extractGameIdFromPath(String path) {
        // expected: /ws/game/{gameId}
        int idx = path.lastIndexOf('/');
        if (idx < 0 || idx == path.length() - 1) {
            throw new IllegalArgumentException("Invalid WS path");
        }
        String last = path.substring(idx + 1);
        return UUID.fromString(last);
    }
}

