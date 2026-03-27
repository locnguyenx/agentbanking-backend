package com.agentbanking.orchestrator.config;

import com.agentbanking.orchestrator.domain.service.TransactionOrchestrator;
import com.agentbanking.orchestrator.domain.port.out.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public TransactionOrchestrator transactionOrchestrator(
            IdempotencyService idempotencyService,
            RulesServicePort rulesServicePort,
            LedgerServicePort ledgerServicePort,
            SwitchAdapterPort switchAdapterPort,
            EventPublisherPort eventPublisherPort) {
        return new TransactionOrchestrator(idempotencyService, rulesServicePort, ledgerServicePort, switchAdapterPort, eventPublisherPort);
    }
}
