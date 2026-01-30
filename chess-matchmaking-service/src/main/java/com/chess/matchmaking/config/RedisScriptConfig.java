package com.chess.matchmaking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

@Configuration
public class RedisScriptConfig {

    private static final String TRY_MATCH_SCRIPT_PATH = "lua/try-match.lua";

    @Bean
    @SuppressWarnings("rawtypes")
    public RedisScript<List> tryMatchScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(TRY_MATCH_SCRIPT_PATH));
        script.setResultType(List.class);
        return script;
    }
}
