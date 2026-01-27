package com.chess.events.constants;

public final class NatsSubjects {

    private NatsSubjects() {
        throw new UnsupportedOperationException("Utility class");
    }

    // Auth Events
    public static final String AUTH_USER_REGISTERED = "domain.auth.UserRegistered";

    // User Events
    public static final String USER_RATING_UPDATED = "domain.user.RatingUpdated";

    // Matchmaking Events
    public static final String MATCHMAKING_PLAYER_QUEUED = "domain.matchmaking.PlayerQueued";
    public static final String MATCHMAKING_PLAYER_DEQUEUED = "domain.matchmaking.PlayerDequeued";
    public static final String MATCHMAKING_MATCH_FOUND = "domain.matchmaking.MatchFound";

    // Game Events
    public static final String GAME_CREATED = "domain.game.GameCreated";
    public static final String GAME_STARTED = "domain.game.GameStarted";
    public static final String GAME_MOVE_MADE = "domain.game.MoveMade";
    public static final String GAME_FINISHED = "domain.game.GameFinished";
    public static final String GAME_TIME_EXPIRED = "domain.game.TimeExpired";

    // Analytics Events
    public static final String ANALYTICS_REQUESTED = "domain.analytics.AnalysisRequested";
    public static final String ANALYTICS_COMPLETED = "domain.analytics.AnalysisCompleted";
    public static final String ANALYTICS_FAILED = "domain.analytics.AnalysisFailed";

    // Stream Names
    public static final String STREAM_AUTH = "AUTH_EVENTS";
    public static final String STREAM_USER = "USER_EVENTS";
    public static final String STREAM_MATCHMAKING = "MATCHMAKING_EVENTS";
    public static final String STREAM_GAME = "GAME_EVENTS";
    public static final String STREAM_ANALYTICS = "ANALYTICS_EVENTS";
}
