package com.agentbanking.ledger.domain.port.in;

import java.util.Map;
import java.util.UUID;

public interface ReverseTransactionUseCase {
    Map<String, Object> reverseTransaction(UUID transactionId);
}