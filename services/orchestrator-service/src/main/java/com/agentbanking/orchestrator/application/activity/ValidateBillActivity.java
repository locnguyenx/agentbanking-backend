package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;

import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillValidationInput;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillValidationResult;




@ActivityInterface
public interface ValidateBillActivity {
    BillValidationResult validateBill(BillValidationInput input);
}
