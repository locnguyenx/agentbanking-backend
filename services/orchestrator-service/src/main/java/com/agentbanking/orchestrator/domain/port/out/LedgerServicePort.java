package com.agentbanking.orchestrator.domain.port.out;

import java.util.Map;
import java.util.UUID;

public interface LedgerServicePort {
    Map<String, Object> blockFloat(Map<String, Object> request);
    void commitFloat(UUID transactionId);
    void rollbackFloat(UUID transactionId);
}
