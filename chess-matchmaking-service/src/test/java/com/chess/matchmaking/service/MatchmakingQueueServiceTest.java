package com.chess.matchmaking.service;

import com.chess.matchmaking.domain.QueuedPlayer;
import com.chess.matchmaking.dto.FindMatchRequest;
import com.chess.matchmaking.dto.PlayerDequeuedDto;
import com.chess.matchmaking.dto.PlayerQueuedDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchmakingQueueService")
class MatchmakingQueueServiceTest {

    private static final String USER_ID = "user-1";
    private static final String OPPONENT_ID = "user-2";
    private static final String TIME_CONTROL = "180+2";
    private static final Double RATING = 1500.0;
    private static final Double RATING_DEVIATION = 100.0;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private ObjectMapper objectMapper;

    @SuppressWarnings("rawtypes")
    @Mock
    private RedisScript<List> findAndRemovePairScript;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private MatchmakingQueueService service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new MatchmakingQueueService(
                redisTemplate,
                zSetOperations,
                objectMapper,
                findAndRemovePairScript);
    }

    @Nested
    @DisplayName("addPlayerToQueue")
    class AddPlayerToQueue {

        @Test
        void addsPlayerToZSetAndStoresPlayerData() throws JsonProcessingException {
            String expectedJson = "{\"userId\":\"user-1\",\"timeControl\":\"180+2\"}";
            when(objectMapper.writeValueAsString(any(QueuedPlayer.class))).thenReturn(expectedJson);

            PlayerQueuedDto dto = PlayerQueuedDto.builder()
                    .userId(USER_ID)
                    .timeControl(TIME_CONTROL)
                    .rating(RATING)
                    .ratingDeviation(RATING_DEVIATION)
                    .build();
            service.addPlayerToQueue(dto);

            verify(zSetOperations).add("matchmaking:queue:180+2", USER_ID, RATING);
            verify(valueOperations).set(
                    eq("matchmaking:player:180+2:user-1"),
                    eq(expectedJson),
                    eq(3600L),
                    eq(TimeUnit.SECONDS));
        }

        @Test
        void usesZeroRatingDeviationWhenNull() throws JsonProcessingException {
            when(objectMapper.writeValueAsString(any(QueuedPlayer.class))).thenReturn("{}");

            service.addPlayerToQueue(PlayerQueuedDto.builder()
                    .userId(USER_ID)
                    .timeControl(TIME_CONTROL)
                    .rating(RATING)
                    .ratingDeviation(null)
                    .build());

            ArgumentCaptor<QueuedPlayer> captor = ArgumentCaptor.forClass(QueuedPlayer.class);
            verify(objectMapper).writeValueAsString(captor.capture());
            assertThat(captor.getValue().getRatingDeviation()).isEqualTo(0.0);
        }

        @Test
        void throwsWhenSerializationFails() throws JsonProcessingException {
            when(objectMapper.writeValueAsString(any(QueuedPlayer.class)))
                    .thenThrow(new JsonProcessingException("bad") {
                    });

            PlayerQueuedDto dto = PlayerQueuedDto.builder()
                    .userId(USER_ID)
                    .timeControl(TIME_CONTROL)
                    .rating(RATING)
                    .ratingDeviation(RATING_DEVIATION)
                    .build();
            assertThatThrownBy(() -> service.addPlayerToQueue(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to add player to queue");
        }
    }

    @Nested
    @DisplayName("removePlayerFromQueue")
    class RemovePlayerFromQueue {

        @Test
        void removesFromZSetAndDeletesPlayerDataKey() {
            PlayerDequeuedDto dto = PlayerDequeuedDto.builder()
                    .userId(USER_ID)
                    .timeControl(TIME_CONTROL)
                    .reason("cancelled")
                    .build();
            service.removePlayerFromQueue(dto);

            verify(zSetOperations).remove("matchmaking:queue:180+2", USER_ID);
            verify(redisTemplate).delete(eq("matchmaking:player:180+2:user-1"));
        }
    }

    @Nested
    @DisplayName("getPlayerRating")
    class GetPlayerRating {

        @Test
        void returnsScoreFromZSet() {
            when(zSetOperations.score(eq("matchmaking:queue:180+2"), eq(USER_ID))).thenReturn(RATING);

            Double result = service.getPlayerRating(USER_ID, TIME_CONTROL);

            assertThat(result).isEqualTo(RATING);
        }

        @Test
        void returnsNullWhenPlayerNotInQueue() {
            when(zSetOperations.score(eq("matchmaking:queue:180+2"), eq(USER_ID))).thenReturn(null);

            Double result = service.getPlayerRating(USER_ID, TIME_CONTROL);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("findMatchForPlayer")
    class FindMatchForPlayer {

        @Test
        void returnsNullWhenScriptReturnsNull() {
            when(redisTemplate.execute(eq(findAndRemovePairScript), anyList(), any(), any(), any()))
                    .thenReturn(null);

            FindMatchRequest request = FindMatchRequest.builder()
                    .userId(USER_ID)
                    .timeControl(TIME_CONTROL)
                    .rating(RATING)
                    .range(100)
                    .build();
            QueuedPlayer result = service.findMatchForPlayer(request);

            assertThat(result).isNull();
            verify(redisTemplate, never()).delete(any(String.class));
            verify(redisTemplate, never()).delete(any(Collection.class));
        }

        @Test
        void returnsNullWhenScriptReturnsSingleElement() {
            when(redisTemplate.execute(eq(findAndRemovePairScript), anyList(), any(), any(), any()))
                    .thenReturn(List.of(USER_ID));

            FindMatchRequest request = FindMatchRequest.builder()
                    .userId(USER_ID)
                    .timeControl(TIME_CONTROL)
                    .rating(RATING)
                    .range(100)
                    .build();
            QueuedPlayer result = service.findMatchForPlayer(request);

            assertThat(result).isNull();
        }

        @Test
        void returnsOpponentAndDeletesBothPlayerDataWhenScriptReturnsPair() throws JsonProcessingException {
            when(redisTemplate.execute(eq(findAndRemovePairScript), anyList(), any(), any(), any()))
                    .thenReturn(List.of(USER_ID, OPPONENT_ID));

            QueuedPlayer opponentData = QueuedPlayer.builder()
                    .userId(OPPONENT_ID)
                    .timeControl(TIME_CONTROL)
                    .rating(1520.0)
                    .ratingDeviation(90.0)
                    .queueTime(1L)
                    .build();
            String opponentJson = "{\"userId\":\"user-2\",\"timeControl\":\"180+2\",\"rating\":1520.0}";
            when(valueOperations.get("matchmaking:player:180+2:user-2")).thenReturn(opponentJson);
            when(objectMapper.readValue(opponentJson, QueuedPlayer.class)).thenReturn(opponentData);

            FindMatchRequest request = FindMatchRequest.builder()
                    .userId(USER_ID)
                    .timeControl(TIME_CONTROL)
                    .rating(RATING)
                    .range(100)
                    .build();
            QueuedPlayer result = service.findMatchForPlayer(request);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(OPPONENT_ID);
            assertThat(result.getRating()).isEqualTo(1520.0);

            ArgumentCaptor<List<String>> deleteCaptor = ArgumentCaptor.forClass(List.class);
            verify(redisTemplate).delete(deleteCaptor.capture());
            assertThat(deleteCaptor.getValue())
                    .containsExactlyInAnyOrder(
                            "matchmaking:player:180+2:user-1",
                            "matchmaking:player:180+2:user-2");
        }

        @Test
        void buildsMinimalOpponentWhenPlayerDataMissingAndDeletesKeys() {
            when(redisTemplate.execute(eq(findAndRemovePairScript), anyList(), any(), any(), any()))
                    .thenReturn(List.of(USER_ID, OPPONENT_ID));
            when(valueOperations.get(anyString())).thenReturn(null);

            FindMatchRequest request = FindMatchRequest.builder()
                    .userId(USER_ID)
                    .timeControl(TIME_CONTROL)
                    .rating(RATING)
                    .range(100)
                    .build();
            QueuedPlayer result = service.findMatchForPlayer(request);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(OPPONENT_ID);
            assertThat(result.getTimeControl()).isEqualTo(TIME_CONTROL);
            assertThat(result.getRating()).isEqualTo(0.0);
            verify(redisTemplate).delete(any(List.class));
        }
    }
}
