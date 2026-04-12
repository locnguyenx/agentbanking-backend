package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;

import com.agentbanking.orchestrator.domain.port.out.ESSPServicePort.ESSPPurchaseResult;




@ActivityInterface
public interface PurchaseESSPActivity {
    ESSPPurchaseResult purchase(java.math.BigDecimal amount, String customerMykad, String idempotencyKey);
}
