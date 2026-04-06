package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.EWalletProviderPort.EWalletTopupResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface TopUpEWalletActivity {
    EWalletTopupResult topup(String provider, String walletId, java.math.BigDecimal amount, String idempotencyKey);
}
