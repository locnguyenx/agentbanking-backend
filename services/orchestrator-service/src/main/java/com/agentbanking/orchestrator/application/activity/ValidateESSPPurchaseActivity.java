package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;

import com.agentbanking.orchestrator.domain.port.out.ESSPServicePort.ESSPValidationResult;




@ActivityInterface
public interface ValidateESSPPurchaseActivity {
    ESSPValidationResult validate(java.math.BigDecimal amount);
}
