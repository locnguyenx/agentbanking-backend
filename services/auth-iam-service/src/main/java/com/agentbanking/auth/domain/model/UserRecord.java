package com.agentbanking.auth.domain.model;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UserRecord(
    UUID userId,
    String username,
    String email,
    String phone,
    String passwordHash,
    String fullName,
    UserStatus status,
    UserType userType,
    UUID agentId,
    String agentCode,
    Boolean mustChangePassword,
    LocalDateTime temporaryPasswordExpiresAt,
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