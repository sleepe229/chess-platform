package com.chess.ws.config;

import com.chess.common.messaging.ProcessedEventStore;
import com.chess.ws.messaging.RedisProcessedEventStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.Objects;

/**
 * When ws.processed-event-store.type=redis, provide Redis connection and RedisProcessedEventStore.
 * Main application excludes RedisAutoConfiguration by default so local runs without Redis.
 */
@Configuration
@ConditionalOnProperty(name = "ws.processed-event-store.type", havingValue = "redis")
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
        return new StringRedisTemplate(Objects.requireNonNull(connectionFactory));
    }

    @Bean
    public ProcessedEventStore processedEventStore(StringRedisTemplate stringRedisTemplate) {
        return new RedisProcessedEventStore(stringRedisTemplate);
    }
}
