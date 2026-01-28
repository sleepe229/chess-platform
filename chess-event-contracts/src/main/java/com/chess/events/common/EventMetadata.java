package com.chess.events.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMetadata implements Serializable {

    private String correlationId;
    private String causationId;
    private String userId;
    private String traceId;
    private Map<String, String> tags;
}
