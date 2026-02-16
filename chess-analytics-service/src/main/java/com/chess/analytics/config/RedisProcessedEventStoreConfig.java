package com.chess.analytics.config;

import com.chess.analytics.messaging.RedisProcessedEventStore;
import com.chess.common.messaging.ProcessedEventStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnProperty(name = "analytics.processed-event-store.type", havingValue = "redis")
public class RedisProcessedEventStoreConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory(
            @org.springframework.beans.factory.annotation.Value("${spring.data.redis.host:localhost}") String host,
            @org.springframework.beans.factory.annotation.Value("${spring.data.redis.port:6379}") int port) {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(host, port);
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public ProcessedEventStore processedEventStore(StringRedisTemplate stringRedisTemplate) {
        return new RedisProcessedEventStore(stringRedisTemplate);
    }
}
