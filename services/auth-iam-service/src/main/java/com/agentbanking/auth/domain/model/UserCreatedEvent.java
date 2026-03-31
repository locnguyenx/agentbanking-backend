package com.agentbanking.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public record UserCreatedEvent(
    UUID eventId,
    String eventType,
    Instant timestamp,
    UserCreatedData data
) {
    public record UserCreatedData(
        UUID userId,
        String username,
        String email,
        String phone,
        String fullName,
        String userType,
        UUID agentId,
        String notificationChannel,
        String temporaryPassword
    ) {}

    public static UserCreatedEvent create(UserCreatedData data) {
        return new UserCreatedEvent(UUID.randomUUID(), "USER_CREATED", Instant.now(), data);
    }
}
