package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import com.agentbanking.orchestrator.domain.port.out.EWalletProviderPort.EWalletValidationResult;



@ActivityInterface
public interface ValidateEWalletActivity {
    @ActivityMethod(name = "ValidateEWallet")
    EWalletValidationResult validate(String provider, String walletId);
}
