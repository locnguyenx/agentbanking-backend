package com.agentbanking.auth.application.usecase;

import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.domain.model.UserStatus;
import com.agentbanking.auth.domain.port.out.PasswordHasher;
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
class ManageUserUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    private ManageUserUseCaseImpl manageUserUseCase;

    private UserRecord testUser;

    @BeforeEach
    void setUp() {
        manageUserUseCase = new ManageUserUseCaseImpl(userRepository, passwordHasher);
        
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
    void createUser_withValidData_shouldCreateUser() {
        // Arrange
        UserRecord newUser = new UserRecord(
                null,
                "newuser",
                "new@example.com",
                "password123",
                "New User",
                UserStatus.ACTIVE,
                java.util.Set.of(),
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                "system"
        );
        
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordHasher.hash("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(UserRecord.class))).thenReturn(testUser);

        // Act
        UserRecord created = manageUserUseCase.createUser(newUser);

        // Assert
        assertNotNull(created);
        verify(userRepository).save(any(UserRecord.class));
    }

    @Test
    void createUser_withExistingUsername_shouldThrowException() {
        // Arrange
        UserRecord newUser = new UserRecord(
                null,
                "testuser", // existing username
                "new@example.com",
                "password123",
                "New User",
                UserStatus.ACTIVE,
                java.util.Set.of(),
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                "system"
        );
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            manageUserUseCase.createUser(newUser)
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserById_withExistingId_shouldReturnUser() {
        // Arrange
        UUID userId = testUser.userId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act
        UserRecord found = manageUserUseCase.getUserById(userId);

        // Assert
        assertNotNull(found);
        assertEquals(testUser.username(), found.username());
    }

    @Test
    void getUserById_withNonExistentId_shouldReturnNull() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act
        UserRecord found = manageUserUseCase.getUserById(nonExistentId);

        // Assert
        assertNull(found);
    }

    @Test
    void updateUser_withValidData_shouldUpdateUser() {
        // Arrange
        UUID userId = testUser.userId();
        UserRecord updateData = new UserRecord(
                userId,
                "updateduser",
                "updated@example.com",
                "hashedPassword",
                "Updated User",
                UserStatus.ACTIVE,
                java.util.Set.of(),
                0,
                null,
                testUser.passwordChangedAt(),
                testUser.passwordExpiresAt(),
                testUser.createdAt(),
                LocalDateTime.now(),
                testUser.lastLoginAt(),
                testUser.createdBy()
        );
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("updateduser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("updated@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(UserRecord.class))).thenReturn(updateData);

        // Act
        UserRecord updated = manageUserUseCase.updateUser(userId, updateData);

        // Assert
        assertNotNull(updated);
        assertEquals("updateduser", updated.username());
        verify(userRepository).save(any(UserRecord.class));
    }

    @Test
    void deleteUser_withExistingId_shouldDeleteUser() {
        // Arrange
        UUID userId = testUser.userId();
        when(userRepository.deleteById(userId)).thenReturn(true);

        // Act
        boolean deleted = manageUserUseCase.deleteUser(userId);

        // Assert
        assertTrue(deleted);
        verify(userRepository).deleteById(userId);
    }

    @Test
    void lockUser_withExistingId_shouldLockUser() {
        // Arrange
        UUID userId = testUser.userId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(UserRecord.class))).thenReturn(testUser);

        // Act
        boolean locked = manageUserUseCase.lockUser(userId);

        // Assert
        assertTrue(locked);
        verify(userRepository).save(any(UserRecord.class));
    }

    @Test
    void resetPassword_withExistingId_shouldResetPassword() {
        // Arrange
        UUID userId = testUser.userId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordHasher.hash("newpassword")).thenReturn("new-hashed-password");
        when(userRepository.save(any(UserRecord.class))).thenReturn(testUser);

        // Act
        boolean reset = manageUserUseCase.resetPassword(userId, "newpassword");

        // Assert
        assertTrue(reset);
        verify(userRepository).save(any(UserRecord.class));
    }
}