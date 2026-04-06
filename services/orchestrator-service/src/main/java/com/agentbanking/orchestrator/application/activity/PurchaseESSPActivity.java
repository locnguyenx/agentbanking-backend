package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.ESSPServicePort.ESSPPurchaseResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface PurchaseESSPActivity {
    ESSPPurchaseResult purchase(java.math.BigDecimal amount, String customerMykad, String idempotencyKey);
}
