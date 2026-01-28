package com.chess.common.utils;

import java.util.UUID;

public class IdempotencyUtils {

    public static String generateIdempotencyKey() {
        return UUID.randomUUID().toString();
    }

    public static boolean isValidUUID(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private IdempotencyUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
}
