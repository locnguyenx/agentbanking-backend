package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.PINInventoryPort.PINGenerationResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface GeneratePINActivity {
    PINGenerationResult generate(String provider, java.math.BigDecimal faceValue, String idempotencyKey);
}
