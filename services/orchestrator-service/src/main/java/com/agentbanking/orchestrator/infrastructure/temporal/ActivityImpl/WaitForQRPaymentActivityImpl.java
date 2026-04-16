package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.WaitForQRPaymentActivity;
import com.agentbanking.orchestrator.domain.port.out.QRPaymentPort;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

@ActivityImpl(workers = "agent-banking-tasks")
@Component
public class WaitForQRPaymentActivityImpl implements WaitForQRPaymentActivity {

    private final QRPaymentPort port;

    public WaitForQRPaymentActivityImpl(QRPaymentPort port) {
        this.port = port;
    }

    @Override
    public QRPaymentPort.QRPaymentStatus waitForPayment(String qrReference, int timeoutSeconds) {
        // In production, this would poll or use a callback
        // For now, return initial status
        return port.checkPaymentStatus(qrReference);
    }
}
