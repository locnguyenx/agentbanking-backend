package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.ValidatePINInventoryActivity;
import com.agentbanking.orchestrator.domain.port.out.PINInventoryPort;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ActivityImpl(workers = "agent-banking-tasks")
public class ValidatePINInventoryActivityImpl implements ValidatePINInventoryActivity {

    private final PINInventoryPort port;

    public ValidatePINInventoryActivityImpl(PINInventoryPort port) {
        this.port = port;
    }

    @Override
    public PINInventoryPort.PINInventoryResult validate(String provider, BigDecimal faceValue) {
        return port.validateInventory(provider, faceValue);
    }
}
