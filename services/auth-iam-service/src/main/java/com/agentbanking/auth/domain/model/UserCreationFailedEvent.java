package com.agentbanking.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public record UserCreationFailedEvent(
    UUID eventId,
    String eventType,
    Instant timestamp,
    UserCreationFailedData data
) {
    public record UserCreationFailedData(UUID agentId, String agentCode, String error) {}

    public static UserCreationFailedEvent create(UUID agentId, String agentCode, String error) {
        return new UserCreationFailedEvent(UUID.randomUUID(), "USER_CREATION_FAILED", Instant.now(),
            new UserCreationFailedData(agentId, agentCode, error));
    }
}
