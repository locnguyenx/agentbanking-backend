package com.agentbanking.onboarding.domain.service;

import com.agentbanking.onboarding.domain.model.AgentOnboardingRecord;
import com.agentbanking.onboarding.domain.model.OnboardingDecision;
import com.agentbanking.onboarding.domain.model.AmlStatus;
import com.agentbanking.onboarding.domain.port.out.AgentOnboardingRepository;
import com.agentbanking.onboarding.domain.port.out.AgentRepository;
import com.agentbanking.onboarding.domain.port.out.AmlScreeningPort;
import com.agentbanking.onboarding.domain.port.out.GpfenceService;
import com.agentbanking.onboarding.domain.port.out.OcroService;
import com.agentbanking.onboarding.domain.port.out.SsmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentOnboardingServiceTest {

    @Mock
    private AgentOnboardingRepository onboardingRepository;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private OcroService ocrService;

    @Mock
    private SsmService ssmService;

    @Mock
    private AmlScreeningPort amlService;

    @Mock
    private GpfenceService gpfenceService;

    @InjectMocks
    private AgentOnboardingService agentOnboardingService;

    private String mykadNumber;
    private String extractedName;
    private String businessName;
    private String ownerName;
    private UUID onboardingId;

    @BeforeEach
    void setUp() {
        mykadNumber = "123456789012";
        extractedName = "John Doe";
        businessName = "John's Shop";
        ownerName = "John Doe";
        onboardingId = UUID.randomUUID();
    }

    @Test
    void shouldStartMicroAgentOnboardingSuccessfully_WhenAllChecksPass() {
        // Arrange
        when(ocrService.extractNameFromMyKad(mykadNumber)).thenReturn(extractedName);
        
        SsmService.SsmResult ssmResult = new SsmService.SsmResult(businessName, ownerName, true);
        when(ssmService.verifyBusiness(mykadNumber)).thenReturn(ssmResult);
        
        when(amlService.screen(mykadNumber, extractedName)).thenReturn(AmlStatus.CLEAN);
        // Note: gpsLowRisk is currently hardcoded to true in service implementation (GPS check not yet integrated)
        
        AgentOnboardingRecord record = new AgentOnboardingRecord(
            onboardingId, mykadNumber, extractedName, businessName, ownerName, "MICRO",
            true, true, true, true, true, LocalDateTime.now(), LocalDateTime.now()
        );
        when(onboardingRepository.save(any())).thenReturn(record);

        // Act
        AgentOnboardingRecord result = agentOnboardingService.startMicroAgentOnboarding(mykadNumber);

        // Assert
        assertEquals(onboardingId, result.onboardingId());
        assertEquals(mykadNumber, result.mykadNumber());
        assertEquals("MICRO", result.agentTier());
        assertTrue(result.ocrNameMatch());
        assertTrue(result.ssmActive());
        assertTrue(result.ssmOwnerMatch());
        assertTrue(result.amlClean());
        assertTrue(result.gpsLowRisk());
        
        verify(ocrService).extractNameFromMyKad(mykadNumber);
        verify(ssmService).verifyBusiness(mykadNumber);
        verify(amlService).screen(mykadNumber, extractedName);
        verify(onboardingRepository).save(any());
    }

    @Test
    void shouldEvaluateMicroAgentOnboardingAndAutoApprove_WhenAllChecksPass() {
        // Arrange
        AgentOnboardingRecord onboarding = new AgentOnboardingRecord(
            onboardingId, mykadNumber, extractedName, businessName, ownerName, "MICRO",
            true, true, true, true, true, LocalDateTime.now(), LocalDateTime.now()
        );
        when(onboardingRepository.findById(onboardingId)).thenReturn(Optional.of(onboarding));

        // Act
        OnboardingDecision decision = agentOnboardingService.evaluateMicroAgentOnboarding(onboardingId);

        // Assert
        assertNotNull(decision.decisionId());
        assertEquals(onboardingId, decision.onboardingId());
        assertEquals(com.agentbanking.onboarding.domain.model.OnboardingDecisionType.AUTO_APPROVED, decision.decisionType());
        assertEquals("All checks passed", decision.reason());
        assertEquals("SYSTEM", decision.reviewerId());
        assertNotNull(decision.decidedAt());
    }

    @Test
    void shouldEvaluateMicroAgentOnboardingAndRequireManualReview_WhenSomeChecksFail() {
        // Arrange
        AgentOnboardingRecord onboarding = new AgentOnboardingRecord(
            onboardingId, mykadNumber, extractedName, businessName, ownerName, "MICRO",
            false, true, true, false, true, LocalDateTime.now(), LocalDateTime.now() // OCR mismatch, AML not clean
        );
        when(onboardingRepository.findById(onboardingId)).thenReturn(Optional.of(onboarding));

        // Act
        OnboardingDecision decision = agentOnboardingService.evaluateMicroAgentOnboarding(onboardingId);

        // Assert
        assertNotNull(decision.decisionId());
        assertEquals(onboardingId, decision.onboardingId());
        assertEquals(com.agentbanking.onboarding.domain.model.OnboardingDecisionType.MANUAL_REVIEW, decision.decisionType());
        assertTrue(decision.reason().contains("OCR name mismatch"));
        assertTrue(decision.reason().contains("AML not clean"));
        assertNull(decision.reviewerId());
        assertNotNull(decision.decidedAt());
    }

    @Test
    void shouldStartStandardPremierOnboarding_WithoutAutomaticChecks() {
        // Arrange
        AgentOnboardingRecord record = new AgentOnboardingRecord(
            onboardingId, mykadNumber, "", "", "", "STANDARD",
            false, false, false, false, true, LocalDateTime.now(), LocalDateTime.now()
        );
        when(onboardingRepository.save(any())).thenReturn(record);

        // Act
        AgentOnboardingRecord result = agentOnboardingService.startStandardPremierOnboarding(mykadNumber, "STANDARD");

        // Assert
        assertEquals(onboardingId, result.onboardingId());
        assertEquals(mykadNumber, result.mykadNumber());
        assertEquals("STANDARD", result.agentTier());
        assertFalse(result.ocrNameMatch());
        assertFalse(result.ssmActive());
        assertFalse(result.ssmOwnerMatch());
        assertFalse(result.amlClean());
        assertTrue(result.gpsLowRisk());
        
        verify(onboardingRepository).save(any());
    }

    @Test
    void shouldThrowException_WhenOnboardingNotFound() {
        // Arrange
        when(onboardingRepository.findById(any())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            agentOnboardingService.evaluateMicroAgentOnboarding(onboardingId);
        });
    }
}