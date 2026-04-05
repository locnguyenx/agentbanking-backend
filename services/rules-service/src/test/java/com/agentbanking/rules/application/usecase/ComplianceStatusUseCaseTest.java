package com.agentbanking.rules.application.usecase;

import com.agentbanking.rules.domain.port.in.ComplianceStatusUseCase.ComplianceStatusResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComplianceStatusUseCaseTest {

    private ComplianceStatusUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ComplianceStatusUseCaseImpl();
    }

    @Test
    void checkCompliance_shouldReturnUnlockedForCleanAgent() {
        // Given
        String agentId = "agent-001";

        // When
        ComplianceStatusResult result = useCase.checkCompliance(agentId);

        // Then
        assertEquals("UNLOCKED", result.status());
        assertNull(result.reason());
        assertNotNull(result.checkedAt());
    }

    @Test
    void checkCompliance_shouldReturnLockedForAgentWithHold() {
        // Given — agent-aml-flagged is in the locked agents set
        String agentId = "agent-aml-flagged";

        // When
        ComplianceStatusResult result = useCase.checkCompliance(agentId);

        // Then
        assertEquals("LOCKED", result.status());
        assertNotNull(result.reason());
        assertFalse(result.reason().isEmpty());
    }
}
