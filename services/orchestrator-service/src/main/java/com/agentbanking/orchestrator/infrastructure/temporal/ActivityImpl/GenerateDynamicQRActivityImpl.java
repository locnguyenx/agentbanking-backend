package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.GenerateDynamicQRActivity;
import com.agentbanking.orchestrator.domain.port.out.QRPaymentPort;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@ActivityImpl(workers = "agent-banking-tasks")
public class GenerateDynamicQRActivityImpl implements GenerateDynamicQRActivity {

    private final QRPaymentPort port;

    public GenerateDynamicQRActivityImpl(QRPaymentPort port) {
        this.port = port;
    }

    @Override
    public QRPaymentPort.QRGenerationResult generate(BigDecimal amount, UUID agentId, String idempotencyKey) {
        return port.generateDynamicQR(amount, agentId, idempotencyKey);
    }
}
