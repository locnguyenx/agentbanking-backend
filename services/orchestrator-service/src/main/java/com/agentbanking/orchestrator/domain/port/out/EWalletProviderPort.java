package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;

public interface EWalletProviderPort {

    EWalletValidationResult validateWallet(String provider, String walletId);

    EWalletWithdrawResult withdraw(String provider, String walletId, BigDecimal amount, String idempotencyKey);

    EWalletTopupResult topup(String provider, String walletId, BigDecimal amount, String idempotencyKey);

    record EWalletValidationResult(boolean valid, BigDecimal walletBalance, String errorCode) {}
    record EWalletWithdrawResult(boolean success, String ewalletReference, String errorCode) {}
    record EWalletTopupResult(boolean success, String ewalletReference, String errorCode) {}
}
