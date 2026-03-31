package com.agentbanking.auth.application.usecase;

import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.domain.model.UserStatus;
import com.agentbanking.auth.domain.model.UserType;
import com.agentbanking.auth.domain.port.out.PasswordHasher;
import com.agentbanking.auth.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
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
                "+60123456789",
                "hashedPassword",
                "Test User",
                UserStatus.ACTIVE,
                UserType.INTERNAL,
                null,
                null,
                false,
                null,
                Set.of(),
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
        UserRecord newUser = new UserRecord(
                null,
                "newuser",
                "new@example.com",
                "+60123456789",
                "password123",
                "New User",
                UserStatus.ACTIVE,
                UserType.INTERNAL,
                null,
                null,
                false,
                null,
                Set.of(),
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

        UserRecord created = manageUserUseCase.createUser(newUser);

        assertNotNull(created);
        verify(userRepository).save(any(UserRecord.class));
    }

    @Test
    void createUser_withExistingUsername_shouldThrowException() {
        UserRecord newUser = new UserRecord(
                null,
                "testuser",
                "new@example.com",
                "+60123456789",
                "password123",
                "New User",
                UserStatus.ACTIVE,
                UserType.INTERNAL,
                null,
                null,
                false,
                null,
                Set.of(),
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

        assertThrows(IllegalArgumentException.class, () -> 
            manageUserUseCase.createUser(newUser)
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserById_withExistingId_shouldReturnUser() {
        UUID userId = testUser.userId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        UserRecord found = manageUserUseCase.getUserById(userId);

        assertNotNull(found);
        assertEquals(testUser.username(), found.username());
    }

    @Test
    void getUserById_withNonExistentId_shouldReturnNull() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        UserRecord found = manageUserUseCase.getUserById(nonExistentId);

        assertNull(found);
    }

    @Test
    void updateUser_withValidData_shouldUpdateUser() {
        UUID userId = testUser.userId();
        UserRecord updateData = new UserRecord(
                userId,
                "updateduser",
                "updated@example.com",
                "+60123456789",
                "hashedPassword",
                "Updated User",
                UserStatus.ACTIVE,
                UserType.INTERNAL,
                null,
                null,
                false,
                null,
                Set.of(),
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

        UserRecord updated = manageUserUseCase.updateUser(userId, updateData);

        assertNotNull(updated);
        assertEquals("updateduser", updated.username());
        verify(userRepository).save(any(UserRecord.class));
    }

    @Test
    void deleteUser_withExistingId_shouldDeleteUser() {
        UUID userId = testUser.userId();
        when(userRepository.deleteById(userId)).thenReturn(true);

        boolean deleted = manageUserUseCase.deleteUser(userId);

        assertTrue(deleted);
        verify(userRepository).deleteById(userId);
    }

    @Test
    void lockUser_withExistingId_shouldLockUser() {
        UUID userId = testUser.userId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(UserRecord.class))).thenReturn(testUser);

        boolean locked = manageUserUseCase.lockUser(userId);

        assertTrue(locked);
        verify(userRepository).save(any(UserRecord.class));
    }

    @Test
    void resetPassword_withExistingId_shouldResetPassword() {
        UUID userId = testUser.userId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordHasher.hash("newpassword")).thenReturn("new-hashed-password");
        when(userRepository.save(any(UserRecord.class))).thenReturn(testUser);

        boolean reset = manageUserUseCase.resetPassword(userId, "newpassword");

        assertTrue(reset);
        verify(userRepository).save(any(UserRecord.class));
    }
}
