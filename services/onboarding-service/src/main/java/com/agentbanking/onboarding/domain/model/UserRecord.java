package com.agentbanking.onboarding.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain record for backoffice users
 * Note: Authentication and IAM logic (passwordHash, role, permissions) has been moved to auth-iam-service
 */
public record UserRecord(
    UUID userId,
    String username,
    String email,
    String fullName,
    UserStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String createdBy
) {}
