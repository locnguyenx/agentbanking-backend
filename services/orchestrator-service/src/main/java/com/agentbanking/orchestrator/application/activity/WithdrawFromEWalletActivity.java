package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;

import com.agentbanking.orchestrator.domain.port.out.EWalletProviderPort.EWalletWithdrawResult;




@ActivityInterface
public interface WithdrawFromEWalletActivity {
    EWalletWithdrawResult withdraw(String provider, String walletId, java.math.BigDecimal amount, String idempotencyKey);
}
