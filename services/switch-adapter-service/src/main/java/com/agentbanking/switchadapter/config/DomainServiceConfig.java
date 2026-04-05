package com.agentbanking.switchadapter.config;

import com.agentbanking.switchadapter.application.usecase.TransactionQuoteUseCaseImpl;
import com.agentbanking.switchadapter.application.usecase.ProxyEnquiryUseCaseImpl;
import com.agentbanking.switchadapter.domain.port.out.FeeCalculationGateway;
import com.agentbanking.switchadapter.domain.port.out.DuitNowProxyGateway;
import com.agentbanking.switchadapter.domain.service.SwitchAdapterService;
import com.agentbanking.switchadapter.domain.port.out.SwitchTransactionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public SwitchAdapterService switchAdapterService(SwitchTransactionRepository repository) {
        return new SwitchAdapterService(repository);
    }

    @Bean
    public TransactionQuoteUseCaseImpl transactionQuoteUseCase(FeeCalculationGateway feeCalculationGateway) {
        return new TransactionQuoteUseCaseImpl(feeCalculationGateway);
    }

    @Bean
    public ProxyEnquiryUseCaseImpl proxyEnquiryUseCase(DuitNowProxyGateway duitNowProxyGateway) {
        return new ProxyEnquiryUseCaseImpl(duitNowProxyGateway);
    }
}
