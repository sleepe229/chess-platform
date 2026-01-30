package com.chess.matchmaking.repo;

import com.chess.matchmaking.repo.entity.MatchmakingRequestAudit;
import com.chess.matchmaking.repo.jpa.MatchmakingRequestAuditJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaMatchmakingAuditRepositoryTest {

    @Mock
    private MatchmakingRequestAuditJpaRepository jpaRepository;

    private JpaMatchmakingAuditRepository repo;

    @BeforeEach
    void setUp() {
        repo = new JpaMatchmakingAuditRepository(jpaRepository);
    }

    @Test
    void upsertQueued_updatesExistingEntity_andSaves() {
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        MatchmakingRequestAudit existing = new MatchmakingRequestAudit();
        existing.setRequestId(requestId);
        existing.setCreatedAt(Instant.now().minusSeconds(60));
        existing.setUpdatedAt(Instant.now().minusSeconds(60));
        existing.setStatus("QUEUED");

        when(jpaRepository.findById(requestId)).thenReturn(Optional.of(existing));

        repo.upsertQueued(requestId, userId, "BLITZ", 180, 2, true, 1500.0, 120.0, "rid", "idem");

        ArgumentCaptor<MatchmakingRequestAudit> captor = ArgumentCaptor.forClass(MatchmakingRequestAudit.class);
        verify(jpaRepository).save(captor.capture());

        MatchmakingRequestAudit saved = captor.getValue();
        assertThat(saved.getRequestId()).isEqualTo(requestId);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getTimeControlType()).isEqualTo("BLITZ");
        assertThat(saved.getBaseSeconds()).isEqualTo(180);
        assertThat(saved.getIncrementSeconds()).isEqualTo(2);
        assertThat(saved.isRated()).isTrue();
        assertThat(saved.getRating()).isEqualTo(1500.0);
        assertThat(saved.getRatingDeviation()).isEqualTo(120.0);
        assertThat(saved.getStatus()).isEqualTo("QUEUED");
        assertThat(saved.getMatchedGameId()).isNull();
        assertThat(saved.getCancelReason()).isNull();
        assertThat(saved.getXRequestId()).isEqualTo("rid");
        assertThat(saved.getIdempotencyKey()).isEqualTo("idem");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void markMatched_createsEntityWhenMissing_andSaves() {
        UUID requestId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();

        when(jpaRepository.findById(requestId)).thenReturn(Optional.empty());

        repo.markMatched(requestId, gameId);

        ArgumentCaptor<MatchmakingRequestAudit> captor = ArgumentCaptor.forClass(MatchmakingRequestAudit.class);
        verify(jpaRepository).save(captor.capture());

        MatchmakingRequestAudit saved = captor.getValue();
        assertThat(saved.getRequestId()).isEqualTo(requestId);
        assertThat(saved.getStatus()).isEqualTo("MATCHED");
        assertThat(saved.getMatchedGameId()).isEqualTo(gameId);
        assertThat(saved.getCancelReason()).isNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}

