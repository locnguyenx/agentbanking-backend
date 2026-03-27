package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class LedgerServiceAdapter implements LedgerServicePort {

    private static final Logger log = LoggerFactory.getLogger(LedgerServiceAdapter.class);

    private final LedgerServiceClient ledgerServiceClient;

    public LedgerServiceAdapter(LedgerServiceClient ledgerServiceClient) {
        this.ledgerServiceClient = ledgerServiceClient;
    }

    @Override
    public Map<String, Object> blockFloat(Map<String, Object> request) {
        log.info("Blocking float for agent: {}", request.get("agentId"));
        return ledgerServiceClient.blockFloat(request);
    }

    @Override
    public void commitFloat(UUID transactionId) {
        log.info("Committing float for transaction: {}", transactionId);
        ledgerServiceClient.commitFloat(Map.of("transactionId", transactionId.toString()));
    }

    @Override
    public void rollbackFloat(UUID transactionId) {
        log.info("Rolling back float for transaction: {}", transactionId);
        ledgerServiceClient.rollbackFloat(Map.of("transactionId", transactionId.toString()));
    }
}
