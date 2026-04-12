package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.PayBillerActivity;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillPaymentInput;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillPaymentResult;


import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(workers = "agent-banking-tasks")
public class PayBillerActivityImpl implements PayBillerActivity {

    private final BillerServicePort billerServicePort;

    public PayBillerActivityImpl(BillerServicePort billerServicePort) {
        this.billerServicePort = billerServicePort;
    }

    @Override
    public BillPaymentResult payBill(BillPaymentInput input) {
        return billerServicePort.payBill(input);
    }
}
