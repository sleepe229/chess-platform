package com.chess.ws.config;

import com.chess.common.messaging.InMemoryProcessedEventStore;
import com.chess.common.messaging.ProcessedEventStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ProcessedEventStoreConfig {

    @Bean
    public ProcessedEventStore processedEventStore() {
        // WS service is stateless; in-memory cache is sufficient for duplicate suppression across redeliveries.
        return new InMemoryProcessedEventStore(Duration.ofDays(7));
    }
}

