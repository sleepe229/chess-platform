package com.chess.events.analytics;

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
public class AnalysisRequestedEvent extends DomainEvent {

    @NotBlank
    private String analysisJobId;

    @NotBlank
    private String gameId;

    @NotBlank
    private String requestedBy;

    @Override
    public String getAggregateId() {
        return analysisJobId;
    }
}
