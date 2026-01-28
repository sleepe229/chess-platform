package com.chess.events.util;

import com.chess.events.common.DomainEvent;
import com.chess.events.common.EventMetadata;

import java.time.Instant;
import java.util.UUID;

public class EventBuilder {

    public static <T extends DomainEvent> T enrichEvent(T event, String producer) {
        event.setEventId(UUID.randomUUID().toString());
        event.setEventVersion(1);
        event.setProducer(producer);
        event.setOccurredAt(Instant.now().toString());

        if (event.getMetadata() == null) {
            event.setMetadata(EventMetadata.builder()
                    .correlationId(UUID.randomUUID().toString())
                    .build());
        }

        return event;
    }
}
