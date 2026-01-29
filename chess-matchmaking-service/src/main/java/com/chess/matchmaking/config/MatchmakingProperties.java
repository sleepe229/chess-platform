package com.chess.matchmaking.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "matchmaking")
public class MatchmakingProperties {

    private int initialRatingRange = 100;
    private int ratingRangeIncrement = 50;
    private int maxRatingRange = 500;
    private long rangeExpansionIntervalSeconds = 10;

    private Map<String, TimeControlParams> timeControls = defaultTimeControls();

    private static Map<String, TimeControlParams> defaultTimeControls() {
        Map<String, TimeControlParams> map = new HashMap<>();
        map.put("180+2", new TimeControlParams(180, 2));
        map.put("60+1", new TimeControlParams(60, 1));
        map.put("300+5", new TimeControlParams(300, 5));
        map.put("600+10", new TimeControlParams(600, 10));
        return map;
    }

    @Data
    public static class TimeControlParams {
        private int initialTimeSeconds;
        private int incrementSeconds;

        public TimeControlParams() {
        }

        public TimeControlParams(int initialTimeSeconds, int incrementSeconds) {
            this.initialTimeSeconds = initialTimeSeconds;
            this.incrementSeconds = incrementSeconds;
        }
    }
}
