package com.chess.matchmaking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.mockito.Mockito.mock;

@SpringBootTest(properties = "nats.enabled=false")
@ActiveProfiles("test")
@ContextConfiguration(classes = MatchmakingServiceApplicationTests.TestConfig.class)
class MatchmakingServiceApplicationTests {

    @Configuration
    static class TestConfig {
        @Bean
        @Primary
        RedisConnectionFactory redisConnectionFactory() {
            return mock(RedisConnectionFactory.class);
        }
    }

    @Test
    void contextLoads() {
    }
}
