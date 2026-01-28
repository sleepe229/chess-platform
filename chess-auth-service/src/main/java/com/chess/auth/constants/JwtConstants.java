package com.chess.auth.constants;

public final class JwtConstants {
    
    private JwtConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    // Token validity in milliseconds
    public static final long ACCESS_TOKEN_VALIDITY_MS = 900_000L; // 15 minutes
    public static final long REFRESH_TOKEN_VALIDITY_MS = 2_592_000_000L; // 30 days
    
    // Token validity in seconds (for API responses)
    public static final long ACCESS_TOKEN_VALIDITY_SECONDS = ACCESS_TOKEN_VALIDITY_MS / 1000;
    public static final long REFRESH_TOKEN_VALIDITY_SECONDS = REFRESH_TOKEN_VALIDITY_MS / 1000;
}

