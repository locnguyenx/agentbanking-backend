package com.agentbanking.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public record OtpRequestedEvent(
    UUID eventId,
    String eventType,
    Instant timestamp,
    OtpRequestedData data
) {
    public record OtpRequestedData(
        UUID userId,
        String username,
        String email,
        String phone,
        String otp,
        String channel
    ) {}
}
