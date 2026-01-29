package com.chess.events.matchmaking;

import com.chess.events.common.DomainEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PlayerQueuedEvent extends DomainEvent {

    @NotBlank
    private String requestId;

    @NotBlank
    private String userId;

    @NotBlank
    private String timeControlType;

    @NotNull
    private Integer baseSeconds;

    @NotNull
    private Integer incrementSeconds;

    @NotNull
    private Boolean rated;

    @NotNull
    private Double rating;
    
    @NotNull
    private Double ratingDeviation;

    @Override
    public String getAggregateId() {
        return userId;
    }
}
