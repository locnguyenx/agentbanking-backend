package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface LedgerServicePort {

    FloatBlockResult blockFloat(FloatBlockInput input);

    FloatCommitResult commitFloat(FloatCommitInput input);

    FloatReleaseResult releaseFloat(FloatReleaseInput input);

    FloatCreditResult creditAgentFloat(FloatCreditInput input);

    FloatReverseResult reverseCreditFloat(FloatReverseInput input);

    AccountValidationResult validateAccount(AccountValidationInput input);

    TransactionDetailsResult getTransactionDetails(UUID transactionId);

    record FloatBlockInput(
        UUID agentId,
        BigDecimal amount,
        BigDecimal customerFee,
        BigDecimal agentCommission,
        BigDecimal bankShare,
        String idempotencyKey,
        String customerCardMasked,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng,
        String agentTier,
        String targetBin
    ) {}

    record FloatBlockResult(
        boolean success,
        UUID transactionId,
        String errorCode
    ) {}

    record FloatCommitInput(
        UUID agentId,
        BigDecimal amount,
        UUID transactionId
    ) {}

    record FloatCommitResult(
        boolean success,
        String errorCode
    ) {}

    record FloatReleaseInput(
        UUID agentId,
        BigDecimal amount,
        UUID transactionId
    ) {}

    record FloatReleaseResult(
        boolean success,
        String errorCode
    ) {}

    record FloatCreditInput(
        UUID agentId,
        BigDecimal amount,
        BigDecimal customerFee,
        BigDecimal agentCommission,
        BigDecimal bankShare,
        String idempotencyKey,
        String destinationAccount,
        String agentTier,
        String targetBin,
        String referenceNumber,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng
    ) {}

    record FloatCreditResult(
        boolean success,
        BigDecimal newBalance,
        UUID transactionId,
        String errorCode
    ) {}

    record FloatReverseInput(
        UUID agentId,
        BigDecimal amount
    ) {}

    record FloatReverseResult(
        boolean success,
        String errorCode
    ) {}

    record AccountValidationInput(
        String destinationAccount
    ) {}

    record AccountValidationResult(
        boolean valid,
        String accountName,
        String errorCode
    ) {}

    record TransactionDetailsResult(
        UUID transactionId,
        UUID agentId,
        String transactionType,
        BigDecimal amount,
        BigDecimal customerFee,
        BigDecimal agentCommission,
        BigDecimal bankShare,
        String status,
        String errorCode,
        String customerCardMasked,
        String referenceNumber,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng,
        String agentTier,
        String targetBin,
        String billerCode,
        String ref1,
        String ref2,
        String destinationAccount,
        String createdAt,
        String completedAt
    ) {}
}
