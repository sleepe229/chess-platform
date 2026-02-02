package com.chess.events.matchmaking;

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
public class PlayerQueuedEvent {

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
}
