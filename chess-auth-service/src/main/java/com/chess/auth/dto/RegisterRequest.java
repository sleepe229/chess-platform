package com.chess.auth.dto;

import com.chess.auth.constants.PasswordConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = PasswordConstants.MIN_LENGTH, max = PasswordConstants.MAX_LENGTH, 
          message = "Password must be between " + PasswordConstants.MIN_LENGTH + " and " + PasswordConstants.MAX_LENGTH + " characters")
    @Pattern(regexp = PasswordConstants.PASSWORD_PATTERN, 
             message = PasswordConstants.PASSWORD_PATTERN_MESSAGE)
    private String password;
}

