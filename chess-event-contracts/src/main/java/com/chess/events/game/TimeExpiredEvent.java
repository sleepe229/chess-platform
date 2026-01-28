package com.chess.events.game;

import com.chess.events.common.DomainEvent;
import jakarta.validation.constraints.NotBlank;
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
public class TimeExpiredEvent extends DomainEvent {

    @NotBlank
    private String gameId;

    @NotBlank
    private String playerId;

    @NotBlank
    private String color;

    @Override
    public String getAggregateId() {
        return gameId;
    }
}
