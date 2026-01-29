package com.chess.events.game;

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
public class GameCreatedEvent extends DomainEvent {

    @NotBlank
    private String gameId;

    @NotBlank
    private String whitePlayerId;

    @NotBlank
    private String blackPlayerId;

    @NotBlank
    private String timeControl;

    @NotNull
    private Integer initialTimeSeconds;

    @NotNull
    private Integer incrementSeconds;

    @Override
    public String getAggregateId() {
        return gameId;
    }
}
