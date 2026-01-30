package com.chess.matchmaking.domain;

import org.springframework.stereotype.Component;

@Component
public class TimeControlClassifier {

    public TimeControlType classify(int baseSeconds, int incrementSeconds) {
        // TS does not define exact thresholds; using common chess conventions by base time.
        if (baseSeconds <= 120) {
            return TimeControlType.BULLET;
        }
        if (baseSeconds <= 300) {
            return TimeControlType.BLITZ;
        }
        if (baseSeconds <= 900) {
            return TimeControlType.RAPID;
        }
        return TimeControlType.CLASSICAL;
    }
}

