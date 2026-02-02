package com.chess.events.matchmaking;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerDequeuedEvent {

    @NotBlank
    private String requestId;

    @NotBlank
    private String userId;

    @NotBlank
    private String reason;
}
