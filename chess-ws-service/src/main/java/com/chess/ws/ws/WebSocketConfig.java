package com.chess.ws.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final GameWebSocketHandler handler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Value("${spring.websocket.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = allowedOrigins != null ? allowedOrigins.split(",") : new String[]{"*"};
        registry.addHandler(handler, "/ws/game/{gameId}")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins(origins);
    }
}

