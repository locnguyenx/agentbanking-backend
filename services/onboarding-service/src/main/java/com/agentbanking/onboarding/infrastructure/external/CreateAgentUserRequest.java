package com.agentbanking.onboarding.infrastructure.external;

import java.util.UUID;

public record CreateAgentUserRequest(
    UUID agentId,
    String agentCode,
    String phone,
    String email,
    String businessName
) {}
