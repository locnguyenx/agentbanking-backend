package com.agentbanking.auth.domain.model;

import java.time.Instant;

/**
 * Domain model for authentication result containing tokens and metadata.
 */
public record AuthenticationResult(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
    public Instant getExpiresAt() {
        return Instant.now().plusSeconds(expiresIn);
    }
}