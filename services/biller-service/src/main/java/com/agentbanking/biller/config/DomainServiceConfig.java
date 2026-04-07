package com.agentbanking.biller.config;

import com.agentbanking.biller.application.usecase.ProxyEnquiryUseCaseImpl;
import com.agentbanking.biller.domain.port.out.BillerConfigRepository;
import com.agentbanking.biller.domain.port.out.BillPaymentRepository;
import com.agentbanking.biller.domain.port.out.DuitNowProxyGateway;
import com.agentbanking.biller.domain.port.out.EWalletTransactionRepository;
import com.agentbanking.biller.domain.port.out.EsspTransactionRepository;
import com.agentbanking.biller.domain.port.out.TopupTransactionRepository;
import com.agentbanking.biller.domain.service.BillerService;
import com.agentbanking.biller.infrastructure.external.DuitNowProxyClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public BillerService billerService(
            BillerConfigRepository billerConfigRepository,
            BillPaymentRepository billPaymentRepository,
            EWalletTransactionRepository ewalletTransactionRepository,
            EsspTransactionRepository esspTransactionRepository,
            TopupTransactionRepository topupTransactionRepository) {
        return new BillerService(billerConfigRepository, billPaymentRepository,
                ewalletTransactionRepository, esspTransactionRepository, topupTransactionRepository);
    }

    @Bean
    public DuitNowProxyGateway duitNowProxyGateway(DuitNowProxyClient duitNowProxyClient) {
        return duitNowProxyClient;
    }

    @Bean
    public ProxyEnquiryUseCaseImpl proxyEnquiryUseCase(DuitNowProxyGateway duitNowProxyGateway) {
        return new ProxyEnquiryUseCaseImpl(duitNowProxyGateway);
    }
}
