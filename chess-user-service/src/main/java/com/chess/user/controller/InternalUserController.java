package com.chess.user.controller;

import com.chess.user.dto.CreateUserInternalRequest;
import com.chess.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Internal API for auth-service to create user profile synchronously on registration.
 * Not exposed via gateway; only reachable inside the Docker network.
 */
@Slf4j
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<Map<String, String>> createUser(@Valid @RequestBody CreateUserInternalRequest request) {
        log.info("Internal create user: userId={}, email={}", request.getUserId(), request.getEmail());
        userService.createUser(request.getUserId(), request.getEmail());
        return ResponseEntity.ok(Map.of("userId", request.getUserId().toString()));
    }
}
