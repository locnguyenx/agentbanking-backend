package com.agentbanking.onboarding.application.usecase;

import com.agentbanking.onboarding.domain.model.AgentOnboardingRecord;
import com.agentbanking.onboarding.domain.port.in.StartStandardPremierOnboardingUseCase;
import com.agentbanking.onboarding.domain.service.AgentOnboardingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartStandardPremierOnboardingUseCaseImplTest {

    @Mock
    private AgentOnboardingService agentOnboardingService;

    @InjectMocks
    private StartStandardPremierOnboardingUseCaseImpl startStandardPremierOnboardingUseCase;

    private String mykadNumber;
    private String agentTier;
    private AgentOnboardingRecord onboardingRecord;

    @BeforeEach
    void setUp() {
        mykadNumber = "123456789012";
        agentTier = "PREMIER";
        UUID onboardingId = UUID.randomUUID();
        onboardingRecord = new AgentOnboardingRecord(
            onboardingId, mykadNumber, "", "", "", agentTier,
            false, false, false, false, true, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    void shouldStartStandardPremierOnboardingSuccessfully() {
        // Arrange
        when(agentOnboardingService.startStandardPremierOnboarding(mykadNumber, agentTier))
            .thenReturn(onboardingRecord);

        // Act
        AgentOnboardingRecord result = startStandardPremierOnboardingUseCase.start(mykadNumber, agentTier);

        // Assert
        assertNotNull(result);
        assertEquals(mykadNumber, result.mykadNumber());
        assertEquals(agentTier, result.agentTier());
        assertEquals("PREMIER", result.agentTier());
        verify(agentOnboardingService).startStandardPremierOnboarding(mykadNumber, agentTier);
    }

    @Test
    void shouldThrowException_WhenOnboardingFails() {
        // Arrange
        when(agentOnboardingService.startStandardPremierOnboarding(anyString(), anyString()))
            .thenThrow(new IllegalArgumentException("Invalid agent tier"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            startStandardPremierOnboardingUseCase.start(mykadNumber, agentTier);
        });
    }
}