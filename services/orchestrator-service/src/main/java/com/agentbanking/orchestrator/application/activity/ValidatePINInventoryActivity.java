package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import com.agentbanking.orchestrator.domain.port.out.PINInventoryPort.PINInventoryResult;



@ActivityInterface
public interface ValidatePINInventoryActivity {
    @ActivityMethod(name = "ValidatePINInventory")
    PINInventoryResult validate(String provider, java.math.BigDecimal faceValue);
}
