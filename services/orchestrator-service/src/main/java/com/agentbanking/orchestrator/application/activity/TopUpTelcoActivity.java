package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.TelcoAggregatorPort.TelcoTopupResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface TopUpTelcoActivity {
    TelcoTopupResult topup(String telcoProvider, String phoneNumber, java.math.BigDecimal amount, String idempotencyKey);
}
