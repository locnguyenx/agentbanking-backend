package com.agentbanking.orchestrator.config;

import com.agentbanking.orchestrator.application.activity.AuthorizeAtSwitchActivity;
import com.agentbanking.orchestrator.application.activity.BlockFloatActivity;
import com.agentbanking.orchestrator.application.activity.CalculateFeesActivity;
import com.agentbanking.orchestrator.application.activity.CheckVelocityActivity;
import com.agentbanking.orchestrator.application.activity.CommitFloatActivity;
import com.agentbanking.orchestrator.application.activity.CreditAgentFloatActivity;
import com.agentbanking.orchestrator.application.activity.NotifyBillerActivity;
import com.agentbanking.orchestrator.application.activity.PayBillerActivity;
import com.agentbanking.orchestrator.application.activity.PostToCBSActivity;
import com.agentbanking.orchestrator.application.activity.ProxyEnquiryActivity;
import com.agentbanking.orchestrator.application.activity.PublishKafkaEventActivity;
import com.agentbanking.orchestrator.application.activity.ReleaseFloatActivity;
import com.agentbanking.orchestrator.application.activity.SendDuitNowTransferActivity;
import com.agentbanking.orchestrator.application.activity.SendReversalToSwitchActivity;
import com.agentbanking.orchestrator.application.activity.ValidateAccountActivity;
import com.agentbanking.orchestrator.application.activity.ValidateBillActivity;
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
    public Worker temporalWorker(WorkerFactory factory,
                                  CheckVelocityActivity checkVelocityActivity,
                                  CalculateFeesActivity calculateFeesActivity,
                                  BlockFloatActivity blockFloatActivity,
                                  CommitFloatActivity commitFloatActivity,
                                  ReleaseFloatActivity releaseFloatActivity,
                                  CreditAgentFloatActivity creditAgentFloatActivity,
                                  AuthorizeAtSwitchActivity authorizeAtSwitchActivity,
                                  SendReversalToSwitchActivity sendReversalToSwitchActivity,
                                  PublishKafkaEventActivity publishKafkaEventActivity,
                                  ValidateAccountActivity validateAccountActivity,
                                  PostToCBSActivity postToCBSActivity,
                                  ValidateBillActivity validateBillActivity,
                                  PayBillerActivity payBillerActivity,
                                  NotifyBillerActivity notifyBillerActivity,
                                  ProxyEnquiryActivity proxyEnquiryActivity,
                                  SendDuitNowTransferActivity sendDuitNowTransferActivity) {
        Worker worker = factory.newWorker(taskQueue);

        worker.registerActivitiesImplementations(
                (io.temporal.activity.Activity) checkVelocityActivity,
                (io.temporal.activity.Activity) calculateFeesActivity,
                (io.temporal.activity.Activity) blockFloatActivity,
                (io.temporal.activity.Activity) commitFloatActivity,
                (io.temporal.activity.Activity) releaseFloatActivity,
                (io.temporal.activity.Activity) creditAgentFloatActivity,
                (io.temporal.activity.Activity) authorizeAtSwitchActivity,
                (io.temporal.activity.Activity) sendReversalToSwitchActivity,
                (io.temporal.activity.Activity) publishKafkaEventActivity,
                (io.temporal.activity.Activity) validateAccountActivity,
                (io.temporal.activity.Activity) postToCBSActivity,
                (io.temporal.activity.Activity) validateBillActivity,
                (io.temporal.activity.Activity) payBillerActivity,
                (io.temporal.activity.Activity) notifyBillerActivity,
                (io.temporal.activity.Activity) proxyEnquiryActivity,
                (io.temporal.activity.Activity) sendDuitNowTransferActivity
        );

        return worker;
    }
}