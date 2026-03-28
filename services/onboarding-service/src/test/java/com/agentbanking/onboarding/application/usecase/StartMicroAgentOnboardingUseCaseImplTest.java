package com.agentbanking.onboarding.application.usecase;

import com.agentbanking.onboarding.domain.model.AgentOnboardingRecord;
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
class StartMicroAgentOnboardingUseCaseImplTest {

    @Mock
    private AgentOnboardingService agentOnboardingService;

    @InjectMocks
    private StartMicroAgentOnboardingUseCaseImpl startMicroAgentOnboardingUseCase;

    private String mykadNumber;
    private AgentOnboardingRecord onboardingRecord;

    @BeforeEach
    void setUp() {
        mykadNumber = "123456789012";
        UUID onboardingId = UUID.randomUUID();
        onboardingRecord = new AgentOnboardingRecord(
            onboardingId, mykadNumber, "John Doe", "John's Shop", "John Doe", "MICRO",
            true, true, true, true, true, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    void shouldStartMicroAgentOnboardingSuccessfully() {
        // Arrange
        when(agentOnboardingService.startMicroAgentOnboarding(mykadNumber)).thenReturn(onboardingRecord);

        // Act
        AgentOnboardingRecord result = startMicroAgentOnboardingUseCase.start(mykadNumber);

        // Assert
        assertNotNull(result);
        assertEquals(mykadNumber, result.mykadNumber());
        assertEquals("MICRO", result.agentTier());
        verify(agentOnboardingService).startMicroAgentOnboarding(mykadNumber);
    }

    @Test
    void shouldThrowException_WhenOnboardingFails() {
        // Arrange
        when(agentOnboardingService.startMicroAgentOnboarding(anyString()))
            .thenThrow(new IllegalArgumentException("OCR extraction failed"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            startMicroAgentOnboardingUseCase.start(mykadNumber);
        });
    }
}