package com.agentbanking.orchestrator.config;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl.*;
import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalWorkerConfig {

    @Value("${temporal.task-queue:agent-banking-tasks}")
    private String taskQueue;

    @Bean
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        return WorkerFactory.newInstance(workflowClient);
    }

    @Bean
    public Worker temporalWorker(WorkerFactory factory) {
        Worker worker = factory.newWorker(taskQueue);

        worker.registerWorkflowImplementationTypes(
                WithdrawalWorkflowImpl.class,
                WithdrawalOnUsWorkflowImpl.class,
                DepositWorkflowImpl.class,
                BillPaymentWorkflowImpl.class,
                DuitNowTransferWorkflowImpl.class
        );

        return worker;
    }
}
