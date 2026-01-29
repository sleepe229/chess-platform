package com.chess.auth.controller;

import com.chess.auth.dto.*;
import com.chess.auth.service.AuthService;
import com.chess.common.security.SecurityUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController")
class AuthControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private AuthService authService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService);
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        void returns201AndRegisterResponse() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("user@example.com")
                    .password("Password1!")
                    .build();
            RegisterResponse responseBody = RegisterResponse.builder().userId(USER_ID).build();
            when(authService.register(request)).thenReturn(responseBody);

            ResponseEntity<RegisterResponse> response = authController.register(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getUserId()).isEqualTo(USER_ID);
            verify(authService).register(request);
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        void returns200AndAuthResponse() {
            LoginRequest request = LoginRequest.builder()
                    .email("user@example.com")
                    .password("pass")
                    .build();
            AuthResponse responseBody = AuthResponse.builder()
                    .userId(USER_ID)
                    .accessToken("access")
                    .refreshToken("refresh")
                    .expiresIn(900L)
                    .build();
            when(authService.login(request)).thenReturn(responseBody);

            ResponseEntity<AuthResponse> response = authController.login(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getUserId()).isEqualTo(USER_ID);
            verify(authService).login(request);
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        void returns200AndAuthResponse() {
            RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("refresh-token").build();
            AuthResponse responseBody = AuthResponse.builder()
                    .userId(USER_ID)
                    .accessToken("new-access")
                    .refreshToken("new-refresh")
                    .expiresIn(900L)
                    .build();
            when(authService.refresh(request)).thenReturn(responseBody);

            ResponseEntity<AuthResponse> response = authController.refresh(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            verify(authService).refresh(request);
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        void returns204WhenUserAuthenticated() {
            SecurityUser user = new SecurityUser(USER_ID, List.of("USER"));
            LogoutRequest request = LogoutRequest.builder().refreshToken("token").build();

            ResponseEntity<Void> response = authController.logout(user, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(authService).logout(USER_ID, request);
        }

        @Test
        void returns401WhenUserNull() {
            ResponseEntity<Void> response = authController.logout(null, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(authService, never()).logout(any(), any());
        }
    }
}
