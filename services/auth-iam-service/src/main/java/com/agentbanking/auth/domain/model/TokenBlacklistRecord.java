package com.agentbanking.auth.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain record for token blacklist entries in the Auth/IAM service
 */
public record TokenBlacklistRecord(
    UUID blacklistId,
    String tokenJti,
    UUID userId,
    String clientId,
    LocalDateTime revokedAt,
    LocalDateTime expiresAt,
    String revokedBy,
    String reason
) {}