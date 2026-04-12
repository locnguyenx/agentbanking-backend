package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;

import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillPaymentInput;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillPaymentResult;




@ActivityInterface
public interface PayBillerActivity {
    BillPaymentResult payBill(BillPaymentInput input);
}
