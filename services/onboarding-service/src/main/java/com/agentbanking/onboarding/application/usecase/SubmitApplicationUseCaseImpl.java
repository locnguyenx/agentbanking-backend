package com.agentbanking.onboarding.application.usecase;

import com.agentbanking.onboarding.domain.model.AgentOnboardingRecord;
import com.agentbanking.onboarding.domain.model.AgentTier;
import com.agentbanking.onboarding.domain.port.in.SubmitApplicationUseCase;
import com.agentbanking.onboarding.domain.port.out.AgentOnboardingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SubmitApplicationUseCaseImpl implements SubmitApplicationUseCase {

    private final AgentOnboardingRepository onboardingRepository;

    public SubmitApplicationUseCaseImpl(AgentOnboardingRepository onboardingRepository) {
        this.onboardingRepository = onboardingRepository;
    }

    @Override
    @Transactional
    public SubmitApplicationResult submitApplication(SubmitApplicationCommand command) {
        AgentOnboardingRecord onboarding = new AgentOnboardingRecord(
            UUID.randomUUID(),
            command.mykadNumber(),
            command.extractedName(),
            command.ssmBusinessName(),
            command.ssmOwnerName(),
            command.agentTier().name(),
            false,
            false,
            false,
            false,
            true,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        AgentOnboardingRecord saved = onboardingRepository.save(onboarding);

        return new SubmitApplicationResult(
            saved.onboardingId(),
            "SUBMITTED",
            "Application submitted successfully. Pending review."
        );
    }
}
