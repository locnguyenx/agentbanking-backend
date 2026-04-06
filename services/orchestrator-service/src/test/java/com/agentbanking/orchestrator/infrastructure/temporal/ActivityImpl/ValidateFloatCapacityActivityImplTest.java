package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.ValidateFloatCapacityActivity;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ValidateFloatCapacityActivityImplTest {

    @Test
    void shouldReturnSufficientFloatWhenAmountIsBelowLimit() {
        var mockPort = mock(LedgerServicePort.class);
        var activity = new ValidateFloatCapacityActivityImpl(mockPort);
        
        var result = activity.validate(UUID.randomUUID(), new BigDecimal("5000.00"));
        
        assertTrue(result.sufficient());
        assertNull(result.errorCode());
    }

    @Test
    void shouldReturnInsufficientFloatWhenAmountExceedsLimit() {
        var mockPort = mock(LedgerServicePort.class);
        var activity = new ValidateFloatCapacityActivityImpl(mockPort);
        
        var result = activity.validate(UUID.randomUUID(), new BigDecimal("15000.00"));
        
        assertFalse(result.sufficient());
    }

    @Test
    void shouldReturnSufficientFloatWhenAmountEqualsLimit() {
        var mockPort = mock(LedgerServicePort.class);
        var activity = new ValidateFloatCapacityActivityImpl(mockPort);
        
        var result = activity.validate(UUID.randomUUID(), new BigDecimal("10000.00"));
        
        assertTrue(result.sufficient());
    }
}
