package com.agentbanking.auth.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain record for user sessions in the Auth/IAM service
 */
public record SessionRecord(
    UUID sessionId,
    UUID userId,
    String refreshTokenHash,
    String userAgent,
    String ipAddress,
    LocalDateTime createdAt,
    LocalDateTime expiresAt,
    LocalDateTime lastAccessedAt,
    LocalDateTime revokedAt,
    Boolean isActive
) {}