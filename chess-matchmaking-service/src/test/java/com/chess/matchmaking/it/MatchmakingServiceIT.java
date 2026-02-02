package com.chess.matchmaking.it;

import com.chess.common.security.JwtTokenProvider;
import com.chess.events.common.EventEnvelope;
import com.chess.events.constants.NatsSubjects;
import com.chess.events.matchmaking.PlayerDequeuedEvent;
import com.chess.events.matchmaking.PlayerQueuedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
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
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ActiveProfiles("it")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MatchmakingServiceIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("mm_it")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Container
    static final GenericContainer<?> nats = new GenericContainer<>("nats:2.10-alpine")
            .withCommand("-js")
            .withExposedPorts(4222);

    static MockWebServer userServiceMock;

    static {
        postgres.start();
        redis.start();
        nats.start();
        try {
            userServiceMock = new MockWebServer();
            userServiceMock.start();
            bootstrapStreams();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    static void stopMock() throws Exception {
        if (userServiceMock != null) {
            userServiceMock.shutdown();
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
        r.add("user-service.base-url", () -> userServiceMock.url("/").toString());
    }

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void join_then_leave_publishesQueuedAndDequeuedEvents() throws Exception {
        UUID userId = UUID.randomUUID();

        // user-service ratings stub
        userServiceMock.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "userId": "%s",
                          "ratings": [
                            { "type": "BULLET", "rating": 1500.0, "deviation": 120.0 },
                            { "type": "BLITZ", "rating": 1500.0, "deviation": 120.0 },
                            { "type": "RAPID", "rating": 1500.0, "deviation": 120.0 },
                            { "type": "CLASSICAL", "rating": 1500.0, "deviation": 120.0 }
                          ]
                        }
                        """.formatted(userId)));

        RestClient rc = RestClient.builder().baseUrl("http://localhost:" + port).build();
        String jwt = new JwtTokenProvider("it-secret-it-secret-it-secret-it-secret", 60000, 300000)
                .generateAccessToken(userId, java.util.List.of("USER"));

        // subscribe to matchmaking events
        String natsUrl = "nats://" + nats.getHost() + ":" + nats.getMappedPort(4222);
        try (Connection nc = Nats.connect(natsUrl)) {
            JetStream js = nc.jetStream();
            PullSubscribeOptions pso = PullSubscribeOptions.builder()
                    .configuration(ConsumerConfiguration.builder()
                            .deliverPolicy(DeliverPolicy.All)
                            .replayPolicy(ReplayPolicy.Instant)
                            .build())
                    .build();
            JetStreamSubscription subQueued = js.subscribe(NatsSubjects.MATCHMAKING_PLAYER_QUEUED, pso);
            JetStreamSubscription subDequeued = js.subscribe(NatsSubjects.MATCHMAKING_PLAYER_DEQUEUED, pso);

            var joinResp = rc.post()
                    .uri("/matchmaking/join")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + jwt)
                    .body(Map.of("baseSeconds", 60, "incrementSeconds", 0, "rated", true))
                    .retrieve()
                    .toEntity(Map.class);
            assertThat(joinResp.getStatusCode().value()).isEqualTo(202);
            String requestId = (String) joinResp.getBody().get("requestId");
            assertThat(requestId).isNotBlank();

            var leaveResp = rc.post()
                    .uri("/matchmaking/leave")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + jwt)
                    .body(Map.of("requestId", requestId))
                    .retrieve()
                    .toBodilessEntity();
            assertThat(leaveResp.getStatusCode().value()).isEqualTo(204);

            Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var msgs = subQueued.fetch(1, Duration.ofMillis(500));
                assertThat(msgs).hasSize(1);
                EventEnvelope<PlayerQueuedEvent> env = objectMapper.readValue(
                        new String(msgs.getFirst().getData()),
                        objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, PlayerQueuedEvent.class)
                );
                assertThat(env.getEventType()).isEqualTo("PlayerQueued");
                assertThat(env.getPayload().getUserId()).isEqualTo(userId.toString());
                assertThat(env.getPayload().getRequestId()).isEqualTo(requestId);
                msgs.getFirst().ack();
            });

            Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var msgs = subDequeued.fetch(1, Duration.ofMillis(500));
                assertThat(msgs).hasSize(1);
                EventEnvelope<PlayerDequeuedEvent> env = objectMapper.readValue(
                        new String(msgs.getFirst().getData()),
                        objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, PlayerDequeuedEvent.class)
                );
                assertThat(env.getEventType()).isEqualTo("PlayerDequeued");
                assertThat(env.getPayload().getUserId()).isEqualTo(userId.toString());
                assertThat(env.getPayload().getRequestId()).isEqualTo(requestId);
                assertThat(env.getPayload().getReason()).isEqualTo("CANCELLED");
                msgs.getFirst().ack();
            });
        }
    }

    private static void bootstrapStreams() throws Exception {
        String natsUrl = "nats://" + nats.getHost() + ":" + nats.getMappedPort(4222);
        try (Connection nc = Nats.connect(natsUrl)) {
            try {
                nc.jetStreamManagement().getStreamInfo(NatsSubjects.STREAM_MATCHMAKING);
            } catch (Exception e) {
                nc.jetStreamManagement().addStream(StreamConfiguration.builder()
                        .name(NatsSubjects.STREAM_MATCHMAKING)
                        .storageType(StorageType.File)
                        .subjects(
                                NatsSubjects.MATCHMAKING_PLAYER_QUEUED,
                                NatsSubjects.MATCHMAKING_PLAYER_DEQUEUED,
                                NatsSubjects.MATCHMAKING_MATCH_FOUND
                        )
                        .build());
            }
        }
    }
}

