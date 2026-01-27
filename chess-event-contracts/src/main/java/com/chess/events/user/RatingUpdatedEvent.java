package com.chess.events.user;

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
public class RatingUpdatedEvent extends DomainEvent {

    @NotBlank
    private String userId;

    @NotBlank
    private String timeControl;

    @NotNull
    private Double oldRating;

    @NotNull
    private Double newRating;

    @NotNull
    private Double oldRd;

    @NotNull
    private Double newRd;

    private String gameId;

    @Override
    public String getAggregateId() {
        return userId;
    }
}
