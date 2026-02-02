package com.chess.game.it;

import com.chess.common.security.JwtTokenProvider;
import com.chess.events.common.EventEnvelope;
import com.chess.events.constants.NatsSubjects;
import com.chess.events.game.GameFinishedEvent;
import com.chess.events.matchmaking.MatchFoundEvent;
import com.chess.events.util.EventBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Nats;
import io.nats.client.PullSubscribeOptions;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import io.nats.client.api.ReplayPolicy;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.impl.Headers;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ActiveProfiles("it")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameServiceIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("game_it")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Container
    static final GenericContainer<?> nats = new GenericContainer<>("nats:2.10-alpine")
            .withCommand("-js")
            .withExposedPorts(4222);

    static {
        postgres.start();
        redis.start();
        nats.start();
        try {
            bootstrapStreams();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);

        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        r.add("nats.url", () -> "nats://" + nats.getHost() + ":" + nats.getMappedPort(4222));
        r.add("nats.enabled", () -> "true");
    }

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void matchFound_createsGame_andResign_publishesGameFinished() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID whiteId = UUID.randomUUID();
        UUID blackId = UUID.randomUUID();

        String natsUrl = "nats://" + nats.getHost() + ":" + nats.getMappedPort(4222);
        try (Connection nc = Nats.connect(natsUrl)) {
            JetStream js = nc.jetStream();

            // subscribe to GAME_FINISHED
            PullSubscribeOptions pso = PullSubscribeOptions.builder()
                    .configuration(ConsumerConfiguration.builder()
                            .deliverPolicy(DeliverPolicy.All)
                            .replayPolicy(ReplayPolicy.Instant)
                            .build())
                    .build();
            JetStreamSubscription finishedSub = js.subscribe(NatsSubjects.GAME_FINISHED, pso);

            // publish MatchFound
            MatchFoundEvent payload = MatchFoundEvent.builder()
                    .gameId(gameId.toString())
                    .whitePlayerId(whiteId.toString())
                    .blackPlayerId(blackId.toString())
                    .timeControlType("BLITZ")
                    .baseSeconds(60)
                    .incrementSeconds(0)
                    .rated(true)
                    .build();
            EventEnvelope<MatchFoundEvent> env = EventBuilder.envelope("MatchFound", "matchmaking-service", payload);
            Headers h = new Headers();
            h.put("Nats-Msg-Id", env.getEventId());
            js.publish(NatsSubjects.MATCHMAKING_MATCH_FOUND, h, objectMapper.writeValueAsBytes(env));

            RestClient rc = RestClient.builder().baseUrl("http://localhost:" + port).build();
            String jwtWhite = new JwtTokenProvider("it-secret-it-secret-it-secret-it-secret", 60000, 300000)
                    .generateAccessToken(whiteId, java.util.List.of("USER"));

            Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                String body = rc.get()
                        .uri("/games/{id}/state", gameId)
                        .header("Authorization", "Bearer " + jwtWhite)
                        .retrieve()
                        .body(String.class);
                JsonNode node = objectMapper.readTree(body);
                assertThat(node.get("gameId").asText()).isEqualTo(gameId.toString());
                assertThat(node.get("whiteId").asText()).isEqualTo(whiteId.toString());
                assertThat(node.get("blackId").asText()).isEqualTo(blackId.toString());
            });

            var resignResp = rc.post()
                    .uri("/games/{id}/resign", gameId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + jwtWhite)
                    .body(Map.of())
                    .retrieve()
                    .toEntity(String.class);
            assertThat(resignResp.getStatusCode().is2xxSuccessful()).isTrue();

            Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                var msgs = finishedSub.fetch(1, Duration.ofMillis(500));
                assertThat(msgs).hasSize(1);
                EventEnvelope<GameFinishedEvent> fin = objectMapper.readValue(
                        new String(msgs.getFirst().getData()),
                        objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, GameFinishedEvent.class)
                );
                assertThat(fin.getEventType()).isEqualTo("GameFinished");
                assertThat(fin.getPayload().getGameId()).isEqualTo(gameId.toString());
                assertThat(fin.getPayload().getWhitePlayerId()).isEqualTo(whiteId.toString());
                assertThat(fin.getPayload().getBlackPlayerId()).isEqualTo(blackId.toString());
                assertThat(fin.getPayload().getResult()).isEqualTo("0-1");
                assertThat(fin.getPayload().getFinishedAt()).isNotBlank();
                msgs.getFirst().ack();
            });
        }
    }

    private static void bootstrapStreams() throws Exception {
        String natsUrl = "nats://" + nats.getHost() + ":" + nats.getMappedPort(4222);
        try (Connection nc = Nats.connect(natsUrl)) {
            // Matchmaking stream must exist before game-service listener subscribes
            try {
                nc.jetStreamManagement().getStreamInfo(NatsSubjects.STREAM_MATCHMAKING);
            } catch (Exception e) {
                nc.jetStreamManagement().addStream(StreamConfiguration.builder()
                        .name(NatsSubjects.STREAM_MATCHMAKING)
                        .storageType(StorageType.File)
                        .subjects(NatsSubjects.MATCHMAKING_MATCH_FOUND)
                        .build());
            }

            // Game stream (for publish assertions) - game-service would create it too, but harmless
            try {
                nc.jetStreamManagement().getStreamInfo(NatsSubjects.STREAM_GAME);
            } catch (Exception e) {
                nc.jetStreamManagement().addStream(StreamConfiguration.builder()
                        .name(NatsSubjects.STREAM_GAME)
                        .storageType(StorageType.File)
                        .subjects(
                                NatsSubjects.GAME_CREATED,
                                NatsSubjects.GAME_STARTED,
                                NatsSubjects.GAME_MOVE_MADE,
                                NatsSubjects.GAME_TIME_EXPIRED,
                                NatsSubjects.GAME_FINISHED
                        )
                        .build());
            }
        }
    }
}

