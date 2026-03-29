package com.agentbanking.auth.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain record for permissions in the Auth/IAM service
 */
public record PermissionRecord(
    UUID permissionId,
    String permissionKey,
    String description,
    String resource,
    String action,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String createdBy
) {}