package com.chess.common.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"));

    public static String formatInstant(Instant instant) {
        if (instant == null) {
            return null;
        }
        return ISO_FORMATTER.format(instant);
    }

    public static Instant parseInstant(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return null;
        }
        return Instant.parse(dateString);
    }

    public static Instant now() {
        return Instant.now();
    }

    private DateUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
}
