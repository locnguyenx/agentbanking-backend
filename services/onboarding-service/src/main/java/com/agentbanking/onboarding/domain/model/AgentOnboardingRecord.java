package com.agentbanking.onboarding.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Record representing an agent onboarding application
 */
public record AgentOnboardingRecord(
    UUID onboardingId,
    String mykadNumber,
    String extractedName,
    String ssmBusinessName,
    String ssmOwnerName,
    String agentTier,
    boolean ocrNameMatch,
    boolean ssmActive,
    boolean ssmOwnerMatch,
    boolean amlClean,
    boolean gpsLowRisk,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}