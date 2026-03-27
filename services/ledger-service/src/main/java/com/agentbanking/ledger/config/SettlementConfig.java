package com.agentbanking.ledger.config;

import com.agentbanking.ledger.domain.port.out.CbsFileGenerator;
import com.agentbanking.ledger.domain.port.out.SettlementSummaryRepository;
import com.agentbanking.ledger.domain.port.out.TransactionRepository;
import com.agentbanking.ledger.domain.service.SettlementService;
import com.agentbanking.ledger.infrastructure.external.CbsFileGeneratorAdapter;
import com.agentbanking.ledger.infrastructure.persistence.repository.SettlementSummaryJpaRepository;
import com.agentbanking.ledger.infrastructure.persistence.repository.SettlementSummaryRepositoryAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SettlementConfig {

    @Bean
    public SettlementSummaryRepository settlementSummaryRepository(
            SettlementSummaryJpaRepository jpaRepository) {
        return new SettlementSummaryRepositoryAdapter(jpaRepository);
    }

    @Bean
    public SettlementService settlementService(
            TransactionRepository transactionRepository,
            SettlementSummaryRepository settlementSummaryRepository,
            CbsFileGenerator cbsFileGenerator) {
        return new SettlementService(transactionRepository, settlementSummaryRepository, cbsFileGenerator);
    }
}
