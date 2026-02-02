package com.chess.events.game;

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
public class MoveMadeEvent {

    @NotBlank
    private String gameId;

    @NotNull
    private Integer moveNumber;

    @NotBlank
    private String playerId;

    @NotBlank
    private String color;

    @NotBlank
    private String from;

    @NotBlank
    private String to;

    private String promotion;

    @NotBlank
    private String san;

    @NotBlank
    private String fen;

    @NotNull
    private Integer whiteTimeLeftMs;

    @NotNull
    private Integer blackTimeLeftMs;

    private Boolean isCheck;
    private Boolean isCheckmate;
    private Boolean isStalemate;
    private Boolean isDraw;
}
