package com.agentbanking.auth.domain.service;

import com.agentbanking.auth.domain.model.AuthenticationResult;
import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.domain.model.UserStatus;
import com.agentbanking.auth.domain.port.out.PasswordHasher;
import com.agentbanking.auth.domain.port.out.TokenProvider;
import com.agentbanking.auth.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private PasswordHasher passwordHasher;

    private AuthenticationService authenticationService;

    private UserRecord testUser;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(userRepository, tokenProvider, passwordHasher);
        
        testUser = new UserRecord(
                UUID.randomUUID(),
                "testuser",
                "test@example.com",
                "hashedPassword",
                "Test User",
                UserStatus.ACTIVE,
                java.util.Set.of(),
                0,
                null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(90),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                "system"
        );
    }

    @Test
    void authenticate_withValidCredentials_shouldReturnTokens() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordHasher.matches("password123", "hashedPassword")).thenReturn(true);
        when(tokenProvider.generateAccessToken(testUser)).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(testUser)).thenReturn("refresh-token");
        when(userRepository.save(any(UserRecord.class))).thenReturn(testUser);

        // Act
        AuthenticationResult result = authenticationService.authenticate("testuser", "password123");

        // Assert
        assertNotNull(result);
        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
        assertEquals(900L, result.expiresIn());
        verify(userRepository).save(any(UserRecord.class));
    }

    @Test
    void authenticate_withInvalidPassword_shouldThrowException() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordHasher.matches("wrongpassword", "hashedPassword")).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            authenticationService.authenticate("testuser", "wrongpassword")
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticate_withNonExistentUser_shouldThrowException() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            authenticationService.authenticate("nonexistent", "password123")
        );
    }

    @Test
    void refreshToken_withValidToken_shouldReturnNewTokens() {
        // Arrange
        UUID userId = testUser.userId();
        when(tokenProvider.validateRefreshToken("valid-refresh-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateAccessToken(testUser)).thenReturn("new-access-token");
        when(tokenProvider.generateRefreshToken(testUser)).thenReturn("new-refresh-token");

        // Act
        AuthenticationResult result = authenticationService.refreshToken("valid-refresh-token");

        // Assert
        assertNotNull(result);
        assertEquals("new-access-token", result.accessToken());
        assertEquals("new-refresh-token", result.refreshToken());
    }

    @Test
    void refreshToken_withInvalidToken_shouldThrowException() {
        // Arrange
        when(tokenProvider.validateRefreshToken("invalid-token")).thenThrow(new SecurityException("Invalid token"));

        // Act & Assert
        assertThrows(SecurityException.class, () -> 
            authenticationService.refreshToken("invalid-token")
        );
    }
}