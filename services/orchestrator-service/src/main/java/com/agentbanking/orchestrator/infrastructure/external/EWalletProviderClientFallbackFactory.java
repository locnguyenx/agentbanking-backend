package com.agentbanking.orchestrator.infrastructure.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EWalletProviderClientFallbackFactory implements FallbackFactory<EWalletProviderClient> {

    private static final Logger log = LoggerFactory.getLogger(EWalletProviderClientFallbackFactory.class);

    @Override
    public EWalletProviderClient create(Throwable cause) {
        log.error("EWalletProviderClient fallback triggered due to: {}", cause.getMessage(), cause);
        return new EWalletProviderClient() {
            @Override
            public EWalletProviderClient.EWalletValidationResponse validateWallet(EWalletProviderClient.EWalletValidationRequest request) {
                log.warn("EWallet provider service unavailable, auto-validating wallet for provider: {}", request.provider());
                return new EWalletProviderClient.EWalletValidationResponse(true, BigDecimal.valueOf(1000.00), "EWALLET_UNAVAILABLE");
            }

            @Override
            public EWalletProviderClient.EWalletWithdrawResponse withdraw(EWalletProviderClient.EWalletWithdrawRequest request) {
                log.warn("EWallet provider service unavailable, auto-approving withdrawal for provider: {}", request.provider());
                return new EWalletProviderClient.EWalletWithdrawResponse(true, "EWALLET_REF_" + System.currentTimeMillis(), "EWALLET_UNAVAILABLE");
            }

            @Override
            public EWalletProviderClient.EWalletTopupResponse topup(EWalletProviderClient.EWalletTopupRequest request) {
                log.warn("EWallet provider service unavailable, auto-approving topup for provider: {}", request.provider());
                return new EWalletProviderClient.EWalletTopupResponse(true, "EWALLET_REF_" + System.currentTimeMillis(), "EWALLET_UNAVAILABLE");
            }
        };
    }
}