package com.agentbanking.auth.infrastructure.web.dto;

import com.agentbanking.auth.domain.model.UserStatus;
import com.agentbanking.auth.domain.model.UserType;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UserResponseDto(
    UUID userId,
    String username,
    String email,
    String fullName,
    UserStatus status,
    UserType userType,
    UUID agentId,
    Set<String> permissions,
    LocalDateTime createdAt,
    LocalDateTime lastLoginAt
) {}
