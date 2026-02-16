package com.chess.ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(excludeName = "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration")
@EnableRetry
public class ChessWsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChessWsServiceApplication.class, args);
    }

}
