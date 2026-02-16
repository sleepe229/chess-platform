package com.chess.analytics.config;

import com.chess.common.messaging.InMemoryProcessedEventStore;
import com.chess.common.messaging.ProcessedEventStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ProcessedEventStoreConfig {

    @Bean
    @ConditionalOnMissingBean(ProcessedEventStore.class)
    @ConditionalOnProperty(name = "analytics.processed-event-store.type", havingValue = "inmemory")
    public ProcessedEventStore processedEventStore() {
        return new InMemoryProcessedEventStore(Duration.ofDays(7));
    }
}
