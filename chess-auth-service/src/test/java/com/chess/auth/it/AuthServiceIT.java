package com.chess.auth.it;

import com.chess.auth.AuthServiceApplication;
import com.chess.events.auth.UserRegisteredEvent;
import com.chess.events.common.EventEnvelope;
import com.chess.events.constants.NatsSubjects;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Nats;
import io.nats.client.PullSubscribeOptions;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import io.nats.client.api.ReplayPolicy;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ActiveProfiles("it")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = AuthServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthServiceIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auth_it")
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
        // Ensure containers are started before Spring reads DynamicPropertySource
        postgres.start();
        redis.start();
        nats.start();
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
    void register_publishesUserRegisteredEnvelopeToNats() throws Exception {
        String natsUrl = "nats://" + nats.getHost() + ":" + nats.getMappedPort(4222);

        try (Connection nc = Nats.connect(natsUrl)) {
            JetStream js = nc.jetStream();
            ConsumerConfiguration cc = ConsumerConfiguration.builder()
                    .deliverPolicy(DeliverPolicy.All)
                    .replayPolicy(ReplayPolicy.Instant)
                    .build();
            PullSubscribeOptions pso = PullSubscribeOptions.builder().configuration(cc).build();
            JetStreamSubscription sub = js.subscribe(NatsSubjects.AUTH_USER_REGISTERED, pso);

            RestClient rc = RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .build();

            ResponseEntity<Map> resp = rc.post()
                    .uri("/auth/register")
                    .body(Map.of("email", "it-user@mail.com", "password", "password123"))
                    .retrieve()
                    .toEntity(Map.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody()).containsKey("userId");

            Awaitility.await()
                    .atMost(Duration.ofSeconds(10))
                    .pollInterval(Duration.ofMillis(200))
                    .untilAsserted(() -> {
                        var msgs = sub.fetch(1, Duration.ofMillis(500));
                        assertThat(msgs).hasSize(1);
                        String json = new String(msgs.getFirst().getData());
                        EventEnvelope<UserRegisteredEvent> env = objectMapper.readValue(
                                json,
                                objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, UserRegisteredEvent.class)
                        );
                        assertThat(env.getEventId()).isNotBlank();
                        assertThat(env.getEventType()).isEqualTo("UserRegistered");
                        assertThat(env.getProducer()).isEqualTo("auth-service");
                        assertThat(env.getCorrelationId()).isNotBlank();
                        assertThat(env.getPayload()).isNotNull();
                        assertThat(env.getPayload().getEmail()).isEqualTo("it-user@mail.com");
                        assertThat(UUID.fromString(env.getPayload().getUserId())).isNotNull();
                        msgs.getFirst().ack();
                    });
        }
    }
}

