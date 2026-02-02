package com.chess.gateway.it;

import com.chess.common.security.JwtTokenProvider;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayIT {

    static MockWebServer downstream;

    static {
        try {
            downstream = new MockWebServer();
            downstream.setDispatcher(new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    String path = request.getPath() == null ? "" : request.getPath();
                    if (path.startsWith("/auth/login")) {
                        return new MockResponse().setResponseCode(200).setBody("ok-auth-login");
                    }
                    if (path.startsWith("/users/ping")) {
                        return new MockResponse().setResponseCode(200).setBody("ok-users");
                    }
                    return new MockResponse().setResponseCode(404);
                }
            });
            downstream.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    static void stop() throws Exception {
        if (downstream != null) {
            downstream.shutdown();
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        String host = downstream.getHostName();
        int p = downstream.getPort();

        // RouteConfig uses these env-style properties
        r.add("AUTH_SERVICE_HOST", () -> host);
        r.add("AUTH_SERVICE_PORT", () -> String.valueOf(p));
        r.add("USER_SERVICE_HOST", () -> host);
        r.add("USER_SERVICE_PORT", () -> String.valueOf(p));
    }

    @LocalServerPort
    int port;

    @Test
    void permitsAuthPath_withoutJwt_andProxies() {
        WebTestClient webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        webTestClient.post()
                .uri("/auth/login")
                .bodyValue("{\"email\":\"u@mail.com\",\"password\":\"p\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).isEqualTo("ok-auth-login"));
    }

    @Test
    void blocksProtectedPath_withoutJwt() {
        WebTestClient webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        webTestClient.get()
                .uri("/users/ping")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void allowsProtectedPath_withJwt_andProxies() {
        UUID userId = UUID.randomUUID();
        String jwt = new JwtTokenProvider("test-jwt-secret-key-which-is-long-enough-12345", 60000, 300000)
                .generateAccessToken(userId, java.util.List.of("USER"));

        WebTestClient webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        webTestClient.get()
                .uri("/users/ping")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).isEqualTo("ok-users"));
    }
}

