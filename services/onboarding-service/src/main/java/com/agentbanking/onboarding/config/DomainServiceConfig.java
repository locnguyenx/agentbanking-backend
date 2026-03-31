package com.agentbanking.onboarding.config;

import com.agentbanking.onboarding.domain.service.AgentService;
import com.agentbanking.onboarding.domain.service.AgentOnboardingService;
import com.agentbanking.onboarding.domain.service.KycDecisionService;
import com.agentbanking.onboarding.domain.port.out.AgentOnboardingRepository;
import com.agentbanking.onboarding.domain.port.out.AgentRepository;
import com.agentbanking.onboarding.domain.port.out.AmlScreeningPort;
import com.agentbanking.onboarding.domain.port.out.GpfenceService;
import com.agentbanking.onboarding.domain.port.out.OcroService;
import com.agentbanking.onboarding.domain.port.out.SsmService;
import com.agentbanking.onboarding.infrastructure.external.AuthUserClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public KycDecisionService kycDecisionService() {
        return new KycDecisionService();
    }

    @Bean
    public AgentService agentService(AgentRepository agentRepository, AuthUserClient authUserClient) {
        return new AgentService(agentRepository, authUserClient);
    }

    @Bean
    public AgentOnboardingService agentOnboardingService(
            AgentOnboardingRepository onboardingRepository,
            AgentRepository agentRepository,
            OcroService ocrService,
            SsmService ssmService,
            AmlScreeningPort amlService,
            GpfenceService gpfenceService) {
        return new AgentOnboardingService(onboardingRepository, agentRepository, ocrService, ssmService, amlService, gpfenceService);
    }
}