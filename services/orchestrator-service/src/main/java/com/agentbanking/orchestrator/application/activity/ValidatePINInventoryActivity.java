package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;

import com.agentbanking.orchestrator.domain.port.out.PINInventoryPort.PINInventoryResult;




@ActivityInterface
public interface ValidatePINInventoryActivity {
    PINInventoryResult validate(String provider, java.math.BigDecimal faceValue);
}
