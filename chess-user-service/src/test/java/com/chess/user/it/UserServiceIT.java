package com.chess.user.it;

import com.chess.events.auth.UserRegisteredEvent;
import com.chess.events.common.EventEnvelope;
import com.chess.events.constants.NatsSubjects;
import com.chess.events.game.GameFinishedEvent;
import com.chess.events.util.EventBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.Nats;
import io.nats.client.impl.Headers;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StorageType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ActiveProfiles("it")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserServiceIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("user_it")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static final GenericContainer<?> nats = new GenericContainer<>("nats:2.10-alpine")
            .withCommand("-js")
            .withExposedPorts(4222);

    static {
        postgres.start();
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

        r.add("nats.url", () -> "nats://" + nats.getHost() + ":" + nats.getMappedPort(4222));
        r.add("nats.enabled", () -> "true");
    }

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void consumesUserRegistered_andCreatesProfileAndRatings() throws Exception {
        UUID userId = UUID.randomUUID();

        publishUserRegistered(userId, "it-user@mail.com");

        RestClient rc = RestClient.builder().baseUrl("http://localhost:" + port).build();

        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> {
                    String body = rc.get().uri("/users/{id}", userId).retrieve().body(String.class);
                    assertThat(body).isNotBlank();
                    JsonNode node = objectMapper.readTree(body);
                    assertThat(node.get("userId").asText()).isEqualTo(userId.toString());
                    assertThat(node.get("username").asText()).isNotBlank();
                });

        String ratingsBody = rc.get().uri("/users/{id}/ratings", userId).retrieve().body(String.class);
        JsonNode ratings = objectMapper.readTree(ratingsBody);
        assertThat(ratings.get("userId").asText()).isEqualTo(userId.toString());
        assertThat(ratings.get("ratings").isArray()).isTrue();
        assertThat(ratings.get("ratings").size()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void consumesGameFinished_andUpdatesRatingHistory() throws Exception {
        UUID whiteId = UUID.randomUUID();
        UUID blackId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();

        publishUserRegistered(whiteId, "white@mail.com");
        publishUserRegistered(blackId, "black@mail.com");

        // wait until both users exist
        RestClient rc = RestClient.builder().baseUrl("http://localhost:" + port).build();
        Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(rc.get().uri("/users/{id}", whiteId).retrieve().toEntity(String.class).getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(rc.get().uri("/users/{id}", blackId).retrieve().toEntity(String.class).getStatusCode().is2xxSuccessful()).isTrue();
        });

        publishGameFinished(gameId, whiteId, blackId, "1-0", true, "BLITZ");

        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(400))
                .untilAsserted(() -> {
                    String body = rc.get().uri("/users/{id}/rating-history/{tc}?page=0&size=10", whiteId, "BLITZ").retrieve().body(String.class);
                    JsonNode page = objectMapper.readTree(body);
                    assertThat(page.get("content").isArray()).isTrue();
                    assertThat(page.get("content").size()).isGreaterThan(0);
                });
    }

    private void publishUserRegistered(UUID userId, String email) throws Exception {
        String natsUrl = "nats://" + nats.getHost() + ":" + nats.getMappedPort(4222);
        try (Connection nc = Nats.connect(natsUrl)) {
            JetStream js = nc.jetStream();
            ensureStreamExists(nc, NatsSubjects.STREAM_AUTH, NatsSubjects.AUTH_USER_REGISTERED);
            UserRegisteredEvent payload = UserRegisteredEvent.builder()
                    .userId(userId.toString())
                    .email(email)
                    .build();
            EventEnvelope<UserRegisteredEvent> env = EventBuilder.envelope("UserRegistered", "auth-service", payload);
            Headers h = new Headers();
            h.put("Nats-Msg-Id", env.getEventId());
            js.publish(NatsSubjects.AUTH_USER_REGISTERED, h, objectMapper.writeValueAsBytes(env));
        }
    }

    private void publishGameFinished(UUID gameId, UUID whiteId, UUID blackId, String result, boolean rated, String timeControlType) throws Exception {
        String natsUrl = "nats://" + nats.getHost() + ":" + nats.getMappedPort(4222);
        try (Connection nc = Nats.connect(natsUrl)) {
            JetStream js = nc.jetStream();
            ensureStreamExists(nc, NatsSubjects.STREAM_GAME, NatsSubjects.GAME_FINISHED);

            GameFinishedEvent payload = GameFinishedEvent.builder()
                    .gameId(gameId.toString())
                    .whitePlayerId(whiteId.toString())
                    .blackPlayerId(blackId.toString())
                    .result(result)
                    .finishReason("RESIGN")
                    .winnerId(result.equals("1-0") ? whiteId.toString() : (result.equals("0-1") ? blackId.toString() : null))
                    .finishedAt(Instant.now().toString())
                    .pgn("[Event \"IT\"]\n\n1. e4 e5\n")
                    .rated(rated)
                    .timeControlType(timeControlType)
                    .build();

            EventEnvelope<GameFinishedEvent> env = EventBuilder.envelope("GameFinished", "game-service", payload);
            Headers h = new Headers();
            h.put("Nats-Msg-Id", env.getEventId());
            js.publish(NatsSubjects.GAME_FINISHED, h, objectMapper.writeValueAsBytes(env));
        }
    }

    private void ensureStreamExists(Connection nc, String streamName, String subject) throws Exception {
        try {
            nc.jetStreamManagement().getStreamInfo(streamName);
        } catch (Exception e) {
            StreamConfiguration sc = StreamConfiguration.builder()
                    .name(streamName)
                    .storageType(StorageType.File)
                    .subjects(subject)
                    .build();
            nc.jetStreamManagement().addStream(sc);
        }
    }

    private static void bootstrapStreams() throws Exception {
        String natsUrl = "nats://" + nats.getHost() + ":" + nats.getMappedPort(4222);
        try (Connection nc = Nats.connect(natsUrl)) {
            try {
                nc.jetStreamManagement().getStreamInfo(NatsSubjects.STREAM_AUTH);
            } catch (Exception e) {
                nc.jetStreamManagement().addStream(StreamConfiguration.builder()
                        .name(NatsSubjects.STREAM_AUTH)
                        .storageType(StorageType.File)
                        .subjects(NatsSubjects.AUTH_USER_REGISTERED)
                        .build());
            }

            try {
                nc.jetStreamManagement().getStreamInfo(NatsSubjects.STREAM_GAME);
            } catch (Exception e) {
                nc.jetStreamManagement().addStream(StreamConfiguration.builder()
                        .name(NatsSubjects.STREAM_GAME)
                        .storageType(StorageType.File)
                        .subjects(NatsSubjects.GAME_FINISHED)
                        .build());
            }
        }
    }
}

