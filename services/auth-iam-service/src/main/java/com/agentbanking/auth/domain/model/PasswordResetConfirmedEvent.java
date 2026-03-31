package com.agentbanking.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public record PasswordResetConfirmedEvent(
    UUID eventId,
    String eventType,
    Instant timestamp,
    PasswordResetConfirmedData data
) {
    public record PasswordResetConfirmedData(UUID userId, String username, String email, String phone) {}

    public static PasswordResetConfirmedEvent create(UUID userId, String username, String email, String phone) {
        return new PasswordResetConfirmedEvent(UUID.randomUUID(), "PASSWORD_RESET_CONFIRMED", Instant.now(),
            new PasswordResetConfirmedData(userId, username, email, phone));
    }
}
