package com.agentbanking.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public record AgentCreatedEvent(
    UUID eventId,
    String eventType,
    Instant timestamp,
    AgentCreatedData data
) {
    public record AgentCreatedData(
        UUID agentId,
        String agentCode,
        String phoneNumber,
        String email,
        String businessName
    ) {}
}
