package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;

import com.agentbanking.orchestrator.domain.port.out.EWalletProviderPort.EWalletValidationResult;




@ActivityInterface
public interface ValidateEWalletActivity {
    EWalletValidationResult validate(String provider, String walletId);
}
