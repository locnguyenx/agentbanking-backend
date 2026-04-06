package com.agentbanking.orchestrator.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "ewallet-provider", url = "${ewallet-provider.url}")
public interface EWalletProviderClient {

    @PostMapping("/internal/validate-wallet")
    EWalletValidationResponse validateWallet(@RequestBody EWalletValidationRequest request);

    @PostMapping("/internal/withdraw")
    EWalletWithdrawResponse withdraw(@RequestBody EWalletWithdrawRequest request);

    @PostMapping("/internal/topup")
    EWalletTopupResponse topup(@RequestBody EWalletTopupRequest request);

    record EWalletValidationRequest(String provider, String walletId) {}
    record EWalletValidationResponse(boolean valid, BigDecimal walletBalance, String errorCode) {}
    record EWalletWithdrawRequest(String provider, String walletId, BigDecimal amount, String idempotencyKey) {}
    record EWalletWithdrawResponse(boolean success, String ewalletReference, String errorCode) {}
    record EWalletTopupRequest(String provider, String walletId, BigDecimal amount, String idempotencyKey) {}
    record EWalletTopupResponse(boolean success, String ewalletReference, String errorCode) {}
}
