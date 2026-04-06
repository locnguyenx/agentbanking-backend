package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.QRPaymentPort.QRGenerationResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface GenerateDynamicQRActivity {
    QRGenerationResult generate(java.math.BigDecimal amount, java.util.UUID agentId, String idempotencyKey);
}
