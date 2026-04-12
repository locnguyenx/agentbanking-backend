package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.ValidateEWalletActivity;
import com.agentbanking.orchestrator.domain.port.out.EWalletProviderPort;


import org.springframework.stereotype.Component;

@Component
public class ValidateEWalletActivityImpl implements ValidateEWalletActivity {

    private final EWalletProviderPort port;

    public ValidateEWalletActivityImpl(EWalletProviderPort port) {
        this.port = port;
    }

    @Override
    public EWalletProviderPort.EWalletValidationResult validate(String provider, String walletId) {
        return port.validateWallet(provider, walletId);
    }
}
