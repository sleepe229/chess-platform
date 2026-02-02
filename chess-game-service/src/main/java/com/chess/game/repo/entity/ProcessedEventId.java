package com.chess.game.repo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEventId implements Serializable {

    @Column(name = "consumer", length = 128, nullable = false)
    private String consumer;

    @Column(name = "event_id", length = 64, nullable = false)
    private String eventId;
}

