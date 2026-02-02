package com.chess.events.common;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.chess.events.auth.UserRegisteredEvent;
import com.chess.events.users.RatingUpdatedEvent;
import com.chess.events.matchmaking.MatchFoundEvent;
import com.chess.events.matchmaking.PlayerDequeuedEvent;
import com.chess.events.matchmaking.PlayerQueuedEvent;
import com.chess.events.game.*;
import com.chess.events.analytics.AnalysisCompletedEvent;
import com.chess.events.analytics.AnalysisFailedEvent;
import com.chess.events.analytics.AnalysisRequestedEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
        // Auth Events
        @JsonSubTypes.Type(value = UserRegisteredEvent.class, name = "UserRegistered"),

        // User Events
        @JsonSubTypes.Type(value = RatingUpdatedEvent.class, name = "RatingUpdated"),

        // Matchmaking Events
        @JsonSubTypes.Type(value = PlayerQueuedEvent.class, name = "PlayerQueued"),
        @JsonSubTypes.Type(value = PlayerDequeuedEvent.class, name = "PlayerDequeued"),
        @JsonSubTypes.Type(value = MatchFoundEvent.class, name = "MatchFound"),

        // Game Events
        @JsonSubTypes.Type(value = GameCreatedEvent.class, name = "GameCreated"),
        @JsonSubTypes.Type(value = GameStartedEvent.class, name = "GameStarted"),
        @JsonSubTypes.Type(value = MoveMadeEvent.class, name = "MoveMade"),
        @JsonSubTypes.Type(value = GameFinishedEvent.class, name = "GameFinished"),
        @JsonSubTypes.Type(value = TimeExpiredEvent.class, name = "TimeExpired"),

        // Analytics Events
        @JsonSubTypes.Type(value = AnalysisRequestedEvent.class, name = "AnalysisRequested"),
        @JsonSubTypes.Type(value = AnalysisCompletedEvent.class, name = "AnalysisCompleted"),
        @JsonSubTypes.Type(value = AnalysisFailedEvent.class, name = "AnalysisFailed")
})
public abstract class DomainEvent implements Serializable {

    private String eventId;
    private String eventType;
    private Integer eventVersion;
    private String producer;
    private String occurredAt;
    private EventMetadata metadata;

    public abstract String getAggregateId();
}
