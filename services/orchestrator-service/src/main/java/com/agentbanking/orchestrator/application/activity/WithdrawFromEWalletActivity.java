package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.EWalletProviderPort.EWalletWithdrawResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface WithdrawFromEWalletActivity {
    EWalletWithdrawResult withdraw(String provider, String walletId, java.math.BigDecimal amount, String idempotencyKey);
}
