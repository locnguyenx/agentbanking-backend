package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface EventPublisherPort {

    void publishTransactionCompleted(TransactionCompletedEvent event);

    void publishTransactionFailed(TransactionFailedEvent event);

    record TransactionCompletedEvent(
        UUID transactionId,
        UUID agentId,
        BigDecimal amount,
        BigDecimal customerFee,
        BigDecimal agentCommission,
        BigDecimal bankShare,
        String transactionType,
        String customerCardMasked,
        String switchTxId,
        String referenceId
    ) {}

    record TransactionFailedEvent(
        UUID transactionId,
        UUID agentId,
        BigDecimal amount,
        String transactionType,
        String customerCardMasked,
        String reason
    ) {}
}
