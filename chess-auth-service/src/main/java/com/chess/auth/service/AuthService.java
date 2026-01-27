package com.chess.auth.service;

import com.chess.auth.constants.Role;
import com.chess.auth.domain.RefreshToken;
import com.chess.auth.domain.User;
import com.chess.auth.dto.*;
import com.chess.auth.messaging.AuthEventPublisher;
import com.chess.auth.repo.RefreshTokenRepository;
import com.chess.auth.repo.UserRepository;
import com.chess.common.exception.ConflictException;
import com.chess.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthEventPublisher eventPublisher;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Registering user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration attempt with existing email: {}", request.getEmail());
            throw new ConflictException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .roles(List.of(Role.USER.name()))
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: userId={}, email={}", user.getId(), user.getEmail());

        // Публикуем событие
        try {
            eventPublisher.publishUserRegistered(user.getId(), user.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish UserRegistered event for userId: {}", user.getId(), e);
            // Don't fail registration if event publishing fails
        }

        return RegisterResponse.builder()
                .userId(user.getId())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login attempt with non-existent email: {}", request.getEmail());
                    return new UnauthorizedException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login attempt with invalid password for email: {}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRoles());
        String refreshTokenValue = jwtService.generateRefreshToken(user.getId());

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, refreshTokenValue);

        log.info("User logged in successfully: userId={}, email={}", user.getId(), user.getEmail());

        return AuthResponse.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .expiresIn(jwtService.getAccessTokenValiditySeconds())
                .build();
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        log.info("Refresh token request received");

        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(request.getRefreshToken());
        User user = refreshToken.getUser();

        log.debug("Refreshing tokens for userId: {}", user.getId());

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRoles());
        String newRefreshTokenValue = jwtService.generateRefreshToken(user.getId());

        // Revoke старый refresh token
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        // Создаем новый refresh token
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user, newRefreshTokenValue);

        log.info("Tokens refreshed successfully for userId: {}", user.getId());

        return AuthResponse.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(newRefreshTokenValue)
                .expiresIn(jwtService.getAccessTokenValiditySeconds())
                .build();
    }

    @Transactional
    public void logout(UUID userId, LogoutRequest request) {
        log.info("Logout request for userId: {}", userId);

        if (request != null && request.getRefreshToken() != null) {
            try {
                refreshTokenService.revokeRefreshToken(request.getRefreshToken());
                log.debug("Refresh token revoked for userId: {}", userId);
            } catch (Exception e) {
                log.warn("Failed to revoke refresh token for userId: {}", userId, e);
                // Continue with logout even if token revocation fails
            }
        }

        log.info("User logged out successfully: userId={}", userId);
    }

    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }
}
