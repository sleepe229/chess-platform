package com.chess.ws.config;

import com.chess.common.messaging.InMemoryProcessedEventStore;
import com.chess.common.messaging.ProcessedEventStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ProcessedEventStoreConfig {

    @Bean
    @ConditionalOnMissingBean(ProcessedEventStore.class)
    public ProcessedEventStore processedEventStore() {
        // Default: in-memory. Set ws.processed-event-store.type=redis for persistent store when scaling.
        return new InMemoryProcessedEventStore(Duration.ofDays(7));
    }
}

