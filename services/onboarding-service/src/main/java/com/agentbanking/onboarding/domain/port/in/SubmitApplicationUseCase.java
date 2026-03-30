package com.agentbanking.onboarding.domain.port.in;

import com.agentbanking.onboarding.domain.model.AgentOnboardingRecord;
import com.agentbanking.onboarding.domain.model.AgentTier;

import java.math.BigDecimal;
import java.util.UUID;

public interface SubmitApplicationUseCase {

    SubmitApplicationResult submitApplication(SubmitApplicationCommand command);

    record SubmitApplicationCommand(
        String mykadNumber,
        String extractedName,
        String ssmBusinessName,
        String ssmOwnerName,
        AgentTier agentTier,
        BigDecimal merchantGpsLat,
        BigDecimal merchantGpsLng,
        String phoneNumber
    ) {}

    record SubmitApplicationResult(
        UUID applicationId,
        String status,
        String message
    ) {}
}
