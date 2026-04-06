package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.GeneratePINActivity;
import com.agentbanking.orchestrator.domain.port.out.PINInventoryPort;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ActivityImpl(workers = "agent-banking-tasks")
public class GeneratePINActivityImpl implements GeneratePINActivity {

    private final PINInventoryPort port;

    public GeneratePINActivityImpl(PINInventoryPort port) {
        this.port = port;
    }

    @Override
    public PINInventoryPort.PINGenerationResult generate(String provider, BigDecimal faceValue, String idempotencyKey) {
        return port.generatePIN(provider, faceValue, idempotencyKey);
    }
}
