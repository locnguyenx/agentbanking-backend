package com.agentbanking.auth.infrastructure.persistence;

import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.domain.model.UserStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Mapper to convert between UserEntity and UserRecord
 */
@Component
public class UserMapper {

    public UserRecord toRecord(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UserRecord(
            entity.getUserId(),
            entity.getUsername(),
            entity.getEmail(),
            entity.getPasswordHash(),
            entity.getFullName(),
            UserStatus.valueOf(entity.getStatus().name()),
            Set.of(), // permissions will be loaded separately
            entity.getFailedLoginAttempts(),
            entity.getLockedUntil(),
            entity.getPasswordChangedAt(),
            entity.getPasswordExpiresAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getLastLoginAt(),
            entity.getCreatedBy()
        );
    }

    public UserEntity toEntity(UserRecord record) {
        if (record == null) {
            return null;
        }
        UserEntity entity = new UserEntity();
        entity.setUserId(record.userId());
        entity.setUsername(record.username());
        entity.setEmail(record.email());
        entity.setPasswordHash(record.passwordHash());
        entity.setFullName(record.fullName());
        entity.setStatus(record.status());
        entity.setFailedLoginAttempts(record.failedLoginAttempts());
        entity.setLockedUntil(record.lockedUntil());
        entity.setPasswordChangedAt(record.passwordChangedAt());
        entity.setPasswordExpiresAt(record.passwordExpiresAt());
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        entity.setLastLoginAt(record.lastLoginAt());
        entity.setCreatedBy(record.createdBy());
        return entity;
    }
}