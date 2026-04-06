package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.EWalletProviderPort.EWalletValidationResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface ValidateEWalletActivity {
    EWalletValidationResult validate(String provider, String walletId);
}
