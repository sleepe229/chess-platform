package com.chess.auth.repo;

import com.chess.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByAuthProviderAndProviderUserId(String authProvider, String providerUserId);

    boolean existsByEmail(String email);
}
