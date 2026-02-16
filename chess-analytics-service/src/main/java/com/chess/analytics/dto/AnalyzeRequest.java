package com.chess.analytics.dto;

import lombok.Data;

@Data
public class AnalyzeRequest {
    private String priority; // optional: "normal" | "high"
}
