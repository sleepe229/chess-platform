package com.chess.matchmaking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.chess.matchmaking",
        "com.chess.common"
})
@EnableRetry
@EnableScheduling
public class MatchmakingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatchmakingServiceApplication.class, args);
    }
}
