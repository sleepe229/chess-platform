package com.chess.matchmaking.service;

import com.chess.matchmaking.config.MatchmakingProperties;
import com.chess.matchmaking.domain.QueuedPlayer;
import com.chess.matchmaking.dto.MatchFoundDto;
import com.chess.matchmaking.dto.PlayerDequeuedDto;
import com.chess.matchmaking.dto.PlayerQueuedDto;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
            PlayerQueuedDto dto = PlayerQueuedDto.builder()
                    .userId(USER_1)
                    .timeControl(TIME_CONTROL)
                    .rating(RATING)
                    .ratingDeviation(100.0)
                    .build();
            when(queueService.getPlayerRating(USER_1, TIME_CONTROL)).thenReturn(RATING);
            when(queueService.findMatchForPlayer(any())).thenReturn(null);

            service.onPlayerQueued(dto);

            ArgumentCaptor<PlayerQueuedDto> addCaptor = ArgumentCaptor.forClass(PlayerQueuedDto.class);
            verify(queueService).addPlayerToQueue(addCaptor.capture());
            assertThat(addCaptor.getValue().getUserId()).isEqualTo(USER_1);
            assertThat(addCaptor.getValue().getTimeControl()).isEqualTo(TIME_CONTROL);
            assertThat(addCaptor.getValue().getRating()).isEqualTo(RATING);
            assertThat(addCaptor.getValue().getRatingDeviation()).isEqualTo(100.0);

            ArgumentCaptor<com.chess.matchmaking.dto.FindMatchRequest> findCaptor = ArgumentCaptor
                    .forClass(com.chess.matchmaking.dto.FindMatchRequest.class);
            verify(queueService).findMatchForPlayer(findCaptor.capture());
            assertThat(findCaptor.getValue().getUserId()).isEqualTo(USER_1);
            assertThat(findCaptor.getValue().getTimeControl()).isEqualTo(TIME_CONTROL);
            assertThat(findCaptor.getValue().getRating()).isEqualTo(RATING);
            assertThat(findCaptor.getValue().getRange()).isEqualTo(100);

            verify(eventPublisher, never()).publishMatchFound(any(MatchFoundDto.class));
        }

        @Test
        void publishesMatchFoundWhenOpponentFound() {
            PlayerQueuedDto dto = PlayerQueuedDto.builder()
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
            when(queueService.findMatchForPlayer(any())).thenReturn(opponent);

            service.onPlayerQueued(dto);

            ArgumentCaptor<MatchFoundDto> captor = ArgumentCaptor.forClass(MatchFoundDto.class);
            verify(eventPublisher).publishMatchFound(captor.capture());
            MatchFoundDto published = captor.getValue();
            assertThat(published.getMatchId()).isNotBlank();
            assertThat(published.getWhitePlayerId()).isIn(USER_1, USER_2);
            assertThat(published.getBlackPlayerId()).isIn(USER_1, USER_2);
            assertThat(published.getWhitePlayerId()).isNotEqualTo(published.getBlackPlayerId());
            assertThat(published.getTimeControl()).isEqualTo(TIME_CONTROL);
            assertThat(published.getInitialTimeSeconds()).isEqualTo(180);
            assertThat(published.getIncrementSeconds()).isEqualTo(2);
        }

        @Test
        void usesDefaultRatingWhenNull() {
            PlayerQueuedDto dto = PlayerQueuedDto.builder()
                    .userId(USER_1)
                    .timeControl(TIME_CONTROL)
                    .rating(null)
                    .build();
            when(queueService.getPlayerRating(USER_1, TIME_CONTROL)).thenReturn(0.0);
            when(queueService.findMatchForPlayer(any())).thenReturn(null);

            service.onPlayerQueued(dto);

            ArgumentCaptor<PlayerQueuedDto> addCaptor = ArgumentCaptor.forClass(PlayerQueuedDto.class);
            verify(queueService).addPlayerToQueue(addCaptor.capture());
            assertThat(addCaptor.getValue().getRating()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("onPlayerDequeued")
    class OnPlayerDequeued {

        @Test
        void removesPlayerFromQueue() {
            PlayerDequeuedDto dto = PlayerDequeuedDto.builder()
                    .userId(USER_1)
                    .timeControl(TIME_CONTROL)
                    .reason("cancelled")
                    .build();

            service.onPlayerDequeued(dto);

            ArgumentCaptor<PlayerDequeuedDto> captor = ArgumentCaptor.forClass(PlayerDequeuedDto.class);
            verify(queueService).removePlayerFromQueue(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_1);
            assertThat(captor.getValue().getTimeControl()).isEqualTo(TIME_CONTROL);
        }
    }

    @Nested
    @DisplayName("processMatchmaking")
    class ProcessMatchmaking {

        @Test
        void doesNothingWhenNoPlayersInQueue() {
            service.processMatchmaking();

            verify(queueService, never()).getPlayerRating(anyString(), anyString());
            verify(queueService, never()).findMatchForPlayer(any(com.chess.matchmaking.dto.FindMatchRequest.class));
        }

        @Test
        void expandsRangeAndTriesMatchmakingForQueuedPlayers() {
            service.onPlayerQueued(PlayerQueuedDto.builder()
                    .userId(USER_1)
                    .timeControl(TIME_CONTROL)
                    .rating(RATING)
                    .build());
            when(queueService.findMatchForPlayer(any())).thenReturn(null);
            when(queueService.getPlayerRating(USER_1, TIME_CONTROL)).thenReturn(RATING);

            service.processMatchmaking();

            verify(queueService, org.mockito.Mockito.atLeast(1)).getPlayerRating(USER_1, TIME_CONTROL);
            verify(queueService, org.mockito.Mockito.atLeast(1))
                    .findMatchForPlayer(any(com.chess.matchmaking.dto.FindMatchRequest.class));
        }
    }
}
