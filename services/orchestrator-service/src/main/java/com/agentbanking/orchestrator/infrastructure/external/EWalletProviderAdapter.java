package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.EWalletProviderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class EWalletProviderAdapter implements EWalletProviderPort {

    private static final Logger log = LoggerFactory.getLogger(EWalletProviderAdapter.class);

    private final EWalletProviderClient client;

    public EWalletProviderAdapter(EWalletProviderClient client) {
        this.client = client;
    }

    @Override
    public EWalletValidationResult validateWallet(String provider, String walletId) {
        log.info("Validating eWallet {} for provider: {}", walletId, provider);
        var response = client.validateWallet(new EWalletProviderClient.EWalletValidationRequest(provider, walletId));
        return new EWalletValidationResult(response.valid(), response.walletBalance(), response.errorCode());
    }

    @Override
    public EWalletWithdrawResult withdraw(String provider, String walletId, BigDecimal amount, String idempotencyKey) {
        log.info("Withdrawing {} from eWallet {} via {}", amount, walletId, provider);
        var response = client.withdraw(new EWalletProviderClient.EWalletWithdrawRequest(provider, walletId, amount, idempotencyKey));
        return new EWalletWithdrawResult(response.success(), response.ewalletReference(), response.errorCode());
    }

    @Override
    public EWalletTopupResult topup(String provider, String walletId, BigDecimal amount, String idempotencyKey) {
        log.info("Topping up eWallet {} via {}: {}", walletId, provider, amount);
        var response = client.topup(new EWalletProviderClient.EWalletTopupRequest(provider, walletId, amount, idempotencyKey));
        return new EWalletTopupResult(response.success(), response.ewalletReference(), response.errorCode());
    }
}
