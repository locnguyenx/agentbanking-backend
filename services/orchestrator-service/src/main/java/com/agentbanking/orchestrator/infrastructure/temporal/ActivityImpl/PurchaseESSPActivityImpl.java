package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.PurchaseESSPActivity;
import com.agentbanking.orchestrator.domain.port.out.ESSPServicePort;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ActivityImpl(workers = "agent-banking-tasks")
public class PurchaseESSPActivityImpl implements PurchaseESSPActivity {

    private final ESSPServicePort port;

    public PurchaseESSPActivityImpl(ESSPServicePort port) {
        this.port = port;
    }

    @Override
    public ESSPServicePort.ESSPPurchaseResult purchase(BigDecimal amount, String customerMykad, String idempotencyKey) {
        return port.purchase(amount, customerMykad, idempotencyKey);
    }
}
