package com.agentbanking.auth.infrastructure.web.dto;

import java.util.UUID;

public record AgentUserStatusResponse(
    UUID agentId,
    String status,
    UUID userId,
    String error
) {}
