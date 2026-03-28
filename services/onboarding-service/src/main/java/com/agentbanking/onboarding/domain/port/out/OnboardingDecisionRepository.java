package com.agentbanking.onboarding.domain.port.out;

import com.agentbanking.onboarding.domain.model.OnboardingDecision;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository port for OnboardingDecision aggregate
 */
public interface OnboardingDecisionRepository {
    OnboardingDecision save(OnboardingDecision decision);
    Optional<OnboardingDecision> findById(UUID decisionId);
}
