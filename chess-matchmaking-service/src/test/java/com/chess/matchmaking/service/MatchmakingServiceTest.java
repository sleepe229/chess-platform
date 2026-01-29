package com.chess.matchmaking.service;

import com.chess.events.matchmaking.PlayerDequeuedEvent;
import com.chess.events.matchmaking.PlayerQueuedEvent;
import com.chess.matchmaking.config.MatchmakingProperties;
import com.chess.matchmaking.model.QueuedPlayer;
import com.chess.matchmaking.messaging.MatchmakingEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchmakingService")
class MatchmakingServiceTest {

    private static final String USER_1 = "user-1";
    private static final String USER_2 = "user-2";
    private static final String TIME_CONTROL = "180+2";
    private static final Double RATING = 1500.0;

    @Mock
    private MatchmakingQueueService queueService;

    @Mock
    private MatchmakingEventPublisher eventPublisher;

    private MatchmakingProperties properties;

    private MatchmakingService service;

    @BeforeEach
    void setUp() {
        properties = new MatchmakingProperties();
        properties.setInitialRatingRange(100);
        properties.setRatingRangeIncrement(50);
        properties.setMaxRatingRange(500);
        properties.setRangeExpansionIntervalSeconds(10);
        service = new MatchmakingService(queueService, eventPublisher, properties);
    }

    @Nested
    @DisplayName("onPlayerQueued")
    class OnPlayerQueued {

        @Test
        void addsPlayerToQueueAndTriesMatchmaking() {
            PlayerQueuedEvent event = PlayerQueuedEvent.builder()
                    .userId(USER_1)
                    .timeControl(TIME_CONTROL)
                    .rating(RATING)
                    .ratingDeviation(100.0)
                    .build();
            when(queueService.getPlayerRating(USER_1, TIME_CONTROL)).thenReturn(RATING);
            when(queueService.findMatchForPlayer(USER_1, TIME_CONTROL, RATING, 100)).thenReturn(null);

            service.onPlayerQueued(event);

            verify(queueService).addPlayerToQueue(USER_1, TIME_CONTROL, RATING, 100.0);
            verify(queueService).findMatchForPlayer(USER_1, TIME_CONTROL, RATING, 100);
            verify(eventPublisher, never()).publishMatchFound(anyString(), anyString(), anyString(), anyString(),
                    anyInt(), anyInt());
        }

        @Test
        void publishesMatchFoundWhenOpponentFound() {
            PlayerQueuedEvent event = PlayerQueuedEvent.builder()
                    .userId(USER_1)
                    .timeControl(TIME_CONTROL)
                    .rating(RATING)
                    .ratingDeviation(100.0)
                    .build();
            QueuedPlayer opponent = QueuedPlayer.builder()
                    .userId(USER_2)
                    .timeControl(TIME_CONTROL)
                    .rating(1520.0)
                    .build();
            when(queueService.getPlayerRating(USER_1, TIME_CONTROL)).thenReturn(RATING);
            when(queueService.findMatchForPlayer(USER_1, TIME_CONTROL, RATING, 100)).thenReturn(opponent);

            service.onPlayerQueued(event);

            ArgumentCaptor<String> matchIdCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> whiteCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> blackCaptor = ArgumentCaptor.forClass(String.class);
            verify(eventPublisher).publishMatchFound(
                    matchIdCaptor.capture(),
                    whiteCaptor.capture(),
                    blackCaptor.capture(),
                    eq(TIME_CONTROL),
                    eq(180),
                    eq(2));

            assertThat(matchIdCaptor.getValue()).isNotBlank();
            assertThat(whiteCaptor.getValue()).isIn(USER_1, USER_2);
            assertThat(blackCaptor.getValue()).isIn(USER_1, USER_2);
            assertThat(whiteCaptor.getValue()).isNotEqualTo(blackCaptor.getValue());
        }

        @Test
        void usesDefaultRatingWhenNull() {
            PlayerQueuedEvent event = PlayerQueuedEvent.builder()
                    .userId(USER_1)
                    .timeControl(TIME_CONTROL)
                    .rating(null)
                    .build();
            when(queueService.getPlayerRating(USER_1, TIME_CONTROL)).thenReturn(0.0);
            when(queueService.findMatchForPlayer(USER_1, TIME_CONTROL, 0.0, 100)).thenReturn(null);

            service.onPlayerQueued(event);

            verify(queueService).addPlayerToQueue(USER_1, TIME_CONTROL, 0.0, null);
        }
    }

    @Nested
    @DisplayName("onPlayerDequeued")
    class OnPlayerDequeued {

        @Test
        void removesPlayerFromQueue() {
            PlayerDequeuedEvent event = PlayerDequeuedEvent.builder()
                    .userId(USER_1)
                    .timeControl(TIME_CONTROL)
                    .reason("cancelled")
                    .build();

            service.onPlayerDequeued(event);

            verify(queueService).removePlayerFromQueue(USER_1, TIME_CONTROL);
        }
    }

    @Nested
    @DisplayName("processMatchmaking")
    class ProcessMatchmaking {

        @Test
        void doesNothingWhenNoPlayersInQueue() {
            service.processMatchmaking();

            verify(queueService, never()).getPlayerRating(anyString(), anyString());
            verify(queueService, never()).findMatchForPlayer(anyString(), anyString(), eq(RATING), anyInt());
        }

        @Test
        void expandsRangeAndTriesMatchmakingForQueuedPlayers() {
            service.onPlayerQueued(PlayerQueuedEvent.builder()
                    .userId(USER_1)
                    .timeControl(TIME_CONTROL)
                    .rating(RATING)
                    .build());
            when(queueService.findMatchForPlayer(USER_1, TIME_CONTROL, RATING, 100)).thenReturn(null);
            when(queueService.getPlayerRating(USER_1, TIME_CONTROL)).thenReturn(RATING);

            service.processMatchmaking();

            verify(queueService, org.mockito.Mockito.atLeast(1)).getPlayerRating(USER_1, TIME_CONTROL);
            verify(queueService, org.mockito.Mockito.atLeast(1)).findMatchForPlayer(USER_1, TIME_CONTROL, RATING, 100);
        }
    }
}
