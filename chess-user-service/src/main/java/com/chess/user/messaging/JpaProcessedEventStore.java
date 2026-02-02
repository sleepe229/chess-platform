package com.chess.user.messaging;

import com.chess.common.messaging.ProcessedEventStore;
import com.chess.user.repo.ProcessedEventRepository;
import com.chess.user.repo.entity.ProcessedEventEntity;
import com.chess.user.repo.entity.ProcessedEventId;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JpaProcessedEventStore implements ProcessedEventStore {

    private final ProcessedEventRepository repository;

    @Override
    public boolean isProcessed(String consumer, String eventId) {
        return repository.existsById(new ProcessedEventId(consumer, eventId));
    }

    @Override
    public void markProcessed(String consumer, String eventId) {
        try {
            repository.save(new ProcessedEventEntity(new ProcessedEventId(consumer, eventId), Instant.now()));
        } catch (DataIntegrityViolationException ignored) {
            // already exists
        }
    }
}

