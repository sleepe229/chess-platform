package com.chess.auth.service;

import com.chess.auth.constants.Role;
import com.chess.auth.domain.RefreshToken;
import com.chess.auth.domain.User;
import com.chess.auth.dto.*;
import com.chess.auth.messaging.AuthEventPublisher;
import com.chess.auth.repo.RefreshTokenRepository;
import com.chess.auth.repo.UserRepository;
import com.chess.common.exception.ConflictException;
import com.chess.common.exception.NotFoundException;
import com.chess.common.exception.UnauthorizedException;
import com.chess.common.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "Password1!";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN_VALUE = "refresh-token";
    private static final long EXPIRES_IN = 900L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthEventPublisher eventPublisher;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtTokenProvider,
                refreshTokenService,
                eventPublisher);
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        void returnsRegisterResponseWhenEmailNotExists() {
            RegisterRequest request = RegisterRequest.builder()
                    .email(EMAIL)
                    .password(PASSWORD)
                    .build();
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded");
            User savedUser = User.builder()
                    .id(USER_ID)
                    .email(EMAIL)
                    .passwordHash("encoded")
                    .roles(List.of(Role.USER.name()))
                    .build();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            RegisterResponse response = authService.register(request);

            assertThat(response.getUserId()).isEqualTo(USER_ID);
            verify(userRepository).save(any(User.class));
            verify(eventPublisher).publishUserRegistered(USER_ID, EMAIL);
        }

        @Test
        void throwsConflictExceptionWhenEmailAlreadyExists() {
            RegisterRequest request = RegisterRequest.builder()
                    .email(EMAIL)
                    .password(PASSWORD)
                    .build();
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Email already registered");
            verify(userRepository, never()).save(any());
            verify(eventPublisher, never()).publishUserRegistered(any(), any());
        }

        @Test
        void doesNotFailRegistrationWhenEventPublishThrows() {
            RegisterRequest request = RegisterRequest.builder()
                    .email(EMAIL)
                    .password(PASSWORD)
                    .build();
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded");
            User savedUser = User.builder().id(USER_ID).email(EMAIL).passwordHash("encoded")
                    .roles(List.of(Role.USER.name())).build();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            doThrow(new RuntimeException("NATS down")).when(eventPublisher).publishUserRegistered(any(), any());

            RegisterResponse response = authService.register(request);

            assertThat(response.getUserId()).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        void returnsAuthResponseWhenCredentialsValid() {
            LoginRequest request = LoginRequest.builder().email(EMAIL).password(PASSWORD).build();
            User user = User.builder()
                    .id(USER_ID)
                    .email(EMAIL)
                    .passwordHash("encoded")
                    .roles(List.of(Role.USER.name()))
                    .build();
            RefreshToken refreshToken = RefreshToken.builder().id(UUID.randomUUID()).user(user).tokenHash("hash")
                    .build();

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, "encoded")).thenReturn(true);
            when(jwtTokenProvider.generateAccessToken(USER_ID, user.getRoles())).thenReturn(ACCESS_TOKEN);
            when(jwtTokenProvider.generateRefreshToken(USER_ID)).thenReturn(REFRESH_TOKEN_VALUE);
            when(jwtTokenProvider.getAccessTokenValidityMs()).thenReturn(EXPIRES_IN * 1000L);
            when(refreshTokenService.createRefreshToken(eq(user), eq(REFRESH_TOKEN_VALUE))).thenReturn(refreshToken);

            AuthResponse response = authService.login(request);

            assertThat(response.getUserId()).isEqualTo(USER_ID);
            assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isEqualTo(REFRESH_TOKEN_VALUE);
            assertThat(response.getExpiresIn()).isEqualTo(EXPIRES_IN);
        }

        @Test
        void throwsUnauthorizedWhenUserNotFound() {
            LoginRequest request = LoginRequest.builder().email(EMAIL).password(PASSWORD).build();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid email or password");
        }

        @Test
        void throwsUnauthorizedWhenPasswordInvalid() {
            LoginRequest request = LoginRequest.builder().email(EMAIL).password(PASSWORD).build();
            User user = User.builder().id(USER_ID).email(EMAIL).passwordHash("encoded").roles(List.of(Role.USER.name()))
                    .build();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, "encoded")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid email or password");
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        void returnsNewTokensWhenRefreshTokenValid() {
            User user = User.builder().id(USER_ID).email(EMAIL).roles(List.of(Role.USER.name())).build();
            RefreshToken refreshToken = RefreshToken.builder().id(UUID.randomUUID()).user(user).tokenHash("hash")
                    .build();
            RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken(REFRESH_TOKEN_VALUE).build();
            String newRefreshValue = "new-refresh";

            when(refreshTokenService.validateRefreshToken(REFRESH_TOKEN_VALUE)).thenReturn(refreshToken);
            when(jwtTokenProvider.generateAccessToken(USER_ID, user.getRoles())).thenReturn(ACCESS_TOKEN);
            when(jwtTokenProvider.generateRefreshToken(USER_ID)).thenReturn(newRefreshValue);
            when(jwtTokenProvider.getAccessTokenValidityMs()).thenReturn(EXPIRES_IN * 1000L);
            when(refreshTokenService.createRefreshToken(eq(user), eq(newRefreshValue))).thenReturn(refreshToken);

            AuthResponse response = authService.refresh(request);

            assertThat(response.getUserId()).isEqualTo(USER_ID);
            assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isEqualTo(newRefreshValue);
            verify(refreshTokenRepository).save(refreshToken);
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        void revokesRefreshTokenWhenRequestHasToken() {
            LogoutRequest request = LogoutRequest.builder().refreshToken(REFRESH_TOKEN_VALUE).build();

            authService.logout(USER_ID, request);

            verify(refreshTokenService).revokeRefreshToken(REFRESH_TOKEN_VALUE);
        }

        @Test
        void doesNothingWhenRequestNull() {
            authService.logout(USER_ID, null);
            verify(refreshTokenService, never()).revokeRefreshToken(any());
        }

        @Test
        void doesNothingWhenRequestTokenNull() {
            authService.logout(USER_ID, new LogoutRequest());
            verify(refreshTokenService, never()).revokeRefreshToken(any());
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        void returnsUserWhenFound() {
            User user = User.builder().id(USER_ID).email(EMAIL).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            User result = authService.getUserById(USER_ID);

            assertThat(result).isEqualTo(user);
        }

        @Test
        void throwsNotFoundExceptionWhenUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getUserById(USER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("User");
        }
    }
}
