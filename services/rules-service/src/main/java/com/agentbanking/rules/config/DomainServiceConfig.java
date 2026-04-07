package com.agentbanking.rules.config;

import com.agentbanking.rules.application.usecase.TransactionQuoteUseCaseImpl;
import com.agentbanking.rules.domain.service.FeeCalculationService;
import com.agentbanking.rules.domain.service.LimitEnforcementService;
import com.agentbanking.rules.domain.service.StpDecisionService;
import com.agentbanking.rules.domain.service.VelocityCheckService;
import com.agentbanking.rules.domain.port.out.FeeCalculationGateway;
import com.agentbanking.rules.domain.port.out.FeeConfigRepository;
import com.agentbanking.rules.domain.port.out.VelocityRuleRepository;
import com.agentbanking.rules.infrastructure.external.FeeCalculationClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public FeeCalculationService feeCalculationService(FeeConfigRepository feeConfigRepository) {
        return new FeeCalculationService(feeConfigRepository);
    }

    @Bean
    public VelocityCheckService velocityCheckService(VelocityRuleRepository velocityRuleRepository) {
        return new VelocityCheckService(velocityRuleRepository);
    }

    @Bean
    public LimitEnforcementService limitEnforcementService() {
        return new LimitEnforcementService();
    }

    @Bean
    public StpDecisionService stpDecisionService(VelocityCheckService velocityCheckService,
                                                    LimitEnforcementService limitEnforcementService) {
        return new StpDecisionService(velocityCheckService, limitEnforcementService);
    }

    @Bean
    public FeeCalculationGateway feeCalculationGateway(FeeCalculationClient feeCalculationClient) {
        return feeCalculationClient;
    }

    @Bean
    public TransactionQuoteUseCaseImpl transactionQuoteUseCase(FeeCalculationGateway feeCalculationGateway) {
        return new TransactionQuoteUseCaseImpl(feeCalculationGateway);
    }
}
