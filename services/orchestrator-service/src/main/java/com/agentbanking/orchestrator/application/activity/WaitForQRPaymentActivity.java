package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;

import com.agentbanking.orchestrator.domain.port.out.QRPaymentPort.QRPaymentStatus;




@ActivityInterface
public interface WaitForQRPaymentActivity {
    QRPaymentStatus waitForPayment(String qrReference, int timeoutSeconds);
}
