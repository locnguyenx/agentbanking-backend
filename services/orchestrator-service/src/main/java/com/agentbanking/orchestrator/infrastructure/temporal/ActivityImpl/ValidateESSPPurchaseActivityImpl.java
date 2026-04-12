package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.ValidateESSPPurchaseActivity;
import com.agentbanking.orchestrator.domain.port.out.ESSPServicePort;


import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ValidateESSPPurchaseActivityImpl implements ValidateESSPPurchaseActivity {

    private final ESSPServicePort port;

    public ValidateESSPPurchaseActivityImpl(ESSPServicePort port) {
        this.port = port;
    }

    @Override
    public ESSPServicePort.ESSPValidationResult validate(BigDecimal amount) {
        return port.validatePurchase(amount);
    }
}
