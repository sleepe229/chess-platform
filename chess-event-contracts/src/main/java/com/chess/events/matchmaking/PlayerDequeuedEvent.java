package com.chess.events.matchmaking;

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
public class PlayerDequeuedEvent extends DomainEvent {

    @NotBlank
    private String userId;

    @NotBlank
    private String timeControl;

    @NotBlank
    private String reason;

    @Override
    public String getAggregateId() {
        return userId;
    }
}
