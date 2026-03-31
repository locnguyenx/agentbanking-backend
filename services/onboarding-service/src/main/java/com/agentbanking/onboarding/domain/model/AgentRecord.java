package com.agentbanking.onboarding.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AgentRecord(
    UUID agentId,
    String agentCode,
    String businessName,
    AgentTier tier,
    AgentStatus status,
    BigDecimal merchantGpsLat,
    BigDecimal merchantGpsLng,
    String mykadNumber,
    String phoneNumber,
    UserCreationStatus userCreationStatus,
    String userCreationError,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
