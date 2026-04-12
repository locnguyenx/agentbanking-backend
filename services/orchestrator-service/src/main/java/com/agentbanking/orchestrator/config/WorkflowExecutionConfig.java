package com.agentbanking.orchestrator.config;

import com.agentbanking.orchestrator.domain.port.out.TransactionRecordRepository;
import com.agentbanking.orchestrator.domain.service.WorkflowExecutionService;
import com.agentbanking.orchestrator.infrastructure.temporal.WorkflowFactory;
import io.temporal.client.WorkflowClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for workflow execution services.
 * Registers domain services as Spring beans while maintaining hexagonal architecture.
 */
@Configuration
public class WorkflowExecutionConfig {

    @Bean
    public WorkflowExecutionService workflowExecutionService(
            WorkflowFactory workflowFactory,
            WorkflowClient workflowClient,
            TransactionRecordRepository transactionRecordRepository,
            @Value("${temporal.namespace:default}") String temporalNamespace) {
        
        return new WorkflowExecutionService(workflowFactory, workflowClient, temporalNamespace, transactionRecordRepository);
    }
}