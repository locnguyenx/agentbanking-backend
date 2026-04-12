package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;

import io.temporal.activity.ActivityInterface;


@ActivityInterface
public interface ValidateFloatCapacityActivity {
    FloatCapacityResult validate(java.util.UUID agentId, java.math.BigDecimal requiredAmount);
    
    record FloatCapacityResult(boolean sufficient, java.math.BigDecimal availableFloat, String errorCode) {}
}
