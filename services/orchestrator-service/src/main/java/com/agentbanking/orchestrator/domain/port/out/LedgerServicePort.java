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

    record FloatBlockInput(
        UUID agentId,
        BigDecimal amount,
        String idempotencyKey
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
        BigDecimal amount
    ) {}

    record FloatCreditResult(
        boolean success,
        BigDecimal newBalance,
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
}
