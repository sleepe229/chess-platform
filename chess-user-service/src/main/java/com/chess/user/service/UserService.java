package com.chess.user.service;

import com.chess.common.exception.ConflictException;
import com.chess.common.exception.NotFoundException;
import com.chess.user.domain.User;
import com.chess.user.dto.UpdateProfileRequest;
import com.chess.user.dto.UserProfilePublicResponse;
import com.chess.user.dto.UserProfileResponse;
import com.chess.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RatingService ratingService;

    @Transactional
    public User createUser(UUID userId, String email) {
        log.info("Creating user profile for userId: {}, email: {}", userId, email);

        if (userRepository.existsById(userId)) {
            throw new ConflictException("User already exists");
        }

        String username = generateUsername(email);

        User user = User.builder()
                .id(userId)
                .username(username)
                .email(email)
                .build();

        user = saveUserWithUniqueUsername(user);

        ratingService.initializeRatings(userId);

        log.info("User profile created: {}", userId);
        return user;
    }

    private User saveUserWithUniqueUsername(User user) {
        String baseUsername = user.getUsername();
        int counter = 1;
        for (int attempt = 0; attempt < 100; attempt++) {
            try {
                return userRepository.save(user);
            } catch (DataIntegrityViolationException e) {
                if (e.getMessage() != null && e.getMessage().contains("username")) {
                    user.setUsername(baseUsername + counter++);
                } else {
                    throw e;
                }
            }
        }
        throw new ConflictException("Could not generate unique username");
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .country(user.getCountry())
                .totalGames(user.getTotalGames())
                .totalWins(user.getTotalWins())
                .totalLosses(user.getTotalLosses())
                .totalDraws(user.getTotalDraws())
                .winRate(user.getWinRate())
                .ratings(ratingService.getUserRatings(userId))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public UserProfilePublicResponse getUserProfilePublic(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return UserProfilePublicResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .avatarUrl(user.getAvatarUrl())
                .country(user.getCountry())
                .totalGames(user.getTotalGames())
                .totalWins(user.getTotalWins())
                .totalLosses(user.getTotalLosses())
                .totalDraws(user.getTotalDraws())
                .winRate(user.getWinRate())
                .ratings(ratingService.getUserRatings(userId))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        log.info("Updating profile for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new ConflictException("Username already taken");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        if (request.getCountry() != null) {
            user.setCountry(request.getCountry());
        }

        user = userRepository.save(user);
        log.info("Profile updated for userId: {}", userId);

        return getUserProfile(userId);
    }

    @Transactional
    public void updateGameStats(UUID userId, String result) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        user.incrementGames();

        switch (result.toUpperCase()) {
            case "WIN" -> user.incrementWins();
            case "LOSS" -> user.incrementLosses();
            case "DRAW" -> user.incrementDraws();
        }

        userRepository.save(user);
        log.info("Game stats updated for userId: {}, result: {}", userId, result);
    }

    @Transactional(readOnly = true)
    public User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private String generateUsername(String email) {
        String baseUsername = email.split("@")[0];
        String username = baseUsername;
        int counter = 1;

        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter++;
        }

        return username;
    }
}
