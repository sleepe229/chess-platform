package com.chess.events.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingUpdatedEvent {

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
}
