package com.agentbanking.onboarding.config;

import com.agentbanking.onboarding.domain.service.KycDecisionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public KycDecisionService kycDecisionService() {
        return new KycDecisionService();
    }
}