package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillValidationInput;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillValidationResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface ValidateBillActivity {
    BillValidationResult validateBill(BillValidationInput input);
}
