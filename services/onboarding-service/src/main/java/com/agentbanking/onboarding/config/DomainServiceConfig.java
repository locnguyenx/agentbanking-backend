package com.agentbanking.onboarding.config;

import com.agentbanking.onboarding.domain.service.AgentService;
import com.agentbanking.onboarding.domain.service.KycDecisionService;
import com.agentbanking.onboarding.domain.port.out.AgentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public KycDecisionService kycDecisionService() {
        return new KycDecisionService();
    }

    @Bean
    public AgentService agentService(AgentRepository agentRepository) {
        return new AgentService(agentRepository);
    }
}