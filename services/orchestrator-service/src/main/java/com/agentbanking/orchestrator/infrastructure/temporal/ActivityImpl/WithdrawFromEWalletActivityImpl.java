package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.WithdrawFromEWalletActivity;
import com.agentbanking.orchestrator.domain.port.out.EWalletProviderPort;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@ActivityImpl(workers = "agent-banking-tasks")
@Component
public class WithdrawFromEWalletActivityImpl implements WithdrawFromEWalletActivity {

    private final EWalletProviderPort port;

    public WithdrawFromEWalletActivityImpl(EWalletProviderPort port) {
        this.port = port;
    }

    @Override
    public EWalletProviderPort.EWalletWithdrawResult withdraw(String provider, String walletId, BigDecimal amount, String idempotencyKey) {
        return port.withdraw(provider, walletId, amount, idempotencyKey);
    }
}
