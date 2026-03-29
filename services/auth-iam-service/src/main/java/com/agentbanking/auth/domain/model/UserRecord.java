package com.agentbanking.auth.domain.model;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Domain record for users in the Auth/IAM service
 */
public record UserRecord(
    UUID userId,
    String username,
    String email,
    String passwordHash,
    String fullName,
    UserStatus status,
    Set<String> permissions,
    Integer failedLoginAttempts,
    LocalDateTime lockedUntil,
    LocalDateTime passwordChangedAt,
    LocalDateTime passwordExpiresAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime lastLoginAt,
    String createdBy
) {}