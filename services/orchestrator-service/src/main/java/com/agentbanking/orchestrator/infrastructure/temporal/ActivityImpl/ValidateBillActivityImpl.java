package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.ValidateBillActivity;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillValidationInput;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillValidationResult;
import org.springframework.stereotype.Component;

@Component
public class ValidateBillActivityImpl implements ValidateBillActivity {

    private final BillerServicePort billerServicePort;

    public ValidateBillActivityImpl(BillerServicePort billerServicePort) {
        this.billerServicePort = billerServicePort;
    }

    @Override
    public BillValidationResult validateBill(BillValidationInput input) {
        return billerServicePort.validateBill(input);
    }
}
