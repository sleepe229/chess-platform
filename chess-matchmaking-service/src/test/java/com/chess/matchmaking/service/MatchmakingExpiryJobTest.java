package com.chess.matchmaking.service;

import com.chess.matchmaking.config.MatchmakingProperties;
import com.chess.matchmaking.messaging.MatchmakingEventPublisher;
import com.chess.matchmaking.repo.MatchmakingAuditRepository;
import com.chess.matchmaking.repo.MatchmakingRequestStore;
import com.chess.matchmaking.repo.RedisMatchmakingEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchmakingExpiryJobTest {

    @Mock
    private RedisMatchmakingEngine matchmakingEngine;
    @Mock
    private MatchmakingRequestStore requestStore;
    @Mock
    private MatchmakingEventPublisher eventPublisher;
    @Mock
    private ObjectProvider<MatchmakingAuditRepository> auditRepositoryProvider;
    @Mock
    private MatchmakingAuditRepository auditRepository;

    private MatchmakingProperties properties;
    private MatchmakingExpiryJob job;

    @BeforeEach
    void setUp() {
        properties = new MatchmakingProperties();
        properties.setQueueTimeoutSeconds(120);
        job = new MatchmakingExpiryJob(matchmakingEngine, requestStore, properties, eventPublisher, auditRepositoryProvider);
    }

    @Test
    void expireQueued_marksExpired_andAudits_andRemovesFromQueues() {
        String requestId = UUID.randomUUID().toString();
        UUID userId = UUID.randomUUID();

        when(matchmakingEngine.findExpired(eq("BULLET"), anyLong(), anyInt())).thenReturn(List.of(requestId));
        when(matchmakingEngine.findExpired(eq("BLITZ"), anyLong(), anyInt())).thenReturn(Collections.emptyList());
        when(matchmakingEngine.findExpired(eq("RAPID"), anyLong(), anyInt())).thenReturn(Collections.emptyList());
        when(matchmakingEngine.findExpired(eq("CLASSICAL"), anyLong(), anyInt())).thenReturn(Collections.emptyList());

        when(requestStore.getRequest(requestId)).thenReturn(new MatchmakingRequestStore.StoredRequest(
                requestId, userId.toString(), "QUEUED", null, "BULLET",
                "60", "1", "true", "1500.0", "120.0", String.valueOf(System.currentTimeMillis())
        ));

        when(auditRepositoryProvider.getIfAvailable()).thenReturn(auditRepository);

        job.expireQueued();

        verify(matchmakingEngine).removeFromQueues(eq("BULLET"), eq(requestId));
        verify(requestStore).markExpired(eq(requestId));
        verify(eventPublisher).publishPlayerDequeued(eq(requestId), eq(userId), eq("EXPIRED"));
        verify(auditRepository).markExpired(eq(UUID.fromString(requestId)));
    }

    @Test
    void expireQueued_doesNotExpireWhenAlreadyMatched_butCleansQueue() {
        String requestId = UUID.randomUUID().toString();
        UUID userId = UUID.randomUUID();

        when(matchmakingEngine.findExpired(eq("BULLET"), anyLong(), anyInt())).thenReturn(List.of(requestId));
        when(matchmakingEngine.findExpired(eq("BLITZ"), anyLong(), anyInt())).thenReturn(Collections.emptyList());
        when(matchmakingEngine.findExpired(eq("RAPID"), anyLong(), anyInt())).thenReturn(Collections.emptyList());
        when(matchmakingEngine.findExpired(eq("CLASSICAL"), anyLong(), anyInt())).thenReturn(Collections.emptyList());

        when(requestStore.getRequest(requestId)).thenReturn(new MatchmakingRequestStore.StoredRequest(
                requestId, userId.toString(), "MATCHED", "game", "BULLET",
                "60", "1", "true", "1500.0", "120.0", String.valueOf(System.currentTimeMillis())
        ));

        job.expireQueued();

        verify(matchmakingEngine).removeFromQueues(eq("BULLET"), eq(requestId));
        verify(requestStore, never()).markExpired(eq(requestId));
    }
}

