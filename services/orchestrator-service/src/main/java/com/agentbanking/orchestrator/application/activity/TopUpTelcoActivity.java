package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import com.agentbanking.orchestrator.domain.port.out.TelcoAggregatorPort.TelcoTopupResult;


@ActivityInterface
public interface TopUpTelcoActivity {
    @ActivityMethod(name = "telcoTopup")
    TelcoTopupResult topup(String telcoProvider, String phoneNumber, java.math.BigDecimal amount, String idempotencyKey);
}
