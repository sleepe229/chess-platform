package com.chess.matchmaking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

@Configuration
public class RedisScriptConfig {

    private static final String SCRIPT_PATH = "lua/find-and-remove-pair.lua";

    @Bean
    @SuppressWarnings("rawtypes")
    public RedisScript<List> findAndRemovePairScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(SCRIPT_PATH));
        script.setResultType(List.class);
        return script;
    }
}
