package com.chess.user.service;

import com.chess.common.exception.ConflictException;
import com.chess.common.exception.NotFoundException;
import com.chess.user.domain.User;
import com.chess.user.dto.UpdateProfileRequest;
import com.chess.user.dto.UserProfilePublicResponse;
import com.chess.user.dto.UserProfileResponse;
import com.chess.user.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RatingService ratingService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, ratingService);
    }

    @Nested
    @DisplayName("Create User Tests")
    class CreateUserTests {

        @Test
        @DisplayName("Should create user successfully")
        void shouldCreateUserSuccessfully() {
            UUID userId = UUID.randomUUID();
            String email = "test@example.com";
            
            User savedUser = User.builder()
                    .id(userId)
                    .username("test")
                    .email(email)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(userRepository.existsById(userId)).thenReturn(false);
            when(userRepository.existsByUsername("test")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            User result = userService.createUser(userId, email);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(userId);
            assertThat(result.getEmail()).isEqualTo(email);
            
            verify(ratingService).initializeRatings(userId);
        }

        @Test
        @DisplayName("Should throw ConflictException when user already exists")
        void shouldThrowConflictExceptionWhenUserExists() {
            UUID userId = UUID.randomUUID();
            String email = "test@example.com";

            when(userRepository.existsById(userId)).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(userId, email))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("User already exists");

            verify(userRepository, never()).save(any());
            verifyNoInteractions(ratingService);
        }

        @Test
        @DisplayName("Should generate unique username when conflict occurs")
        void shouldGenerateUniqueUsernameWhenConflictOccurs() {
            UUID userId = UUID.randomUUID();
            String email = "test@example.com";
            
            User savedUser = User.builder()
                    .id(userId)
                    .username("test1")
                    .email(email)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(userRepository.existsById(userId)).thenReturn(false);
            when(userRepository.existsByUsername("test")).thenReturn(true);
            when(userRepository.existsByUsername("test1")).thenReturn(false);
            when(userRepository.save(any(User.class)))
                    .thenThrow(new DataIntegrityViolationException("username constraint"))
                    .thenReturn(savedUser);

            User result = userService.createUser(userId, email);

            assertThat(result).isNotNull();
            verify(userRepository, times(2)).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Get User Profile Tests")
    class GetUserProfileTests {

        @Test
        @DisplayName("Should return user profile when found")
        void shouldReturnUserProfileWhenFound() {
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .username("testuser")
                    .email("test@example.com")
                    .bio("Test bio")
                    .country("US")
                    .totalGames(10)
                    .totalWins(5)
                    .totalLosses(3)
                    .totalDraws(2)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(ratingService.getUserRatings(userId)).thenReturn(Collections.emptyList());

            UserProfileResponse response = userService.getUserProfile(userId);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(userId);
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getEmail()).isEqualTo("test@example.com");
            assertThat(response.getBio()).isEqualTo("Test bio");
            assertThat(response.getCountry()).isEqualTo("US");
            assertThat(response.getTotalGames()).isEqualTo(10);
            assertThat(response.getTotalWins()).isEqualTo(5);
            assertThat(response.getTotalLosses()).isEqualTo(3);
            assertThat(response.getTotalDraws()).isEqualTo(2);
            assertThat(response.getWinRate()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should throw NotFoundException when user not found")
        void shouldThrowNotFoundExceptionWhenUserNotFound() {
            UUID userId = UUID.randomUUID();

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserProfile(userId))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get User Profile Public Tests")
    class GetUserProfilePublicTests {

        @Test
        @DisplayName("Should return public user profile when found")
        void shouldReturnPublicUserProfileWhenFound() {
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .username("testuser")
                    .email("test@example.com")
                    .bio("Test bio")
                    .country("US")
                    .totalGames(10)
                    .totalWins(5)
                    .totalLosses(3)
                    .totalDraws(2)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(ratingService.getUserRatings(userId)).thenReturn(Collections.emptyList());

            UserProfilePublicResponse response = userService.getUserProfilePublic(userId);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(userId);
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getTotalGames()).isEqualTo(10);
            // Public response should not contain email
        }

        @Test
        @DisplayName("Should throw NotFoundException when user not found")
        void shouldThrowNotFoundExceptionWhenUserNotFound() {
            UUID userId = UUID.randomUUID();

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserProfilePublic(userId))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Update Profile Tests")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should update profile successfully")
        void shouldUpdateProfileSuccessfully() {
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .username("oldusername")
                    .email("test@example.com")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .username("newusername")
                    .bio("New bio")
                    .country("UK")
                    .avatarUrl("http://example.com/avatar.jpg")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.existsByUsername("newusername")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(ratingService.getUserRatings(userId)).thenReturn(Collections.emptyList());

            UserProfileResponse response = userService.updateProfile(userId, request);

            assertThat(response).isNotNull();
            
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getUsername()).isEqualTo("newusername");
            assertThat(savedUser.getBio()).isEqualTo("New bio");
            assertThat(savedUser.getCountry()).isEqualTo("UK");
            assertThat(savedUser.getAvatarUrl()).isEqualTo("http://example.com/avatar.jpg");
        }

        @Test
        @DisplayName("Should throw ConflictException when username is taken")
        void shouldThrowConflictExceptionWhenUsernameTaken() {
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .username("oldusername")
                    .email("test@example.com")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .username("takenusername")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.existsByUsername("takenusername")).thenReturn(true);

            assertThatThrownBy(() -> userService.updateProfile(userId, request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Username already taken");
        }

        @Test
        @DisplayName("Should throw NotFoundException when user not found")
        void shouldThrowNotFoundExceptionWhenUserNotFound() {
            UUID userId = UUID.randomUUID();
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .username("newusername")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateProfile(userId, request))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Should not check username uniqueness if username unchanged")
        void shouldNotCheckUsernameUniquenessIfUsernameUnchanged() {
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .username("sameusername")
                    .email("test@example.com")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .username("sameusername")
                    .bio("Updated bio")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(ratingService.getUserRatings(userId)).thenReturn(Collections.emptyList());

            userService.updateProfile(userId, request);

            verify(userRepository, never()).existsByUsername(any());
        }
    }

    @Nested
    @DisplayName("Update Game Stats Tests")
    class UpdateGameStatsTests {

        @Test
        @DisplayName("Should update stats for win")
        void shouldUpdateStatsForWin() {
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .username("testuser")
                    .email("test@example.com")
                    .totalGames(10)
                    .totalWins(5)
                    .totalLosses(3)
                    .totalDraws(2)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            userService.updateGameStats(userId, "WIN");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getTotalGames()).isEqualTo(11);
            assertThat(savedUser.getTotalWins()).isEqualTo(6);
        }

        @Test
        @DisplayName("Should update stats for loss")
        void shouldUpdateStatsForLoss() {
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .username("testuser")
                    .email("test@example.com")
                    .totalGames(10)
                    .totalWins(5)
                    .totalLosses(3)
                    .totalDraws(2)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            userService.updateGameStats(userId, "LOSS");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getTotalGames()).isEqualTo(11);
            assertThat(savedUser.getTotalLosses()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should update stats for draw")
        void shouldUpdateStatsForDraw() {
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .username("testuser")
                    .email("test@example.com")
                    .totalGames(10)
                    .totalWins(5)
                    .totalLosses(3)
                    .totalDraws(2)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            userService.updateGameStats(userId, "DRAW");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getTotalGames()).isEqualTo(11);
            assertThat(savedUser.getTotalDraws()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Get User Tests")
    class GetUserTests {

        @Test
        @DisplayName("Should return user when found")
        void shouldReturnUserWhenFound() {
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .username("testuser")
                    .email("test@example.com")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            User result = userService.getUser(userId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should throw NotFoundException when user not found")
        void shouldThrowNotFoundExceptionWhenUserNotFound() {
            UUID userId = UUID.randomUUID();

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUser(userId))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
