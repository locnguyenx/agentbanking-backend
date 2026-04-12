package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import com.agentbanking.orchestrator.domain.port.out.PINInventoryPort.PINGenerationResult;


@ActivityInterface
public interface GeneratePINActivity {
    @ActivityMethod(name = "generatePIN")
    PINGenerationResult generate(String provider, java.math.BigDecimal faceValue, String idempotencyKey);
}
