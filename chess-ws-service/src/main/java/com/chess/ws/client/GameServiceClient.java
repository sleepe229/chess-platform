package com.chess.ws.client;

import com.chess.common.dto.ErrorResponse;
import com.chess.ws.dto.GameStateMessage;
import com.chess.ws.dto.MoveCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameServiceClient {

    private final ObjectMapper objectMapper;

    @Value("${game-service.base-url}")
    private String baseUrl;

    @Value("${game-service.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${game-service.read-timeout-ms:15000}")
    private int readTimeoutMs;

    private RestClient restClient;

    @PostConstruct
    void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(java.time.Duration.ofMillis(readTimeoutMs));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    private RestClient client() {
        return restClient;
    }

    public GameStateMessage getState(UUID gameId, String token) {
        return client().get()
                .uri("/games/{id}/state", gameId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-Request-Id", requestId())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(GameStateMessage.class);
    }

    public GameStateMessage move(UUID gameId, String token, MoveCommand request) {
        return client().post()
                .uri("/games/{id}/move", gameId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-Request-Id", requestId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GameStateMessage.class);
    }

    public GameStateMessage resign(UUID gameId, String token) {
        return client().post()
                .uri("/games/{id}/resign", gameId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-Request-Id", requestId())
                .retrieve()
                .body(GameStateMessage.class);
    }

    public GameStateMessage offerDraw(UUID gameId, String token) {
        return client().post()
                .uri("/games/{id}/offer-draw", gameId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-Request-Id", requestId())
                .retrieve()
                .body(GameStateMessage.class);
    }

    public GameStateMessage acceptDraw(UUID gameId, String token) {
        return client().post()
                .uri("/games/{id}/accept-draw", gameId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-Request-Id", requestId())
                .retrieve()
                .body(GameStateMessage.class);
    }

    private String requestId() {
        String traceId = MDC.get("traceId");
        return traceId != null && !traceId.isBlank() ? traceId : UUID.randomUUID().toString();
    }

    public String extractErrorCode(Throwable t) {
        if (t instanceof HttpStatusCodeException hsce) {
            try {
                ErrorResponse er = objectMapper.readValue(hsce.getResponseBodyAsByteArray(), ErrorResponse.class);
                return er.getError() != null ? er.getError() : "ERROR";
            } catch (Exception ignored) {
                return "ERROR";
            }
        }
        return "ERROR";
    }
}

