package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.PINInventoryPort.PINInventoryResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface ValidatePINInventoryActivity {
    PINInventoryResult validate(String provider, java.math.BigDecimal faceValue);
}
