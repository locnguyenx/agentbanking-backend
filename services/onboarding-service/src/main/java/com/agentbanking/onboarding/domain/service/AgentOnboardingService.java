package com.agentbanking.onboarding.domain.service;

import com.agentbanking.onboarding.domain.model.AgentOnboardingRecord;
import com.agentbanking.onboarding.domain.model.OnboardingDecision;
import com.agentbanking.onboarding.domain.model.OnboardingDecisionType;
import com.agentbanking.onboarding.domain.model.AmlStatus;
import com.agentbanking.onboarding.domain.port.out.AgentOnboardingRepository;
import com.agentbanking.onboarding.domain.port.out.AgentRepository;
import com.agentbanking.onboarding.domain.port.out.AmlScreeningPort;
import com.agentbanking.onboarding.domain.port.out.GpfenceService;
import com.agentbanking.onboarding.domain.port.out.OcroService;
import com.agentbanking.onboarding.domain.port.out.SsmService;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for processing agent onboarding applications (US-A01, US-A02)
 */
public class AgentOnboardingService {

    private final AgentOnboardingRepository onboardingRepository;
    private final AgentRepository agentRepository;
    private final OcroService ocrService;
    private final SsmService ssmService;
    private final AmlScreeningPort amlService;
    private final GpfenceService gpfenceService;

    public AgentOnboardingService(AgentOnboardingRepository onboardingRepository,
                                   AgentRepository agentRepository,
                                   OcroService ocrService,
                                   SsmService ssmService,
                                   AmlScreeningPort amlService,
                                   GpfenceService gpfenceService) {
        this.onboardingRepository = onboardingRepository;
        this.agentRepository = agentRepository;
        this.ocrService = ocrService;
        this.ssmService = ssmService;
        this.amlService = amlService;
        this.gpfenceService = gpfenceService;
    }

    /**
     * Starts the onboarding process for a micro-agent (Conditional STP)
     * Performs OCR, SSM, AML, and GPS checks automatically
     *
     * @param mykadNumber The MyKad number of the applicant
     * @return The created onboarding record
     */
    public AgentOnboardingRecord startMicroAgentOnboarding(String mykadNumber) {
        // OCR extraction
        String extractedName = ocrService.extractNameFromMyKad(mykadNumber);
        
        // SSM verification
        SsmService.SsmResult ssmResult = ssmService.verifyBusiness(mykadNumber);
        
        // AML screening
        AmlStatus amlStatus = amlService.screen(mykadNumber, extractedName);
        
        // GPS check (if available) - in a real scenario, we would get the GPS from the request context
        // For now, we assume low risk if not provided (or we could get from agent's registered GPS)
        boolean gpsLowRisk = true; // This should ideally come from the agent's registered location vs. applicant's GPS
        
        AgentOnboardingRecord onboarding = new AgentOnboardingRecord(
            UUID.randomUUID(),
            mykadNumber,
            extractedName,
            ssmResult.businessName(),
            ssmResult.ownerName(),
            "MICRO", // Micro-agent tier
            extractedName.equalsIgnoreCase(ssmResult.ownerName()),
            ssmResult.isActive(),
            extractedName.equalsIgnoreCase(ssmResult.ownerName()),
            amlStatus == AmlStatus.CLEAN,
            gpsLowRisk,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        
        return onboardingRepository.save(onboarding);
    }

    /**
     * Evaluates a micro-agent onboarding application and makes an automatic decision
     * (Conditional STP: auto-approve if all checks pass, otherwise manual review)
     *
     * @param onboardingId The ID of the onboarding record to evaluate
     * @return The onboarding decision
     */
    public OnboardingDecision evaluateMicroAgentOnboarding(UUID onboardingId) {
        AgentOnboardingRecord onboarding = onboardingRepository.findById(onboardingId)
            .orElseThrow(() -> new IllegalArgumentException("Onboarding not found"));
        
        // Decision logic for micro-agent (Conditional STP)
        boolean allChecksPass = onboarding.ocrNameMatch() &&
                               onboarding.ssmActive() &&
                               onboarding.ssmOwnerMatch() &&
                               onboarding.amlClean() &&
                               onboarding.gpsLowRisk();
        
        if (allChecksPass) {
            return new OnboardingDecision(
                UUID.randomUUID(),
                onboardingId,
                OnboardingDecisionType.AUTO_APPROVED,
                "All checks passed",
                "SYSTEM",
                LocalDateTime.now()
            );
        } else {
            StringBuilder reason = new StringBuilder("Failed checks: ");
            if (!onboarding.ocrNameMatch()) reason.append("OCR name mismatch, ");
            if (!onboarding.ssmActive()) reason.append("SSM inactive, ");
            if (!onboarding.ssmOwnerMatch()) reason.append("SSM owner mismatch, ");
            if (!onboarding.amlClean()) reason.append("AML not clean, ");
            if (!onboarding.gpsLowRisk()) reason.append("High-risk GPS zone, ");
            
            return new OnboardingDecision(
                UUID.randomUUID(),
                onboardingId,
                OnboardingDecisionType.MANUAL_REVIEW,
                reason.toString(),
                null,
                LocalDateTime.now()
            );
        }
    }
    
    /**
     * Starts the onboarding process for a standard or premier agent (Non-STP)
     * This does not make an automatic decision; it creates a record for human review (Maker-Checker)
     *
     * @param mykadNumber The MyKad number of the applicant
     * @param agentTier The tier of agent being applied for (STANDARD or PREMIER)
     * @return The created onboarding record
     */
    public AgentOnboardingRecord startStandardPremierOnboarding(String mykadNumber, String agentTier) {
        // For standard/premier agents, we don't perform automatic checks; we create a record for human review
        // In a real system, we might still do some basic checks (like MyKad format) but the decision is human
        
        AgentOnboardingRecord onboarding = new AgentOnboardingRecord(
            UUID.randomUUID(),
            mykadNumber,
            "", // OCR not required for Standard/Premier initially (done by human)
            "", // SSM not checked initially (done by human)
            "", // SSM owner not checked initially (done by human)
            agentTier,
            false, // OCR name match not applicable initially
            false, // SSM active not checked initially
            false, // SSM owner match not applicable
            false, // AML clean not checked initially
            true,  // GPS risk not checked initially (assumed low until human verification)
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        
        return onboardingRepository.save(onboarding);
    }
}