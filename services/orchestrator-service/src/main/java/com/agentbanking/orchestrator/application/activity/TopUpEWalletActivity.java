package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import com.agentbanking.orchestrator.domain.port.out.EWalletProviderPort.EWalletTopupResult;


@ActivityInterface
public interface TopUpEWalletActivity {
    @ActivityMethod(name = "eWalletTopup")
    EWalletTopupResult topup(String provider, String walletId, java.math.BigDecimal amount, String idempotencyKey);
}
