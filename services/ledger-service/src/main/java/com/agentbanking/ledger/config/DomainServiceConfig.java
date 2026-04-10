package com.agentbanking.ledger.config;

import com.agentbanking.ledger.domain.port.out.*;
import com.agentbanking.ledger.domain.service.LedgerService;
import com.agentbanking.ledger.domain.service.MerchantTransactionService;
import com.agentbanking.ledger.domain.service.ReconciliationService;
import com.agentbanking.common.efm.EfmEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public LedgerService ledgerService(
            AgentFloatRepository agentFloatRepository,
            TransactionRepository transactionRepository,
            JournalEntryRepository journalEntryRepository,
            IdempotencyCache idempotencyCache,
            SwitchServicePort switchService,
            AgentRepository agentRepository,
            MerchantTransactionService merchantTransactionService,
            EfmEventPublisher efmEventPublisher) {
        return new LedgerService(
                agentFloatRepository,
                transactionRepository,
                journalEntryRepository,
                idempotencyCache,
                switchService,
                agentRepository,
                merchantTransactionService,
                efmEventPublisher);
    }

    @Bean
    public MerchantTransactionService merchantTransactionService() {
        return new MerchantTransactionService();
    }

    @Bean
    public ReconciliationService reconciliationService() {
        return new ReconciliationService();
    }
}
