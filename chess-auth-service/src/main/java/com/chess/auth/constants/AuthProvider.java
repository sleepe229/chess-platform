package com.chess.auth.constants;

public final class AuthProvider {

    public static final String LOCAL = "LOCAL";
    public static final String GOOGLE = "GOOGLE";
    public static final String GITHUB = "GITHUB";

    private AuthProvider() {
        throw new UnsupportedOperationException("Utility class");
    }
}
