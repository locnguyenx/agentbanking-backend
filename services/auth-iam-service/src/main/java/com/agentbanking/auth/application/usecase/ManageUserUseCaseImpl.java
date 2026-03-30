package com.agentbanking.auth.application.usecase;

import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.domain.model.UserStatus;
import com.agentbanking.auth.domain.port.in.ManageUserUseCase;
import com.agentbanking.auth.domain.port.out.PasswordHasher;
import com.agentbanking.auth.domain.port.out.UserRepository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Use case implementation for user management.
 * Uses PasswordHasher outbound port for password operations.
 */
public class ManageUserUseCaseImpl implements ManageUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public ManageUserUseCaseImpl(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
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
                hashedPassword,
                userRecord.fullName(),
                userRecord.status() != null ? userRecord.status() : UserStatus.ACTIVE,
                userRecord.permissions(),
                0, // failedLoginAttempts
                null, // lockedUntil
                LocalDateTime.now(), // passwordChangedAt
                LocalDateTime.now().plusDays(90), // passwordExpiresAt (90 days)
                LocalDateTime.now(), // createdAt
                LocalDateTime.now(), // updatedAt
                null, // lastLoginAt
                userRecord.createdBy(),
                null, // agentId - to be set later via onboarding/linking service
                null    // agentCode - to be set later via onboarding/linking service
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
                existing.passwordHash(), // Password should be updated via resetPassword
                userRecord.fullName(),
                userRecord.status() != null ? userRecord.status() : existing.status(),
                userRecord.permissions() != null ? userRecord.permissions() : existing.permissions(),
                existing.failedLoginAttempts(),
                existing.lockedUntil(),
                existing.passwordChangedAt(),
                existing.passwordExpiresAt(),
                existing.createdAt(),
                LocalDateTime.now(),
                existing.lastLoginAt(),
                existing.createdBy(),
                null, // agentId - preserve existing
                null    // agentCode - preserve existing
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
                user.passwordHash(),
                user.fullName(),
                UserStatus.LOCKED,
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
                user.passwordHash(),
                user.fullName(),
                UserStatus.ACTIVE,
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
                hashedNewPassword,
                user.fullName(),
                user.status(),
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
}