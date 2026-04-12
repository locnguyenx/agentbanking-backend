package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.ValidateFloatCapacityActivity;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort;


import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@ActivityImpl(workers = "agent-banking-tasks")
public class ValidateFloatCapacityActivityImpl implements ValidateFloatCapacityActivity {

    private final LedgerServicePort port;

    public ValidateFloatCapacityActivityImpl(LedgerServicePort port) {
        this.port = port;
    }

    @Override
    public FloatCapacityResult validate(UUID agentId, BigDecimal requiredAmount) {
        // Simplified - in production would query actual float balance
        BigDecimal availableFloat = new BigDecimal("10000.00");
        boolean sufficient = availableFloat.compareTo(requiredAmount) >= 0;
        return new FloatCapacityResult(sufficient, availableFloat, null);
    }
}
