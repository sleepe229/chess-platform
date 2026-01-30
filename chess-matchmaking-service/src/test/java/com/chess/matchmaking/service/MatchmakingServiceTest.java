package com.chess.matchmaking.service;

import com.chess.common.exception.ForbiddenException;
import com.chess.matchmaking.client.UserRatingsClient;
import com.chess.matchmaking.config.MatchmakingProperties;
import com.chess.matchmaking.domain.TimeControlClassifier;
import com.chess.matchmaking.domain.TimeControlType;
import com.chess.matchmaking.dto.MatchFoundDto;
import com.chess.matchmaking.repo.MatchmakingAuditRepository;
import com.chess.matchmaking.messaging.MatchmakingEventPublisher;
import com.chess.matchmaking.repo.MatchmakingRequestStore;
import com.chess.matchmaking.repo.RedisMatchmakingEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchmakingServiceTest {

    @Mock
    private MatchmakingRequestStore requestStore;
    @Mock
    private TimeControlClassifier timeControlClassifier;
    @Mock
    private UserRatingsClient userRatingsClient;
    @Mock
    private RedisMatchmakingEngine matchmakingEngine;
    @Mock
    private MatchmakingEventPublisher eventPublisher;
    @Mock
    private ObjectProvider<MatchmakingAuditRepository> auditRepositoryProvider;
    @Mock
    private MatchmakingAuditRepository auditRepository;

    private MatchmakingProperties properties;
    private MatchmakingService service;

    @BeforeEach
    void setUp() {
        properties = new MatchmakingProperties();
        properties.setInitialRatingRange(100);
        properties.setRatingRangeIncrement(50);
        properties.setMaxRatingRange(500);
        properties.setRangeExpansionIntervalSeconds(10);

        service = new MatchmakingService(
                requestStore,
                timeControlClassifier,
                userRatingsClient,
                matchmakingEngine,
                properties,
                eventPublisher,
                auditRepositoryProvider
        );
    }

    @Test
    void join_enrichesQueuedRequest_auditsQueued_andEnqueues() {
        UUID userId = UUID.randomUUID();
        String requestId = UUID.randomUUID().toString();

        when(requestStore.createOrGetActiveRequest(eq(userId), eq(180), eq(2), eq(true), eq("idem"), eq("rid")))
                .thenReturn(requestId);
        when(timeControlClassifier.classify(180, 2)).thenReturn(TimeControlType.BLITZ);
        when(userRatingsClient.fetchRating(eq(userId), eq(TimeControlType.BLITZ.name())))
                .thenReturn(new UserRatingsClient.RatingInfo(TimeControlType.BLITZ.name(), 1500.0, 120.0));
        when(auditRepositoryProvider.getIfAvailable()).thenReturn(auditRepository);

        MatchmakingRequestStore.StoredRequest initial = new MatchmakingRequestStore.StoredRequest(
                requestId, userId.toString(), "QUEUED", null, null,
                "180", "2", "true", null, null, String.valueOf(System.currentTimeMillis())
        );
        MatchmakingRequestStore.StoredRequest enriched = new MatchmakingRequestStore.StoredRequest(
                requestId, userId.toString(), "QUEUED", null, TimeControlType.BLITZ.name(),
                "180", "2", "true", "1500.0", "120.0", String.valueOf(System.currentTimeMillis())
        );
        when(requestStore.getRequest(requestId)).thenReturn(initial, enriched);

        when(matchmakingEngine.enqueueAndTryMatch(
                eq(TimeControlType.BLITZ.name()),
                eq(requestId),
                anyDouble(),
                anyInt(),
                anyInt(),
                anyInt(),
                anyLong()
        )).thenReturn(null);

        String returned = service.join(userId, 180, 2, true, "idem", "rid");

        assertThat(returned).isEqualTo(requestId);
        verify(requestStore).enrichQueuedRequest(eq(requestId), eq(TimeControlType.BLITZ.name()), eq(1500.0), eq(120.0));
        verify(auditRepository).upsertQueued(
                eq(UUID.fromString(requestId)),
                eq(userId),
                eq(TimeControlType.BLITZ.name()),
                eq(180),
                eq(2),
                eq(true),
                eq(1500.0),
                eq(120.0),
                eq("rid"),
                eq("idem")
        );
        verify(eventPublisher, never()).publishMatchFound(any(MatchFoundDto.class));
        verify(requestStore, never()).markMatched(anyString(), anyString());
    }

    @Test
    void join_whenMatched_marksMatched_publishesEvent_andAuditsMatched() {
        UUID userId = UUID.randomUUID();
        String requestId = UUID.randomUUID().toString();
        String otherRequestId = UUID.randomUUID().toString();
        UUID otherUserId = UUID.randomUUID();

        when(requestStore.createOrGetActiveRequest(eq(userId), eq(180), eq(2), eq(true), eq(null), eq(null)))
                .thenReturn(requestId);
        when(timeControlClassifier.classify(180, 2)).thenReturn(TimeControlType.BLITZ);
        when(userRatingsClient.fetchRating(eq(userId), eq(TimeControlType.BLITZ.name())))
                .thenReturn(new UserRatingsClient.RatingInfo(TimeControlType.BLITZ.name(), 1500.0, 120.0));
        when(auditRepositoryProvider.getIfAvailable()).thenReturn(auditRepository);

        MatchmakingRequestStore.StoredRequest initial = new MatchmakingRequestStore.StoredRequest(
                requestId, userId.toString(), "QUEUED", null, null,
                "180", "2", "true", null, null, String.valueOf(System.currentTimeMillis())
        );
        MatchmakingRequestStore.StoredRequest enriched = new MatchmakingRequestStore.StoredRequest(
                requestId, userId.toString(), "QUEUED", null, TimeControlType.BLITZ.name(),
                "180", "2", "true", "1500.0", "120.0", String.valueOf(System.currentTimeMillis())
        );
        when(requestStore.getRequest(requestId)).thenReturn(initial, enriched);

        when(matchmakingEngine.enqueueAndTryMatch(anyString(), anyString(), anyDouble(), anyInt(), anyInt(), anyInt(), anyLong()))
                .thenReturn(new RedisMatchmakingEngine.MatchPair(requestId, otherRequestId));

        when(requestStore.getRequest(otherRequestId)).thenReturn(new MatchmakingRequestStore.StoredRequest(
                otherRequestId, otherUserId.toString(), "QUEUED", null, TimeControlType.BLITZ.name(),
                "180", "2", "true", "1510.0", "110.0", String.valueOf(System.currentTimeMillis())
        ));

        service.join(userId, 180, 2, true, null, null);

        ArgumentCaptor<String> gameIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestStore, times(2)).markMatched(anyString(), gameIdCaptor.capture());
        String gameId = gameIdCaptor.getAllValues().get(0);
        assertThat(gameId).isNotBlank();

        ArgumentCaptor<MatchFoundDto> eventCaptor = ArgumentCaptor.forClass(MatchFoundDto.class);
        verify(eventPublisher).publishMatchFound(eventCaptor.capture());
        MatchFoundDto event = eventCaptor.getValue();

        assertThat(event.getMatchId()).isEqualTo(gameId);
        assertThat(event.getTimeControl()).isEqualTo(TimeControlType.BLITZ.name());
        assertThat(event.getInitialTimeSeconds()).isEqualTo(180);
        assertThat(event.getIncrementSeconds()).isEqualTo(2);
        assertThat(event.getRated()).isTrue();
        assertThat(event.getWhitePlayerId()).isIn(userId.toString(), otherUserId.toString());
        assertThat(event.getBlackPlayerId()).isIn(userId.toString(), otherUserId.toString());
        assertThat(event.getWhitePlayerId()).isNotEqualTo(event.getBlackPlayerId());

        verify(auditRepository).markMatched(eq(UUID.fromString(requestId)), eq(UUID.fromString(gameId)));
        verify(auditRepository).markMatched(eq(UUID.fromString(otherRequestId)), eq(UUID.fromString(gameId)));
    }

    @Test
    void leave_forOtherUser_throwsForbidden() {
        UUID userId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        String requestId = UUID.randomUUID().toString();

        when(requestStore.getRequest(requestId)).thenReturn(new MatchmakingRequestStore.StoredRequest(
                requestId, ownerId.toString(), "QUEUED", null, "BLITZ",
                "180", "2", "true", "1500.0", "120.0", String.valueOf(System.currentTimeMillis())
        ));

        assertThatThrownBy(() -> service.leave(userId, requestId, "idem", "rid"))
                .isInstanceOf(ForbiddenException.class);

        verify(requestStore, never()).cancelRequest(any(), anyString(), anyString(), anyString());
        verify(matchmakingEngine, never()).removeFromQueues(anyString(), anyString());
    }

    @Test
    void leave_cancelsRequest_removesFromQueues_andAuditsCancelled() {
        UUID userId = UUID.randomUUID();
        String requestId = UUID.randomUUID().toString();

        when(requestStore.getRequest(requestId)).thenReturn(new MatchmakingRequestStore.StoredRequest(
                requestId, userId.toString(), "QUEUED", null, "BLITZ",
                "180", "2", "true", "1500.0", "120.0", String.valueOf(System.currentTimeMillis())
        ));
        when(auditRepositoryProvider.getIfAvailable()).thenReturn(auditRepository);

        service.leave(userId, requestId, "idem", "rid");

        verify(matchmakingEngine).removeFromQueues(eq("BLITZ"), eq(requestId));
        verify(requestStore).cancelRequest(eq(userId), eq(requestId), eq("idem"), eq("rid"));
        verify(auditRepository).markCancelled(eq(UUID.fromString(requestId)), eq("cancelled"));
    }
}

