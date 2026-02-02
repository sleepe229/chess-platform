package com.chess.ws.it;

import com.chess.common.security.JwtTokenProvider;
import com.chess.events.common.EventEnvelope;
import com.chess.events.constants.NatsSubjects;
import com.chess.events.game.MoveMadeEvent;
import com.chess.events.util.EventBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.Nats;
import io.nats.client.impl.Headers;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ActiveProfiles("it")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WsServiceIT {

    @Container
    static final GenericContainer<?> nats = new GenericContainer<>("nats:2.10-alpine")
            .withCommand("-js")
            .withExposedPorts(4222);

    static MockWebServer gameServiceMock;

    static {
        nats.start();
        try {
            gameServiceMock = new MockWebServer();
            gameServiceMock.start();
            cleanupDurableConsumer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    static void stopMock() throws Exception {
        if (gameServiceMock != null) {
            gameServiceMock.shutdown();
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("nats.url", () -> "nats://" + nats.getHost() + ":" + nats.getMappedPort(4222));
        r.add("nats.enabled", () -> "true");
        r.add("game-service.base-url", () -> gameServiceMock.url("/").toString());
    }

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void wsSync_returnsGameState_andMoveMadeBroadcastsMoveAccepted() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID whiteId = UUID.randomUUID();
        UUID blackId = UUID.randomUUID();

        gameServiceMock.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getPath() != null && request.getPath().startsWith("/games/" + gameId + "/state")) {
                    // ws-service always calls with Authorization: Bearer <token>
                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody("""
                                    {
                                      "gameId": "%s",
                                      "whiteId": "%s",
                                      "blackId": "%s",
                                      "fen": "startpos",
                                      "moves": [
                                        { "ply": 1, "uci": "e2e4", "san": "e4", "fenAfter": "fen1", "playedAt": null, "byUserId": "%s" }
                                      ],
                                      "clocks": { "whiteMs": 60000, "blackMs": 60000 },
                                      "status": "RUNNING",
                                      "sideToMove": "BLACK",
                                      "result": null,
                                      "finishReason": null
                                    }
                                    """.formatted(gameId, whiteId, blackId, whiteId));
                }
                return new MockResponse().setResponseCode(404);
            }
        });

        String jwt = new JwtTokenProvider("it-secret-it-secret-it-secret-it-secret", 60000, 300000)
                .generateAccessToken(whiteId, java.util.List.of("USER"));

        BlockingQueue<String> incoming = new LinkedBlockingQueue<>();
        List<String> received = new CopyOnWriteArrayList<>();
        WebSocket ws = connect(gameId, jwt, incoming, received);

        ws.sendText("{\"type\":\"SYNC\",\"lastSeenPly\":0}", true).join();

        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            String msg = incoming.poll();
            assertThat(msg).isNotNull();
            JsonNode node = objectMapper.readTree(msg);
            assertThat(node.get("type").asText()).isEqualTo("GAME_STATE");
            assertThat(node.get("gameId").asText()).isEqualTo(gameId.toString());
            assertThat(node.get("moves").isArray()).isTrue();
        });

        publishMoveMade(gameId, whiteId, "WHITE", 2, "e2", "e4", "e4", "fen-after", 59000, 60000);

        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            String msg = received.stream().filter(s -> {
                try {
                    JsonNode n = objectMapper.readTree(s);
                    return n.hasNonNull("type") && "MOVE_ACCEPTED".equals(n.get("type").asText());
                } catch (Exception e) {
                    return false;
                }
            }).findFirst().orElse(null);
            assertThat(msg).isNotNull();
            JsonNode node = objectMapper.readTree(msg);
            assertThat(node.get("type").asText()).isEqualTo("MOVE_ACCEPTED");
            assertThat(node.get("gameId").asText()).isEqualTo(gameId.toString());
            assertThat(node.get("ply").asInt()).isEqualTo(2);
            assertThat(node.get("fen").asText()).isEqualTo("fen-after");
            assertThat(node.get("clocks").get("whiteMs").asLong()).isEqualTo(59000L);
        });

        ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye").join();
    }

    private WebSocket connect(UUID gameId, String jwt, BlockingQueue<String> incoming, List<String> received) throws Exception {
        String token = URLEncoder.encode(jwt, StandardCharsets.UTF_8);
        URI uri = URI.create("ws://localhost:" + port + "/ws/game/" + gameId + "?token=" + token);

        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<WebSocket> fut = client.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .buildAsync(uri, new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        webSocket.request(1);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        incoming.add(data.toString());
                        received.add(data.toString());
                        webSocket.request(1);
                        return CompletableFuture.completedFuture(null);
                    }
                });
        WebSocket ws = fut.join();
        assertThat(ws).isNotNull();
        return ws;
    }

    private static void cleanupDurableConsumer() throws Exception {
        // If a previous run created durable consumer with different config, JetStream rejects modifications.
        String natsUrl = "nats://" + nats.getHost() + ":" + nats.getMappedPort(4222);
        try (Connection nc = Nats.connect(natsUrl)) {
            try {
                nc.jetStreamManagement().deleteConsumer(NatsSubjects.STREAM_GAME, "ws-service-domain-game");
            } catch (Exception ignored) {
                // consumer might not exist yet
            }
        }
    }

    private void publishMoveMade(UUID gameId, UUID playerId, String color, int moveNumber,
                                 String from, String to, String san, String fen,
                                 int whiteMs, int blackMs) throws Exception {
        String natsUrl = "nats://" + nats.getHost() + ":" + nats.getMappedPort(4222);
        try (Connection nc = Nats.connect(natsUrl)) {
            JetStream js = nc.jetStream();
            MoveMadeEvent payload = MoveMadeEvent.builder()
                    .gameId(gameId.toString())
                    .moveNumber(moveNumber)
                    .playerId(playerId.toString())
                    .color(color)
                    .from(from)
                    .to(to)
                    .san(san)
                    .fen(fen)
                    .whiteTimeLeftMs(whiteMs)
                    .blackTimeLeftMs(blackMs)
                    .build();
            EventEnvelope<MoveMadeEvent> env = EventBuilder.envelope("MoveMade", "game-service", payload);
            Headers h = new Headers();
            h.put("Nats-Msg-Id", env.getEventId());
            js.publish(NatsSubjects.GAME_MOVE_MADE, h, objectMapper.writeValueAsBytes(env));
        }
    }
}

