package com.agentbanking.orchestrator.domain.port.in;

import com.agentbanking.orchestrator.domain.model.TransactionType;
import java.math.BigDecimal;
import java.util.UUID;

public interface StartTransactionUseCase {
    StartTransactionResult start(StartTransactionCommand command);

    record StartTransactionCommand(
        TransactionType transactionType,
        UUID agentId,
        BigDecimal amount,
        String idempotencyKey,
        String pan,
        String pinBlock,
        String customerCardMasked,
        String destinationAccount,
        boolean requiresBiometric,
        String billerCode,
        String ref1,
        String ref2,
        String proxyType,
        String proxyValue,
        String customerMykad,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng,
        String targetBIN,
        String agentTier
    ) {}

    record StartTransactionResult(
        String status,
        String workflowId,
        String pollUrl
    ) {}
}