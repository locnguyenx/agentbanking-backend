package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import com.agentbanking.orchestrator.domain.port.out.QRPaymentPort.QRGenerationResult;


@ActivityInterface
public interface GenerateDynamicQRActivity {
    @ActivityMethod(name = "generateDynamicQR")
    QRGenerationResult generate(java.math.BigDecimal amount, java.util.UUID agentId, String idempotencyKey);
}
