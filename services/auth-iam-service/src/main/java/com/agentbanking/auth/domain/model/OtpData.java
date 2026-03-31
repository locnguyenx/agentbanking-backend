package com.agentbanking.auth.domain.model;

import java.time.LocalDateTime;

public record OtpData(
    String hashedOtp,
    int attempts,
    LocalDateTime createdAt
) {}
