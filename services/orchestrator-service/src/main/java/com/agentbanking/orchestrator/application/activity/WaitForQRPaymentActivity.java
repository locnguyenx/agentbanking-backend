package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.QRPaymentPort.QRPaymentStatus;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface WaitForQRPaymentActivity {
    QRPaymentStatus waitForPayment(String qrReference, int timeoutSeconds);
}
