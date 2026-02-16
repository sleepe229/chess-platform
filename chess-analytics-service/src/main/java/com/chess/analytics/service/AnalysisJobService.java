package com.chess.analytics.service;

import com.chess.analytics.domain.AnalysisJob;
import com.chess.analytics.messaging.AnalyticsEventPublisher;
import com.chess.analytics.repo.AnalysisJobRepository;
import com.chess.events.analytics.AnalysisCompletedEvent;
import com.chess.events.analytics.AnalysisFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisJobService {

    private final AnalysisJobRepository repository;

    @Autowired(required = false)
    private AnalyticsEventPublisher analyticsEventPublisher;

    @Transactional
    public void onAnalysisCompleted(AnalysisCompletedEvent e) {
        UUID jobId = UUID.fromString(e.getAnalysisJobId());
        repository.findById(jobId).ifPresent(job -> {
            job.setStatus(AnalysisJob.STATUS_COMPLETED);
            job.setTotalMoves(e.getTotalMoves());
            job.setAccuracyWhite(e.getAccuracyWhite());
            job.setAccuracyBlack(e.getAccuracyBlack());
            job.setCompletedAt(Instant.now());
            repository.save(job);
            log.debug("Analysis job completed: jobId={}", jobId);
        });
    }

    @Transactional
    public void onAnalysisFailed(AnalysisFailedEvent e) {
        UUID jobId = UUID.fromString(e.getAnalysisJobId());
        repository.findById(jobId).ifPresent(job -> {
            job.setStatus(AnalysisJob.STATUS_FAILED);
            job.setErrorMessage(e.getErrorMessage());
            job.setCompletedAt(Instant.now());
            repository.save(job);
            log.debug("Analysis job failed: jobId={}, error={}", jobId, e.getErrorMessage());
        });
    }

    @Transactional(readOnly = true)
    public Optional<AnalysisJob> findByJobId(UUID jobId) {
        return repository.findById(jobId);
    }

    @Transactional
    public AnalysisJob createJob(UUID gameId, UUID requestedBy) {
        AnalysisJob job = AnalysisJob.builder()
                .analysisJobId(UUID.randomUUID())
                .gameId(gameId)
                .requestedBy(requestedBy)
                .status(AnalysisJob.STATUS_PENDING)
                .createdAt(Instant.now())
                .build();
        return repository.save(job);
    }

    /** Create job and publish AnalysisRequested event for async processing. */
    @Transactional
    public AnalysisJob requestAnalysis(UUID gameId, UUID requestedBy) {
        Optional<AnalysisJob> existing = repository.findFirstByGameIdAndStatus(gameId, AnalysisJob.STATUS_PENDING);
        if (existing.isPresent()) {
            return existing.get();
        }
        AnalysisJob job = createJob(gameId, requestedBy);
        if (analyticsEventPublisher != null) {
            analyticsEventPublisher.publishAnalysisRequested(
                    job.getAnalysisJobId().toString(),
                    gameId.toString(),
                    requestedBy.toString());
        }
        return job;
    }
}
