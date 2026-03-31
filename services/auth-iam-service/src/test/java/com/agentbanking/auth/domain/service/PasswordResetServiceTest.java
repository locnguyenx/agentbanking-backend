package com.agentbanking.auth.domain.service;

import com.agentbanking.auth.domain.model.AuthBusinessException;
import com.agentbanking.auth.domain.model.OtpData;
import com.agentbanking.auth.domain.model.OtpRequestedEvent;
import com.agentbanking.auth.domain.model.PasswordResetConfirmedEvent;
import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.domain.model.UserStatus;
import com.agentbanking.auth.domain.model.UserType;
import com.agentbanking.auth.domain.port.out.NotificationPublisher;
import com.agentbanking.auth.domain.port.out.OtpStore;
import com.agentbanking.auth.domain.port.out.PasswordHasher;
import com.agentbanking.auth.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class PasswordResetServiceTest {

    @Mock
    private OtpStore otpStore;

    @Mock
    private NotificationPublisher notificationPublisher;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TemporaryPasswordGenerator temporaryPasswordGenerator;

    private PasswordResetService passwordResetService;

    private UserRecord testUser;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                otpStore,
                notificationPublisher,
                passwordHasher,
                userRepository,
                temporaryPasswordGenerator
        );

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
    void requestReset_withValidUser_shouldGenerateOtpAndPublishEvent() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordHasher.hash(anyString())).thenReturn("hashedOtp");

        passwordResetService.requestReset("testuser");

        verify(otpStore).storeOtp(eq("testuser"), eq("hashedOtp"), eq(600));
        verify(notificationPublisher).publishOtpRequested(any(OtpRequestedEvent.class));
    }

    @Test
    void requestReset_withNonExistentUser_shouldThrowException() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(AuthBusinessException.class, () ->
                passwordResetService.requestReset("nonexistent")
        );
    }

    @Test
    void requestReset_withInactiveUser_shouldThrowException() {
        UserRecord inactiveUser = new UserRecord(
                testUser.userId(), testUser.username(), testUser.email(), testUser.phone(),
                testUser.passwordHash(), testUser.fullName(), UserStatus.INACTIVE,
                testUser.userType(), testUser.agentId(), testUser.agentCode(),
                testUser.mustChangePassword(), testUser.temporaryPasswordExpiresAt(),
                testUser.permissions(), testUser.failedLoginAttempts(), testUser.lockedUntil(),
                testUser.passwordChangedAt(), testUser.passwordExpiresAt(),
                testUser.createdAt(), testUser.updatedAt(), testUser.lastLoginAt(), testUser.createdBy()
        );
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(inactiveUser));

        AuthBusinessException exception = assertThrows(AuthBusinessException.class, () ->
                passwordResetService.requestReset("testuser")
        );

        assertEquals("ERR_AUTH_USER_INACTIVE", exception.getErrorCode());
    }

    @Test
    void requestReset_withDeletedUser_shouldThrowException() {
        UserRecord deletedUser = new UserRecord(
                testUser.userId(), testUser.username(), testUser.email(), testUser.phone(),
                testUser.passwordHash(), testUser.fullName(), UserStatus.DELETED,
                testUser.userType(), testUser.agentId(), testUser.agentCode(),
                testUser.mustChangePassword(), testUser.temporaryPasswordExpiresAt(),
                testUser.permissions(), testUser.failedLoginAttempts(), testUser.lockedUntil(),
                testUser.passwordChangedAt(), testUser.passwordExpiresAt(),
                testUser.createdAt(), testUser.updatedAt(), testUser.lastLoginAt(), testUser.createdBy()
        );
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(deletedUser));

        AuthBusinessException exception = assertThrows(AuthBusinessException.class, () ->
                passwordResetService.requestReset("testuser")
        );

        assertEquals("ERR_AUTH_USER_INACTIVE", exception.getErrorCode());
    }

    @Test
    void verifyReset_withValidOtp_shouldResetPasswordAndPublishEvent() {
        String otp = "123456";
        String hashedOtp = "hashedOtp";
        String newPassword = "NewPass123";

        OtpData otpData = new OtpData(hashedOtp, 0, LocalDateTime.now());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(otpStore.retrieveOtp("testuser")).thenReturn(otpData);
        when(passwordHasher.matches(otp, hashedOtp)).thenReturn(true);
        when(passwordHasher.hash(newPassword)).thenReturn("hashedNewPassword");
        when(userRepository.save(any(UserRecord.class))).thenReturn(testUser);

        passwordResetService.verifyReset("testuser", otp, newPassword);

        verify(userRepository).save(any(UserRecord.class));
        verify(otpStore).deleteOtp("testuser");
        verify(notificationPublisher).publishPasswordResetConfirmed(any(PasswordResetConfirmedEvent.class));
    }

    @Test
    void verifyReset_withExpiredOtp_shouldThrowException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(otpStore.retrieveOtp("testuser")).thenReturn(null);

        AuthBusinessException exception = assertThrows(AuthBusinessException.class, () ->
                passwordResetService.verifyReset("testuser", "123456", "NewPass123")
        );

        assertEquals("ERR_AUTH_OTP_EXPIRED", exception.getErrorCode());
    }

    @Test
    void verifyReset_withInvalidOtp_shouldIncrementAttempts() {
        String otp = "wrongOtp";
        String hashedOtp = "hashedOtp";

        OtpData otpData = new OtpData(hashedOtp, 0, LocalDateTime.now());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(otpStore.retrieveOtp("testuser")).thenReturn(otpData);
        when(passwordHasher.matches(otp, hashedOtp)).thenReturn(false);

        AuthBusinessException exception = assertThrows(AuthBusinessException.class, () ->
                passwordResetService.verifyReset("testuser", otp, "NewPass123")
        );

        assertEquals("ERR_AUTH_OTP_INVALID", exception.getErrorCode());
        verify(otpStore).incrementAttempts("testuser");
    }

    @Test
    void verifyReset_withMaxAttemptsExceeded_shouldDeleteOtpAndThrowException() {
        String otp = "123456";
        String hashedOtp = "hashedOtp";

        OtpData otpData = new OtpData(hashedOtp, 3, LocalDateTime.now());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(otpStore.retrieveOtp("testuser")).thenReturn(otpData);

        AuthBusinessException exception = assertThrows(AuthBusinessException.class, () ->
                passwordResetService.verifyReset("testuser", otp, "NewPass123")
        );

        assertEquals("ERR_AUTH_OTP_MAX_ATTEMPTS", exception.getErrorCode());
        verify(otpStore).deleteOtp("testuser");
    }

    @Test
    void generateTemporaryPassword_shouldReturnValidPassword() {
        when(temporaryPasswordGenerator.generate()).thenReturn("TempPass123");

        String password = passwordResetService.generateTemporaryPassword();

        assertEquals("TempPass123", password);
        verify(temporaryPasswordGenerator).generate();
    }

    @Test
    void verifyReset_shouldClearMustChangePasswordFlag() {
        UserRecord userWithMustChange = new UserRecord(
                testUser.userId(), testUser.username(), testUser.email(), testUser.phone(),
                testUser.passwordHash(), testUser.fullName(), UserStatus.ACTIVE,
                testUser.userType(), testUser.agentId(), testUser.agentCode(),
                true, LocalDateTime.now().plusDays(3), testUser.permissions(),
                testUser.failedLoginAttempts(), testUser.lockedUntil(),
                testUser.passwordChangedAt(), testUser.passwordExpiresAt(),
                testUser.createdAt(), testUser.updatedAt(), testUser.lastLoginAt(), testUser.createdBy()
        );

        String otp = "123456";
        String hashedOtp = "hashedOtp";
        String newPassword = "NewPass123";

        OtpData otpData = new OtpData(hashedOtp, 0, LocalDateTime.now());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(userWithMustChange));
        when(otpStore.retrieveOtp("testuser")).thenReturn(otpData);
        when(passwordHasher.matches(otp, hashedOtp)).thenReturn(true);
        when(passwordHasher.hash(newPassword)).thenReturn("hashedNewPassword");

        ArgumentCaptor<UserRecord> userCaptor = ArgumentCaptor.forClass(UserRecord.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        passwordResetService.verifyReset("testuser", otp, newPassword);

        UserRecord savedUser = userCaptor.getValue();
        assertFalse(savedUser.mustChangePassword());
        assertNull(savedUser.temporaryPasswordExpiresAt());
    }

    @Test
    void verifyReset_shouldPublishConfirmationEventWithCorrectData() {
        String otp = "123456";
        String hashedOtp = "hashedOtp";
        String newPassword = "NewPass123";

        OtpData otpData = new OtpData(hashedOtp, 0, LocalDateTime.now());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(otpStore.retrieveOtp("testuser")).thenReturn(otpData);
        when(passwordHasher.matches(otp, hashedOtp)).thenReturn(true);
        when(passwordHasher.hash(newPassword)).thenReturn("hashedNewPassword");
        when(userRepository.save(any(UserRecord.class))).thenReturn(testUser);

        passwordResetService.verifyReset("testuser", otp, newPassword);

        ArgumentCaptor<PasswordResetConfirmedEvent> eventCaptor = ArgumentCaptor.forClass(PasswordResetConfirmedEvent.class);
        verify(notificationPublisher).publishPasswordResetConfirmed(eventCaptor.capture());

        PasswordResetConfirmedEvent event = eventCaptor.getValue();
        assertEquals(testUser.userId(), event.data().userId());
        assertEquals(testUser.username(), event.data().username());
    }
}
