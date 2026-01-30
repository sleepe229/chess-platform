package com.chess.auth.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthEventPublisher")
class AuthEventPublisherTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "user@example.com";

    @Mock
    private Connection natsConnection;

    private ObjectMapper objectMapper;
    private AuthEventPublisher publisher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        publisher = new AuthEventPublisher(natsConnection, objectMapper);
    }

    @Nested
    @DisplayName("publishUserRegistered")
    class PublishUserRegistered {

        @Test
        void doesNotPublishWhenConnectionIsNull() {
            AuthEventPublisher publisherWithNullConnection = new AuthEventPublisher(null, objectMapper);

            publisherWithNullConnection.publishUserRegistered(USER_ID, EMAIL);

            verify(natsConnection, never()).publish(any(), any(byte[].class));
        }

        @Test
        void doesNotPublishWhenConnectionNotConnected() {
            when(natsConnection.getStatus()).thenReturn(Connection.Status.DISCONNECTED);

            publisher.publishUserRegistered(USER_ID, EMAIL);

            verify(natsConnection, never()).publish(any(), any(byte[].class));
        }

        @Test
        void publishesEventWhenConnected() throws Exception {
            when(natsConnection.getStatus()).thenReturn(Connection.Status.CONNECTED);

            publisher.publishUserRegistered(USER_ID, EMAIL);

            ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
            ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
            verify(natsConnection).publish(subjectCaptor.capture(), payloadCaptor.capture());

            assertThat(subjectCaptor.getValue()).isEqualTo("domain.auth.UserRegistered");
            String json = new String(payloadCaptor.getValue());
            assertThat(json).contains(USER_ID.toString()).contains(EMAIL);
        }
    }
}
