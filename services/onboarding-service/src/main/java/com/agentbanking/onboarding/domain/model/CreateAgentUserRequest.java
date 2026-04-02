package com.agentbanking.onboarding.domain.model;

import java.util.UUID;

public record CreateAgentUserRequest(
    UUID agentId,
    String agentCode,
    String phone,
    String email,
    String businessName
) {}
