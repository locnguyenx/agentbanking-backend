package com.agentbanking.auth.domain.service;

import com.agentbanking.auth.domain.model.AuthBusinessException;
import com.agentbanking.auth.domain.model.OtpData;
import com.agentbanking.auth.domain.model.OtpRequestedEvent;
import com.agentbanking.auth.domain.model.PasswordResetConfirmedEvent;
import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.domain.model.UserStatus;
import com.agentbanking.auth.domain.port.out.NotificationPublisher;
import com.agentbanking.auth.domain.port.out.OtpStore;
import com.agentbanking.auth.domain.port.out.PasswordHasher;
import com.agentbanking.auth.domain.port.out.UserRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public class PasswordResetService {

    private static final int OTP_TTL_SECONDS = 300;
    private static final int MAX_OTP_ATTEMPTS = 3;
    private static final int TEMPORARY_PASSWORD_VALIDITY_HOURS = 24;

    private final OtpStore otpStore;
    private final NotificationPublisher notificationPublisher;
    private final PasswordHasher passwordHasher;
    private final UserRepository userRepository;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;

    public PasswordResetService(OtpStore otpStore,
                                 NotificationPublisher notificationPublisher,
                                 PasswordHasher passwordHasher,
                                 UserRepository userRepository,
                                 TemporaryPasswordGenerator temporaryPasswordGenerator) {
        this.otpStore = otpStore;
        this.notificationPublisher = notificationPublisher;
        this.passwordHasher = passwordHasher;
        this.userRepository = userRepository;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
    }

    public void requestReset(String username) {
        UserRecord user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthBusinessException("ERR_AUTH_USER_NOT_FOUND", "User not found", "RETRY"));

        if (user.status() == UserStatus.DELETED || user.status() == UserStatus.INACTIVE) {
            throw new AuthBusinessException("ERR_AUTH_USER_INACTIVE", "User account is not active", "RETRY");
        }

        String temporaryPassword = generateTemporaryPassword();
        String hashedOtp = passwordHasher.hash(temporaryPassword);
        
        otpStore.storeOtp(username, hashedOtp, OTP_TTL_SECONDS);

        OtpRequestedEvent event = new OtpRequestedEvent(
                UUID.randomUUID(),
                "OTP_REQUESTED",
                Instant.now(),
                new OtpRequestedEvent.OtpRequestedData(
                        user.userId(),
                        user.username(),
                        user.email(),
                        user.phone(),
                        temporaryPassword,
                        "PASSWORD_RESET"
                )
        );
        notificationPublisher.publishOtpRequested(event);
    }

    public void verifyReset(String username, String otp, String newPassword) {
        UserRecord user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthBusinessException("ERR_AUTH_USER_NOT_FOUND", "User not found", "RETRY"));

        OtpData otpData = otpStore.retrieveOtp(username);
        if (otpData == null) {
            throw new AuthBusinessException("ERR_AUTH_OTP_EXPIRED", "OTP has expired", "RETRY");
        }

        if (otpData.attempts() >= MAX_OTP_ATTEMPTS) {
            otpStore.deleteOtp(username);
            throw new AuthBusinessException("ERR_AUTH_OTP_MAX_ATTEMPTS", "Maximum OTP attempts exceeded", "RETRY");
        }

        if (!passwordHasher.matches(otp, otpData.hashedOtp())) {
            otpStore.incrementAttempts(username);
            throw new AuthBusinessException("ERR_AUTH_OTP_INVALID", "Invalid OTP", "RETRY");
        }

        String hashedPassword = passwordHasher.hash(newPassword);

        UserRecord updatedUser = new UserRecord(
                user.userId(),
                user.username(),
                user.email(),
                user.phone(),
                hashedPassword,
                user.fullName(),
                user.status(),
                user.userType(),
                user.agentId(),
                user.agentCode(),
                false,
                null,
                user.permissions(),
                0,
                null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(90),
                user.createdAt(),
                LocalDateTime.now(),
                user.lastLoginAt(),
                user.createdBy()
        );
        userRepository.save(updatedUser);

        otpStore.deleteOtp(username);

        PasswordResetConfirmedEvent event = PasswordResetConfirmedEvent.create(
                user.userId(),
                user.username(),
                user.email(),
                user.phone()
        );
        notificationPublisher.publishPasswordResetConfirmed(event);
    }

    public String generateTemporaryPassword() {
        return temporaryPasswordGenerator.generate();
    }
}
