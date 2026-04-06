package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.TopUpEWalletActivity;
import com.agentbanking.orchestrator.domain.port.out.EWalletProviderPort;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ActivityImpl(workers = "agent-banking-tasks")
public class TopUpEWalletActivityImpl implements TopUpEWalletActivity {

    private final EWalletProviderPort port;

    public TopUpEWalletActivityImpl(EWalletProviderPort port) {
        this.port = port;
    }

    @Override
    public EWalletProviderPort.EWalletTopupResult topup(String provider, String walletId, BigDecimal amount, String idempotencyKey) {
        return port.topup(provider, walletId, amount, idempotencyKey);
    }
}
