package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.ESSPServicePort.ESSPValidationResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface ValidateESSPPurchaseActivity {
    ESSPValidationResult validate(java.math.BigDecimal amount);
}
