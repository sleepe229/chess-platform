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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

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

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUserSuccessfully() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("test@example.com")
                    .password("Password123")
                    .build();

            UUID userId = UUID.randomUUID();
            User savedUser = User.builder()
                    .id(userId)
                    .email("test@example.com")
                    .passwordHash("hashedPassword")
                    .roles(List.of(Role.USER.name()))
                    .createdAt(Instant.now())
                    .build();

            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("Password123")).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            RegisterResponse response = authService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(userId);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getEmail()).isEqualTo("test@example.com");
            assertThat(capturedUser.getPasswordHash()).isEqualTo("hashedPassword");
            assertThat(capturedUser.getRoles()).contains(Role.USER.name());

            verify(eventPublisher).publishUserRegistered(userId, "test@example.com");
        }

        @Test
        @DisplayName("Should throw ConflictException when email already exists")
        void shouldThrowConflictExceptionWhenEmailExists() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("existing@example.com")
                    .password("Password123")
                    .build();

            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Email already registered");

            verify(userRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Should continue registration even if event publishing fails")
        void shouldContinueRegistrationEvenIfEventPublishingFails() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("test@example.com")
                    .password("Password123")
                    .build();

            UUID userId = UUID.randomUUID();
            User savedUser = User.builder()
                    .id(userId)
                    .email("test@example.com")
                    .passwordHash("hashedPassword")
                    .roles(List.of(Role.USER.name()))
                    .createdAt(Instant.now())
                    .build();

            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("Password123")).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            doThrow(new RuntimeException("Event publishing failed")).when(eventPublisher).publishUserRegistered(any(), any());

            RegisterResponse response = authService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(userId);
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login user successfully with valid credentials")
        void shouldLoginUserSuccessfully() {
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("Password123")
                    .build();

            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .email("test@example.com")
                    .passwordHash("hashedPassword")
                    .roles(List.of(Role.USER.name()))
                    .build();

            RefreshToken refreshToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .tokenHash("tokenHash")
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("Password123", "hashedPassword")).thenReturn(true);
            when(jwtTokenProvider.generateAccessToken(userId, List.of(Role.USER.name()))).thenReturn("accessToken");
            when(jwtTokenProvider.generateRefreshToken(userId)).thenReturn("refreshToken");
            when(refreshTokenService.createRefreshToken(user, "refreshToken")).thenReturn(refreshToken);
            when(jwtTokenProvider.getAccessTokenValidityMs()).thenReturn(900000L);

            AuthResponse response = authService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getAccessToken()).isEqualTo("accessToken");
            assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
            assertThat(response.getExpiresIn()).isEqualTo(900);
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when email not found")
        void shouldThrowUnauthorizedExceptionWhenEmailNotFound() {
            LoginRequest request = LoginRequest.builder()
                    .email("nonexistent@example.com")
                    .password("Password123")
                    .build();

            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid email or password");
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when password is incorrect")
        void shouldThrowUnauthorizedExceptionWhenPasswordIncorrect() {
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("WrongPassword")
                    .build();

            User user = User.builder()
                    .id(UUID.randomUUID())
                    .email("test@example.com")
                    .passwordHash("hashedPassword")
                    .roles(List.of(Role.USER.name()))
                    .build();

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("WrongPassword", "hashedPassword")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid email or password");
        }
    }

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh tokens successfully")
        void shouldRefreshTokensSuccessfully() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("validRefreshToken")
                    .build();

            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .email("test@example.com")
                    .passwordHash("hashedPassword")
                    .roles(List.of(Role.USER.name()))
                    .build();

            RefreshToken oldRefreshToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .tokenHash("oldTokenHash")
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            RefreshToken newRefreshToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .tokenHash("newTokenHash")
                    .expiresAt(Instant.now().plusSeconds(7200))
                    .build();

            when(refreshTokenService.validateRefreshToken("validRefreshToken")).thenReturn(oldRefreshToken);
            when(jwtTokenProvider.generateAccessToken(userId, List.of(Role.USER.name()))).thenReturn("newAccessToken");
            when(jwtTokenProvider.generateRefreshToken(userId)).thenReturn("newRefreshToken");
            when(refreshTokenRepository.save(any())).thenReturn(oldRefreshToken);
            when(refreshTokenService.createRefreshToken(user, "newRefreshToken")).thenReturn(newRefreshToken);
            when(jwtTokenProvider.getAccessTokenValidityMs()).thenReturn(900000L);

            AuthResponse response = authService.refresh(request);

            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
            assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken");
        }
    }

    @Nested
    @DisplayName("Logout Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should logout user successfully with refresh token")
        void shouldLogoutUserSuccessfullyWithRefreshToken() {
            UUID userId = UUID.randomUUID();
            LogoutRequest request = LogoutRequest.builder()
                    .refreshToken("refreshToken")
                    .build();

            authService.logout(userId, request);

            verify(refreshTokenService).revokeRefreshToken("refreshToken");
        }

        @Test
        @DisplayName("Should logout user successfully without refresh token")
        void shouldLogoutUserSuccessfullyWithoutRefreshToken() {
            UUID userId = UUID.randomUUID();

            authService.logout(userId, null);

            verifyNoInteractions(refreshTokenService);
        }

        @Test
        @DisplayName("Should continue logout even if token revocation fails")
        void shouldContinueLogoutEvenIfTokenRevocationFails() {
            UUID userId = UUID.randomUUID();
            LogoutRequest request = LogoutRequest.builder()
                    .refreshToken("refreshToken")
                    .build();

            doThrow(new RuntimeException("Revocation failed")).when(refreshTokenService).revokeRefreshToken("refreshToken");

            // Should not throw exception
            authService.logout(userId, request);

            verify(refreshTokenService).revokeRefreshToken("refreshToken");
        }
    }

    @Nested
    @DisplayName("GetUserById Tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return user when found")
        void shouldReturnUserWhenFound() {
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .email("test@example.com")
                    .passwordHash("hashedPassword")
                    .roles(List.of(Role.USER.name()))
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            User result = authService.getUserById(userId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(userId);
            assertThat(result.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should throw NotFoundException when user not found")
        void shouldThrowNotFoundExceptionWhenUserNotFound() {
            UUID userId = UUID.randomUUID();

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getUserById(userId))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
