package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.TopUpTelcoActivity;
import com.agentbanking.orchestrator.domain.port.out.TelcoAggregatorPort;


import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ActivityImpl(workers = "agent-banking-tasks")
public class TopUpTelcoActivityImpl implements TopUpTelcoActivity {

    private final TelcoAggregatorPort port;

    public TopUpTelcoActivityImpl(TelcoAggregatorPort port) {
        this.port = port;
    }

    @Override
    public TelcoAggregatorPort.TelcoTopupResult topup(String telcoProvider, String phoneNumber, BigDecimal amount, String idempotencyKey) {
        return port.processTopup(telcoProvider, phoneNumber, amount, idempotencyKey);
    }
}
