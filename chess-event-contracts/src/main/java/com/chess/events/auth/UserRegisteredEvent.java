package com.chess.events.auth;

import com.chess.events.common.DomainEvent;
import jakarta.validation.constraints.Email;
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
public class UserRegisteredEvent extends DomainEvent {

    @NotBlank
    private String userId;

    @NotBlank
    @Email
    private String email;

    @Override
    public String getAggregateId() {
        return userId;
    }
}
