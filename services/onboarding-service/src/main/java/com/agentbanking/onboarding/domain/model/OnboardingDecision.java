package com.agentbanking.onboarding.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Record representing an onboarding decision
 */
public record OnboardingDecision(
    UUID decisionId,
    UUID onboardingId,
    OnboardingDecisionType decisionType,
    String reason,
    String reviewerId,
    LocalDateTime decidedAt
) {}