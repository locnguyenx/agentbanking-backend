package com.agentbanking.auth.infrastructure.web.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record MyProfileResponse(
    String userId,
    String username,
    String email,
    String fullName,
    String userType,
    String status,
    String agentId,
    Boolean mustChangePassword,
    LocalDateTime temporaryPasswordExpiresAt,
    LocalDateTime createdAt,
    LocalDateTime lastLoginAt,
    Set<String> permissions
) {}
