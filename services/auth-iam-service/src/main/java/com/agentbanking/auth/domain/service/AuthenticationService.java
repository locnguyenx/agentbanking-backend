package com.agentbanking.auth.domain.service;

import com.agentbanking.auth.domain.model.AuthenticationResult;
import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.domain.port.in.AuthenticateUserUseCase;
import com.agentbanking.auth.domain.port.out.PasswordHasher;
import com.agentbanking.auth.domain.port.out.TokenProvider;
import com.agentbanking.auth.domain.port.out.UserRepository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain service for user authentication.
 * Uses outbound ports to abstract infrastructure concerns (password hashing, token generation).
 */
public class AuthenticationService implements AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final PasswordHasher passwordHasher;

    public AuthenticationService(UserRepository userRepository, TokenProvider tokenProvider, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public AuthenticationResult authenticate(String username, String password) {
        UserRecord user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordHasher.matches(password, user.passwordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        // Generate tokens using the token provider port
        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);

        // Update last login
        UserRecord updatedUser = new UserRecord(
                user.userId(),
                user.username(),
                user.email(),
                user.passwordHash(),
                user.fullName(),
                user.status(),
                user.permissions(),
                user.failedLoginAttempts(),
                user.lockedUntil(),
                user.passwordChangedAt(),
                user.passwordExpiresAt(),
                user.createdAt(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                user.createdBy()
        );
        userRepository.save(updatedUser);

        return new AuthenticationResult(accessToken, refreshToken, 900L);
    }

    @Override
    public AuthenticationResult refreshToken(String refreshToken) {
        // Validate refresh token using the token provider port
        UUID userId = tokenProvider.validateRefreshToken(refreshToken);
        UserRecord user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        String accessToken = tokenProvider.generateAccessToken(user);
        String newRefreshToken = tokenProvider.generateRefreshToken(user);

        return new AuthenticationResult(accessToken, newRefreshToken, 900L);
    }
}