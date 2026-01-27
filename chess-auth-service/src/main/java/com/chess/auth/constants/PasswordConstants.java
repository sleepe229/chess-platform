package com.chess.auth.constants;

public final class PasswordConstants {
    
    private PasswordConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 100;
    
    // Password validation regex: at least one letter and one number
    public static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d).+$";
    public static final String PASSWORD_PATTERN_MESSAGE = 
            "Password must contain at least one letter and one number";
}

