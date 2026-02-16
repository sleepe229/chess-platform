package com.chess.analytics.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Stub controller so /v1/analysis/** does not return 404.
 * Full game analysis API can be implemented later.
 */
@RestController
@RequestMapping("/analysis")
public class AnalysisController {

    @GetMapping
    public ResponseEntity<Map<String, String>> info() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of(
                        "status", "not_implemented",
                        "message", "Game analysis API is planned. Use /v1/games/{id}/state for game data."
                ));
    }
}
