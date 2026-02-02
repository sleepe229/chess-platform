package com.chess.game.job;

import com.chess.game.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "game.timeout-scan-enabled", havingValue = "true", matchIfMissing = true)
public class TimeoutScanJob {

    private final GameService gameService;

    @Scheduled(fixedDelayString = "${game.timeout-scan-interval-ms:1000}")
    public void scan() {
        try {
            gameService.scanTimeouts(200);
        } catch (Exception e) {
            log.warn("Timeout scan failed", e);
        }
    }
}

