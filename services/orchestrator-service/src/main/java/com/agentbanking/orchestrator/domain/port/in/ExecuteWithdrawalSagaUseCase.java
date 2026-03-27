package com.agentbanking.orchestrator.domain.port.in;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface ExecuteWithdrawalSagaUseCase {

    SagaResult executeSaga(WithdrawalSagaCommand command);

    record WithdrawalSagaCommand(
        UUID agentId,
        BigDecimal amount,
        String pan,
        String customerCardMasked,
        String idempotencyKey,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng
    ) {}

    record SagaResult(
        String status,
        UUID transactionId,
        String message
    ) {}
}
