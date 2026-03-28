package com.agentbanking.onboarding.application.usecase;

import com.agentbanking.onboarding.domain.model.AgentOnboardingRecord;
import com.agentbanking.onboarding.domain.model.OnboardingDecision;
import com.agentbanking.onboarding.domain.port.in.EvaluateMicroAgentOnboardingUseCase;
import com.agentbanking.onboarding.domain.service.AgentOnboardingService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluateMicroAgentOnboardingUseCaseImplTest {

    @Mock
    private AgentOnboardingService agentOnboardingService;

    @InjectMocks
    private EvaluateMicroAgentOnboardingUseCaseImpl evaluateMicroAgentOnboardingUseCase;

    private UUID onboardingId;
    private AgentOnboardingRecord onboardingRecord;

    @BeforeEach
    void setUp() {
        onboardingId = UUID.randomUUID();
        onboardingRecord = new AgentOnboardingRecord(
            onboardingId, "123456789012", "John Doe", "John's Shop", "John Doe", "MICRO",
            true, true, true, true, true, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    void shouldEvaluateMicroAgentOnboardingSuccessfully() {
        // Arrange
        OnboardingDecision decision = new OnboardingDecision(
            UUID.randomUUID(), onboardingId, com.agentbanking.onboarding.domain.model.OnboardingDecisionType.AUTO_APPROVED,
            "All checks passed", "SYSTEM", LocalDateTime.now()
        );
        when(agentOnboardingService.evaluateMicroAgentOnboarding(onboardingId)).thenReturn(decision);

        // Act
        OnboardingDecision result = evaluateMicroAgentOnboardingUseCase.evaluate(onboardingId);

        // Assert
        assertNotNull(result);
        assertEquals(com.agentbanking.onboarding.domain.model.OnboardingDecisionType.AUTO_APPROVED, result.decisionType());
        assertEquals("All checks passed", result.reason());
        verify(agentOnboardingService).evaluateMicroAgentOnboarding(onboardingId);
    }

    @Test
    void shouldThrowException_WhenOnboardingNotFound() {
        // Arrange
        when(agentOnboardingService.evaluateMicroAgentOnboarding(any()))
            .thenThrow(new IllegalArgumentException("Onboarding not found"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            evaluateMicroAgentOnboardingUseCase.evaluate(onboardingId);
        });
    }
}