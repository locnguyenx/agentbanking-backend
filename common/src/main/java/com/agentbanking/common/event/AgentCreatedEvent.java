package com.agentbanking.common.event;

import java.time.Instant;
import java.util.UUID;

public record AgentCreatedEvent(
    UUID eventId,
    String eventType,
    Instant timestamp,
    AgentCreatedData data
) {
    public static final String EVENT_TYPE = "AGENT_CREATED";

    public record AgentCreatedData(
        UUID agentId,
        String agentCode,
        String phoneNumber,
        String email,
        String businessName,
        String tier,
        Double merchantGpsLat,
        Double merchantGpsLng
    ) {}

    public static AgentCreatedEvent create(UUID agentId, String agentCode, String phoneNumber,
            String email, String businessName, String tier, Double merchantGpsLat, Double merchantGpsLng) {
        return new AgentCreatedEvent(
            UUID.randomUUID(),
            EVENT_TYPE,
            Instant.now(),
            new AgentCreatedData(agentId, agentCode, phoneNumber, email, businessName, tier, merchantGpsLat, merchantGpsLng)
        );
    }
}