package com.chess.matchmaking.messaging;

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
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchmakingEventPublisher")
class MatchmakingEventPublisherTest {

    private static final String MATCH_ID = "match-123";
    private static final String WHITE_ID = "user-white";
    private static final String BLACK_ID = "user-black";
    private static final String TIME_CONTROL = "180+2";
    private static final int INITIAL_TIME = 180;
    private static final int INCREMENT = 2;

    @Mock
    private Connection natsConnection;

    private ObjectMapper objectMapper;
    private MatchmakingEventPublisher publisher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        publisher = new MatchmakingEventPublisher(objectMapper);
        ReflectionTestUtils.setField(publisher, "natsConnection", natsConnection);
    }

    @Nested
    @DisplayName("publishMatchFound")
    class PublishMatchFound {

        @Test
        void doesNotPublishWhenConnectionIsNull() {
            ReflectionTestUtils.setField(publisher, "natsConnection", null);

            publisher.publishMatchFound(MATCH_ID, WHITE_ID, BLACK_ID, TIME_CONTROL, INITIAL_TIME, INCREMENT);

            verify(natsConnection, never()).publish(any(), any(byte[].class));
        }

        @Test
        void doesNotPublishWhenConnectionNotConnected() {
            when(natsConnection.getStatus()).thenReturn(Connection.Status.DISCONNECTED);

            publisher.publishMatchFound(MATCH_ID, WHITE_ID, BLACK_ID, TIME_CONTROL, INITIAL_TIME, INCREMENT);

            verify(natsConnection, never()).publish(any(), any(byte[].class));
        }

        @Test
        void publishesEventWhenConnected() throws Exception {
            when(natsConnection.getStatus()).thenReturn(Connection.Status.CONNECTED);

            publisher.publishMatchFound(MATCH_ID, WHITE_ID, BLACK_ID, TIME_CONTROL, INITIAL_TIME, INCREMENT);

            ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
            ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
            verify(natsConnection).publish(subjectCaptor.capture(), payloadCaptor.capture());

            assertThat(subjectCaptor.getValue()).isEqualTo("domain.matchmaking.MatchFound");
            String json = new String(payloadCaptor.getValue());
            assertThat(json).contains(MATCH_ID).contains(WHITE_ID).contains(BLACK_ID)
                    .contains(TIME_CONTROL).contains("180").contains("2");
        }
    }
}
