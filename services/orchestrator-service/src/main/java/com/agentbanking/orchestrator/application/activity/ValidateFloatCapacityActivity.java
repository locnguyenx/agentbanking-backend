package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ValidateFloatCapacityActivity {
    @ActivityMethod(name = "ValidateFloatCapacity")
    FloatCapacityResult validate(java.util.UUID agentId, java.math.BigDecimal requiredAmount);
    
    record FloatCapacityResult(boolean sufficient, java.math.BigDecimal availableFloat, String errorCode) {}
}
