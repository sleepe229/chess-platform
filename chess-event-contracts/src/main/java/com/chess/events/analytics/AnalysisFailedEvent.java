package com.chess.events.analytics;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisFailedEvent {

    @NotBlank
    private String analysisJobId;

    @NotBlank
    private String gameId;

    @NotBlank
    private String errorMessage;
}
