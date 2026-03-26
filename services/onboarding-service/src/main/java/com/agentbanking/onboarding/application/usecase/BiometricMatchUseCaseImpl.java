package com.agentbanking.onboarding.application.usecase;

import com.agentbanking.onboarding.domain.model.AmlStatus;
import com.agentbanking.onboarding.domain.model.BiometricResult;
import com.agentbanking.onboarding.domain.model.KycStatus;
import com.agentbanking.onboarding.domain.port.in.BiometricMatchUseCase;
import com.agentbanking.onboarding.domain.service.KycDecisionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BiometricMatchUseCaseImpl implements BiometricMatchUseCase {

    private final KycDecisionService kycDecisionService;

    public BiometricMatchUseCaseImpl(KycDecisionService kycDecisionService) {
        this.kycDecisionService = kycDecisionService;
    }

    @Override
    public BiometricMatchResult matchBiometric(String verificationId, String biometricData) {
        if (verificationId == null) {
            throw new IllegalArgumentException("verificationId is required");
        }

        // Simulate biometric match (would call HSM/match-on-card in production)
        BiometricResult match = BiometricResult.MATCH;
        AmlStatus amlStatus = AmlStatus.CLEAN;
        int age = 35;

        // Apply decision matrix
        KycStatus decision = kycDecisionService.decide(match, amlStatus, age);

        return new BiometricMatchResult(
            verificationId,
            decision.name(),
            match.name(),
            LocalDateTime.now().toString()
        );
    }
}