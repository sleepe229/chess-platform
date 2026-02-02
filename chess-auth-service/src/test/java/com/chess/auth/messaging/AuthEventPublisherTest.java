package com.chess.auth.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.impl.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthEventPublisher")
class AuthEventPublisherTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "user@example.com";

    @Mock
    private Connection natsConnection;

    @Mock
    private JetStream jetStream;

    private ObjectMapper objectMapper;
    private AuthEventPublisher publisher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        publisher = new AuthEventPublisher(objectMapper);
        ReflectionTestUtils.setField(publisher, "natsConnection", natsConnection);
        ReflectionTestUtils.setField(publisher, "jetStream", jetStream);
    }

    @Nested
    @DisplayName("publishUserRegistered")
    class PublishUserRegistered {

        @Test
        void doesNotPublishWhenConnectionIsNull() {
            ReflectionTestUtils.setField(publisher, "natsConnection", null);

            publisher.publishUserRegistered(USER_ID, EMAIL);

            verify(natsConnection, never()).publish(any(), any(byte[].class));
            verifyNoInteractions(jetStream);
        }

        @Test
        void doesNotPublishWhenConnectionNotConnected() {
            when(natsConnection.getStatus()).thenReturn(Connection.Status.DISCONNECTED);

            publisher.publishUserRegistered(USER_ID, EMAIL);

            verify(natsConnection, never()).publish(any(), any(byte[].class));
            verifyNoInteractions(jetStream);
        }

        @Test
        void publishesEventWhenConnected() throws Exception {
            when(natsConnection.getStatus()).thenReturn(Connection.Status.CONNECTED);

            publisher.publishUserRegistered(USER_ID, EMAIL);

            ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
            ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Headers> headersCaptor = ArgumentCaptor.forClass(Headers.class);
            verify(jetStream).publish(subjectCaptor.capture(), headersCaptor.capture(), payloadCaptor.capture());

            assertThat(subjectCaptor.getValue()).isEqualTo("domain.auth.UserRegistered");
            String json = new String(payloadCaptor.getValue());
            assertThat(json).contains(USER_ID.toString()).contains(EMAIL);
            assertThat(headersCaptor.getValue().getFirst("Nats-Msg-Id")).isNotBlank();
        }
    }
}
