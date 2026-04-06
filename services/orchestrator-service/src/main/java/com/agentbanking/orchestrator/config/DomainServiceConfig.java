package com.agentbanking.orchestrator.config;

import com.agentbanking.orchestrator.domain.service.WorkflowRouter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public WorkflowRouter workflowRouter() {
        return new WorkflowRouter();
    }
}
