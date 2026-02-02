package com.chess.events.analytics;

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
public class AnalysisCompletedEvent {

    @NotBlank
    private String analysisJobId;

    @NotBlank
    private String gameId;

    @NotNull
    private Integer totalMoves;

    @NotNull
    private Integer accuracyWhite;

    @NotNull
    private Integer accuracyBlack;
}
