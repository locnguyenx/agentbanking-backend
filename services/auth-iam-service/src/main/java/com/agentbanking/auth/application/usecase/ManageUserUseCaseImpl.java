package com.agentbanking.auth.application.usecase;

import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.domain.model.UserStatus;
import com.agentbanking.auth.domain.model.UserType;
import com.agentbanking.auth.domain.port.in.ManageUserUseCase;
import com.agentbanking.auth.domain.port.out.PasswordHasher;
import com.agentbanking.auth.domain.port.out.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
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
        
        // 21 fields mapping
        UserRecord newUser = new UserRecord(
                UUID.randomUUID(),   // 0: userId
                userRecord.username(), // 1: username
                userRecord.email(),    // 2: email
                userRecord.phone(),    // 3: phone
                hashedPassword,        // 4: passwordHash
                userRecord.fullName(), // 5: fullName
                userRecord.status() != null ? userRecord.status() : UserStatus.ACTIVE, // 6: status
                userRecord.userType() != null ? userRecord.userType() : UserType.INTERNAL, // 7: userType
                userRecord.agentId(),  // 8: agentId
                userRecord.agentCode(), // 9: agentCode
                userRecord.mustChangePassword() != null ? userRecord.mustChangePassword() : false, // 10: mustChangePassword
                userRecord.temporaryPasswordExpiresAt(), // 11: temporaryPasswordExpiresAt
                userRecord.permissions(), // 12: permissions
                0, // 13: failedLoginAttempts
                null, // 14: lockedUntil
                LocalDateTime.now(), // 15: passwordChangedAt
                LocalDateTime.now().plusDays(90), // 16: passwordExpiresAt
                LocalDateTime.now(), // 17: createdAt
                LocalDateTime.now(), // 18: updatedAt
                null, // 19: lastLoginAt
                userRecord.createdBy() // 20: createdBy
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
                existing.passwordHash(), // Password should be updated via resetPassword
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
    public boolean changePassword(UUID userId, String currentPassword, String newPassword) {
        UserRecord user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        // Verify current password
        if (!passwordHasher.matches(currentPassword, user.passwordHash())) {
            return false;
        }

        // Hash the new password
        String hashedNewPassword = passwordHasher.hash(newPassword);
        
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
                false, // User changed their own password, no longer need to change
                null, // Clear temporary password flag
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
}