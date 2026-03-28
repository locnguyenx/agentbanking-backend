package com.agentbanking.biller.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Use case for processing e-Wallet transactions (Sarawak Pay withdrawal/top-up)
 */
public interface ProcessEWalletUseCase {
    EWalletTransactionResult processEWalletTransaction(EWalletTransactionCommand command);

    record EWalletTransactionCommand(
        String walletProvider,
        String walletId,
        BigDecimal amount,
        UUID internalTransactionId,
        boolean isWithdrawal
    ) {}

    record EWalletTransactionResult(
        String status,
        String transactionId,
        String walletReference
    ) {}
}