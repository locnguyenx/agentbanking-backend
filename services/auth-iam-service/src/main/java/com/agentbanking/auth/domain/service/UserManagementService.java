package com.agentbanking.auth.domain.service;

import com.agentbanking.auth.domain.model.*;
import com.agentbanking.auth.domain.port.in.CreateAgentUserUseCase;
import com.agentbanking.auth.domain.port.in.ManageUserUseCase;
import com.agentbanking.auth.domain.port.out.PasswordHasher;
import com.agentbanking.auth.domain.port.out.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Domain service for user management.
 * Uses outbound port for password hashing to maintain hexagonal architecture.
 */
public class UserManagementService implements ManageUserUseCase, CreateAgentUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TemporaryPasswordGenerator tempPasswordGenerator;

    public UserManagementService(UserRepository userRepository, PasswordHasher passwordHasher, TemporaryPasswordGenerator tempPasswordGenerator) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tempPasswordGenerator = tempPasswordGenerator;
    }

    @Override
    public UserRecord createUser(UserRecord userRecord) {
        // Check if username or email already exists
        if (userRepository.findByUsername(userRecord.username()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.findByEmail(userRecord.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Hash the password using the outbound port
        String hashedPassword = passwordHasher.hash(userRecord.passwordHash());

        // Create user record with proper defaults
        UserRecord newUser = new UserRecord(
                UUID.randomUUID(),
                userRecord.username(),
                userRecord.email(),
                userRecord.phone(),
                hashedPassword,
                userRecord.fullName(),
                userRecord.status() != null ? userRecord.status() : UserStatus.ACTIVE,
                userRecord.userType(),
                userRecord.agentId(),
                userRecord.agentCode(),
                userRecord.mustChangePassword(),
                userRecord.temporaryPasswordExpiresAt(),
                userRecord.permissions(),
                0, // failedLoginAttempts
                null, // lockedUntil
                LocalDateTime.now(), // passwordChangedAt
                LocalDateTime.now().plusDays(90), // passwordExpiresAt (90 days)
                LocalDateTime.now(), // createdAt
                LocalDateTime.now(), // updatedAt
                null, // lastLoginAt
                userRecord.createdBy()
        );

        return userRepository.save(newUser);
    }

    @Override
    public UserRecord getUserById(UUID userId) {
        return userRepository.findById(userId).orElse(null);
    }

    @Override
    public UserRecord getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    public UserRecord getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Override
    public UserRecord updateUser(UUID userId, UserRecord userRecord) {
        UserRecord existing = userRepository.findById(userId).orElse(null);
        if (existing == null) {
            return null;
        }

        // Check if new username/email conflicts with other users
        if (!userRecord.username().equals(existing.username()) &&
            userRepository.findByUsername(userRecord.username()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (!userRecord.email().equals(existing.email()) &&
            userRepository.findByEmail(userRecord.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Update fields while preserving immutable ones
        UserRecord updatedUser = new UserRecord(
                existing.userId(),
                userRecord.username(),
                userRecord.email(),
                userRecord.phone() != null ? userRecord.phone() : existing.phone(),
                existing.passwordHash(),
                userRecord.fullName(),
                userRecord.status() != null ? userRecord.status() : existing.status(),
                userRecord.userType() != null ? userRecord.userType() : existing.userType(),
                userRecord.agentId() != null ? userRecord.agentId() : existing.agentId(),
                userRecord.agentCode() != null ? userRecord.agentCode() : existing.agentCode(),
                userRecord.mustChangePassword() != null ? userRecord.mustChangePassword() : existing.mustChangePassword(),
                userRecord.temporaryPasswordExpiresAt() != null ? userRecord.temporaryPasswordExpiresAt() : existing.temporaryPasswordExpiresAt(),
                userRecord.permissions() != null ? userRecord.permissions() : existing.permissions(),
                existing.failedLoginAttempts(),
                existing.lockedUntil(),
                existing.passwordChangedAt(),
                existing.passwordExpiresAt(),
                existing.createdAt(),
                LocalDateTime.now(),
                existing.lastLoginAt(),
                existing.createdBy()
        );

        return userRepository.save(updatedUser);
    }

    @Override
    public boolean deleteUser(UUID userId) {
        return userRepository.deleteById(userId);
    }

    @Override
    public boolean lockUser(UUID userId) {
        UserRecord user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        UserRecord lockedUser = new UserRecord(
                user.userId(),
                user.username(),
                user.email(),
                user.phone(),
                user.passwordHash(),
                user.fullName(),
                UserStatus.LOCKED,
                user.userType(),
                user.agentId(),
                user.agentCode(),
                user.mustChangePassword(),
                user.temporaryPasswordExpiresAt(),
                user.permissions(),
                user.failedLoginAttempts(),
                LocalDateTime.now().plusMinutes(30), // Lock for 30 minutes
                user.passwordChangedAt(),
                user.passwordExpiresAt(),
                user.createdAt(),
                LocalDateTime.now(),
                user.lastLoginAt(),
                user.createdBy()
        );

        userRepository.save(lockedUser);
        return true;
    }

    @Override
    public boolean unlockUser(UUID userId) {
        UserRecord user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        UserRecord unlockedUser = new UserRecord(
                user.userId(),
                user.username(),
                user.email(),
                user.phone(),
                user.passwordHash(),
                user.fullName(),
                UserStatus.ACTIVE,
                user.userType(),
                user.agentId(),
                user.agentCode(),
                user.mustChangePassword(),
                user.temporaryPasswordExpiresAt(),
                user.permissions(),
                0, // Reset failed login attempts
                null, // Clear lockout time
                user.passwordChangedAt(),
                user.passwordExpiresAt(),
                user.createdAt(),
                LocalDateTime.now(),
                user.lastLoginAt(),
                user.createdBy()
        );

        userRepository.save(unlockedUser);
        return true;
    }

    @Override
    public boolean resetPassword(UUID userId, String newPasswordHash) {
        UserRecord user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        // Hash the new password using the outbound port
        String hashedNewPassword = passwordHasher.hash(newPasswordHash);

        // Update password and related fields
        UserRecord updatedUser = new UserRecord(
                user.userId(),
                user.username(),
                user.email(),
                user.phone(),
                hashedNewPassword,
                user.fullName(),
                user.status(),
                user.userType(),
                user.agentId(),
                user.agentCode(),
                user.mustChangePassword(),
                user.temporaryPasswordExpiresAt(),
                user.permissions(),
                0, // Reset failed login attempts
                null, // Clear lockout time
                LocalDateTime.now(), // Password changed now
                LocalDateTime.now().plusDays(90), // Password expires in 90 days
                user.createdAt(),
                LocalDateTime.now(),
                user.lastLoginAt(),
                user.createdBy()
        );

        userRepository.save(updatedUser);
        return true;
    }

    @Override
    public List<UserRecord> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public UserRecord getProfile(UUID userId) {
        return getUserById(userId);
    }

    @Override
    public UserRecord createAgentUser(UUID agentId, String agentCode, String phone, String email, String businessName) {
        userRepository.findByAgentId(agentId).ifPresent(u -> {
            throw new UserAlreadyExistsException("User already exists for this agent");
        });

        String tempPassword = tempPasswordGenerator.generate();
        String hashedPassword = passwordHasher.hash(tempPassword);

        LocalDateTime tempPasswordExpiresAt = LocalDateTime.now().plusDays(3);

        String effectiveEmail = email != null ? email : agentCode + "@agent.local";

        UserRecord user = new UserRecord(
                UUID.randomUUID(),
                agentCode,
                effectiveEmail,
                phone,
                hashedPassword,
                businessName,
                UserStatus.ACTIVE,
                UserType.EXTERNAL,
                agentId,
                agentCode,
                true,
                tempPasswordExpiresAt,
                Set.of("AGENT"),
                0,
                null,
                LocalDateTime.now(),
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                "SYSTEM"
        );

        userRepository.save(user);

        return new UserRecord(
                user.userId(), user.username(), user.email(), user.phone(), tempPassword,
                user.fullName(), user.status(), user.userType(), user.agentId(), user.agentCode(),
                user.mustChangePassword(), user.temporaryPasswordExpiresAt(), user.permissions(),
                user.failedLoginAttempts(), user.lockedUntil(), user.passwordChangedAt(),
                user.passwordExpiresAt(), user.createdAt(), user.updatedAt(), user.lastLoginAt(), user.createdBy()
        );
    }

    public Optional<UserRecord> findByAgentId(UUID agentId) {
        return userRepository.findByAgentId(agentId);
    }

    @Override
    public boolean changePassword(UUID userId, String currentPassword, String newPassword) {
        UserRecord user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordHasher.matches(currentPassword, user.passwordHash())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }

        String hashedPassword = passwordHasher.hash(newPassword);
        userRepository.updatePassword(userId, hashedPassword, LocalDateTime.now());

        userRepository.clearTempPasswordFlags(userId);
        return true;
    }
}