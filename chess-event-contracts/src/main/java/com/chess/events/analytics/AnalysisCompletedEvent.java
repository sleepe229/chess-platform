package com.chess.events.analytics;

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
public class AnalysisCompletedEvent extends DomainEvent {

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

    @Override
    public String getAggregateId() {
        return analysisJobId;
    }
}
