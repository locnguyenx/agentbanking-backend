package com.agentbanking.auth.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain record for roles in the Auth/IAM service
 */
public record RoleRecord(
    UUID roleId,
    String roleName,
    String description,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String createdBy
) {}