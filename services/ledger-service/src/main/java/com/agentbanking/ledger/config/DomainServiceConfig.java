package com.agentbanking.ledger.config;

import com.agentbanking.ledger.domain.service.LedgerService;
import com.agentbanking.ledger.domain.port.out.AgentFloatRepository;
import com.agentbanking.ledger.domain.port.out.TransactionRepository;
import com.agentbanking.ledger.domain.port.out.JournalEntryRepository;
import com.agentbanking.ledger.domain.port.out.IdempotencyCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public LedgerService ledgerService(
            AgentFloatRepository agentFloatRepository,
            TransactionRepository transactionRepository,
            JournalEntryRepository journalEntryRepository,
            IdempotencyCache idempotencyCache) {
        return new LedgerService(agentFloatRepository, transactionRepository, journalEntryRepository, idempotencyCache);
    }
}
