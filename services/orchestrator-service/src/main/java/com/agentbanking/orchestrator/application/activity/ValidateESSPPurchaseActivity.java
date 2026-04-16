package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import com.agentbanking.orchestrator.domain.port.out.ESSPServicePort.ESSPValidationResult;



@ActivityInterface
public interface ValidateESSPPurchaseActivity {
    @ActivityMethod(name = "ValidateESSPPurchase")
    ESSPValidationResult validate(java.math.BigDecimal amount);
}
