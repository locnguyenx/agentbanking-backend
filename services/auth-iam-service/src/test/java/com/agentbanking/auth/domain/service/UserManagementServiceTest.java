package com.agentbanking.auth.domain.service;

import com.agentbanking.auth.domain.model.InvalidPasswordException;
import com.agentbanking.auth.domain.model.UserAlreadyExistsException;
import com.agentbanking.auth.domain.model.UserNotFoundException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TemporaryPasswordGenerator temporaryPasswordGenerator;

    private UserManagementService userManagementService;

    @BeforeEach
    void setUp() {
        userManagementService = new UserManagementService(userRepository, passwordHasher, temporaryPasswordGenerator);
    }

    @Test
    void createAgentUser_withNewAgent_shouldCreateExternalUser() {
        UUID agentId = UUID.randomUUID();
        String agentCode = "AGENT001";
        String phone = "+60123456789";
        String email = "agent@example.com";
        String businessName = "Agent Business";

        String tempPassword = "Temp1234";
        when(temporaryPasswordGenerator.generate()).thenReturn(tempPassword);
        when(passwordHasher.hash(tempPassword)).thenReturn("hashedTempPassword");
        when(userRepository.findByAgentId(agentId)).thenReturn(Optional.empty());
        when(userRepository.save(any(UserRecord.class))).thenAnswer(i -> i.getArgument(0));

        UserRecord result = userManagementService.createAgentUser(agentId, agentCode, phone, email, businessName);

        assertNotNull(result);
        assertEquals(agentCode, result.username());
        assertEquals(email, result.email());
        assertEquals(phone, result.phone());
        assertEquals(businessName, result.fullName());
        assertEquals(UserType.EXTERNAL, result.userType());
        assertEquals(agentId, result.agentId());
        assertEquals(agentCode, result.agentCode());
        assertTrue(result.mustChangePassword());
        assertNotNull(result.temporaryPasswordExpiresAt());
        assertTrue(result.temporaryPasswordExpiresAt().isAfter(LocalDateTime.now()));
        assertTrue(result.permissions().contains("AGENT"));
        assertEquals("SYSTEM", result.createdBy());

        verify(userRepository).save(any(UserRecord.class));
    }

    @Test
    void createAgentUser_withExistingAgent_shouldThrowException() {
        UUID agentId = UUID.randomUUID();
        UserRecord existingUser = new UserRecord(
                UUID.randomUUID(), "AGENT001", "agent@example.com", "+60123456789",
                "hashedPassword", "Existing Agent", UserStatus.ACTIVE, UserType.EXTERNAL,
                agentId, "AGENT001", false, null, Set.of("AGENT"), 0, null,
                LocalDateTime.now(), LocalDateTime.now().plusDays(90),
                LocalDateTime.now(), LocalDateTime.now(), null, "SYSTEM"
        );

        when(userRepository.findByAgentId(agentId)).thenReturn(Optional.of(existingUser));

        assertThrows(UserAlreadyExistsException.class, () ->
                userManagementService.createAgentUser(agentId, "AGENT001", "+60123456789", "agent@example.com", "Agent Business")
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void createAgentUser_withNoPhone_shouldUseEmailForNotification() {
        UUID agentId = UUID.randomUUID();
        String agentCode = "AGENT002";
        String email = "agent2@example.com";
        String businessName = "Agent Business 2";

        String tempPassword = "Temp5678";
        when(temporaryPasswordGenerator.generate()).thenReturn(tempPassword);
        when(passwordHasher.hash(tempPassword)).thenReturn("hashedTempPassword");
        when(userRepository.findByAgentId(agentId)).thenReturn(Optional.empty());
        when(userRepository.save(any(UserRecord.class))).thenAnswer(i -> i.getArgument(0));

        UserRecord result = userManagementService.createAgentUser(agentId, agentCode, null, email, businessName);

        assertNotNull(result);
        assertNull(result.phone());
        assertEquals(email, result.email());
    }

    @Test
    void createAgentUser_tempPasswordExpiresIn3Days() {
        UUID agentId = UUID.randomUUID();
        LocalDateTime beforeCreate = LocalDateTime.now();

        String tempPassword = "Temp1234";
        when(temporaryPasswordGenerator.generate()).thenReturn(tempPassword);
        when(passwordHasher.hash(tempPassword)).thenReturn("hashedTempPassword");
        when(userRepository.findByAgentId(agentId)).thenReturn(Optional.empty());
        when(userRepository.save(any(UserRecord.class))).thenAnswer(i -> i.getArgument(0));

        UserRecord result = userManagementService.createAgentUser(
                agentId, "AGENT001", "+60123456789", "agent@example.com", "Agent Business"
        );

        assertNotNull(result.temporaryPasswordExpiresAt());
        assertTrue(result.temporaryPasswordExpiresAt().isAfter(beforeCreate.plusDays(2)));
        assertTrue(result.temporaryPasswordExpiresAt().isBefore(beforeCreate.plusDays(4)));
    }

    @Test
    void createAgentUser_returnsPlainTextTempPassword() {
        UUID agentId = UUID.randomUUID();
        String tempPassword = "Secret123";

        when(temporaryPasswordGenerator.generate()).thenReturn(tempPassword);
        when(passwordHasher.hash(tempPassword)).thenReturn("hashedSecret123");
        when(userRepository.findByAgentId(agentId)).thenReturn(Optional.empty());
        when(userRepository.save(any(UserRecord.class))).thenAnswer(i -> i.getArgument(0));

        UserRecord result = userManagementService.createAgentUser(
                agentId, "AGENT001", "+60123456789", "agent@example.com", "Agent Business"
        );

        assertEquals(tempPassword, result.passwordHash());
    }

    @Test
    void findByAgentId_withExistingAgent_shouldReturnUser() {
        UUID agentId = UUID.randomUUID();
        UserRecord existingUser = new UserRecord(
                UUID.randomUUID(), "AGENT001", "agent@example.com", "+60123456789",
                "hashedPassword", "Existing Agent", UserStatus.ACTIVE, UserType.EXTERNAL,
                agentId, "AGENT001", false, null, Set.of("AGENT"), 0, null,
                LocalDateTime.now(), LocalDateTime.now().plusDays(90),
                LocalDateTime.now(), LocalDateTime.now(), null, "SYSTEM"
        );

        when(userRepository.findByAgentId(agentId)).thenReturn(Optional.of(existingUser));

        Optional<UserRecord> result = userManagementService.findByAgentId(agentId);

        assertTrue(result.isPresent());
        assertEquals(existingUser.userId(), result.get().userId());
    }

    @Test
    void findByAgentId_withNonExistingAgent_shouldReturnEmpty() {
        UUID agentId = UUID.randomUUID();
        when(userRepository.findByAgentId(agentId)).thenReturn(Optional.empty());

        Optional<UserRecord> result = userManagementService.findByAgentId(agentId);

        assertTrue(result.isEmpty());
    }

    @Test
    void changePassword_withCorrectCurrentPassword_shouldUpdatePassword() {
        UUID userId = UUID.randomUUID();
        String currentPassword = "OldPass123";
        String newPassword = "NewPass456";
        UserRecord existingUser = new UserRecord(
                userId, "testuser", "test@example.com", "+60123456789",
                "hashedOldPassword", "Test User", UserStatus.ACTIVE, UserType.INTERNAL,
                null, null, true, LocalDateTime.now().plusDays(3), Set.of("USER"), 0, null,
                LocalDateTime.now(), LocalDateTime.now().plusDays(90),
                LocalDateTime.now(), LocalDateTime.now(), null, "system"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordHasher.matches(currentPassword, "hashedOldPassword")).thenReturn(true);
        when(passwordHasher.hash(newPassword)).thenReturn("hashedNewPassword");

        boolean result = userManagementService.changePassword(userId, currentPassword, newPassword);

        assertTrue(result);
        verify(userRepository).updatePassword(eq(userId), eq("hashedNewPassword"), any(LocalDateTime.class));
        verify(userRepository).clearTempPasswordFlags(userId);
    }

    @Test
    void changePassword_withIncorrectCurrentPassword_shouldThrowException() {
        UUID userId = UUID.randomUUID();
        String currentPassword = "WrongPassword";
        String newPassword = "NewPass456";
        UserRecord existingUser = new UserRecord(
                userId, "testuser", "test@example.com", "+60123456789",
                "hashedOldPassword", "Test User", UserStatus.ACTIVE, UserType.INTERNAL,
                null, null, true, LocalDateTime.now().plusDays(3), Set.of("USER"), 0, null,
                LocalDateTime.now(), LocalDateTime.now().plusDays(90),
                LocalDateTime.now(), LocalDateTime.now(), null, "system"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordHasher.matches(currentPassword, "hashedOldPassword")).thenReturn(false);

        assertThrows(InvalidPasswordException.class, () ->
                userManagementService.changePassword(userId, currentPassword, newPassword)
        );
    }

    @Test
    void changePassword_shouldClearMustChangePasswordFlag() {
        UUID userId = UUID.randomUUID();
        String currentPassword = "OldPass123";
        String newPassword = "NewPass456";
        UserRecord existingUser = new UserRecord(
                userId, "testuser", "test@example.com", "+60123456789",
                "hashedOldPassword", "Test User", UserStatus.ACTIVE, UserType.INTERNAL,
                null, null, true, LocalDateTime.now().plusDays(3), Set.of("USER"), 0, null,
                LocalDateTime.now(), LocalDateTime.now().plusDays(90),
                LocalDateTime.now(), LocalDateTime.now(), null, "system"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordHasher.matches(currentPassword, "hashedOldPassword")).thenReturn(true);
        when(passwordHasher.hash(newPassword)).thenReturn("hashedNewPassword");

        userManagementService.changePassword(userId, currentPassword, newPassword);

        verify(userRepository).clearTempPasswordFlags(userId);
    }

    @Test
    void changePassword_withNonExistentUser_shouldThrowException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                userManagementService.changePassword(userId, "OldPass123", "NewPass456")
        );
    }
}
