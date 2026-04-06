package com.agentbanking.orchestrator.config;

import com.agentbanking.orchestrator.domain.port.in.ApproveResolutionUseCase;
import com.agentbanking.orchestrator.domain.port.in.ProposeResolutionUseCase;
import com.agentbanking.orchestrator.domain.port.in.RejectResolutionUseCase;
import com.agentbanking.orchestrator.domain.port.out.ResolutionCaseRepository;
import com.agentbanking.orchestrator.domain.service.ResolutionService;
import com.agentbanking.orchestrator.domain.service.WorkflowRouter;
import com.agentbanking.orchestrator.application.usecase.ApproveResolutionUseCaseImpl;
import com.agentbanking.orchestrator.application.usecase.ProposeResolutionUseCaseImpl;
import com.agentbanking.orchestrator.application.usecase.RejectResolutionUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public WorkflowRouter workflowRouter() {
        return new WorkflowRouter();
    }

    @Bean
    public ResolutionService resolutionService(ResolutionCaseRepository repository) {
        return new ResolutionService(repository);
    }

    @Bean
    public ProposeResolutionUseCase proposeResolutionUseCase(ResolutionService resolutionService) {
        return new ProposeResolutionUseCaseImpl(resolutionService);
    }

    @Bean
    public ApproveResolutionUseCase approveResolutionUseCase(ResolutionService resolutionService) {
        return new ApproveResolutionUseCaseImpl(resolutionService);
    }

    @Bean
    public RejectResolutionUseCase rejectResolutionUseCase(ResolutionService resolutionService) {
        return new RejectResolutionUseCaseImpl(resolutionService);
    }
}
