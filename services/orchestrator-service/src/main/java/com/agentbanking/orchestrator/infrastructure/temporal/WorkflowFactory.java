package com.agentbanking.orchestrator.infrastructure.temporal;

import com.agentbanking.orchestrator.application.workflow.BillPaymentWorkflow;
import com.agentbanking.orchestrator.application.workflow.DepositWorkflow;
import com.agentbanking.orchestrator.application.workflow.DuitNowTransferWorkflow;
import com.agentbanking.orchestrator.application.workflow.WithdrawalWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WorkflowFactory {

    private final WorkflowClient workflowClient;

    @Value("${temporal.task-queue:agent-banking-tasks}")
    private String taskQueue;

    public WorkflowFactory(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    public String startWithdrawalWorkflow(String idempotencyKey,
                                          WithdrawalWorkflow.WithdrawalInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(idempotencyKey)
                .setTaskQueue(taskQueue)
                .setWorkflowIdReusePolicy(
                        io.temporal.common.WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        WithdrawalWorkflow workflow = workflowClient.newWorkflowStub(
                WithdrawalWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return idempotencyKey;
    }

    public String startDepositWorkflow(String idempotencyKey,
                                        DepositWorkflow.DepositInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(idempotencyKey)
                .setTaskQueue(taskQueue)
                .setWorkflowIdReusePolicy(
                        io.temporal.common.WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        DepositWorkflow workflow = workflowClient.newWorkflowStub(
                DepositWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return idempotencyKey;
    }

    public String startBillPaymentWorkflow(String idempotencyKey,
                                            BillPaymentWorkflow.BillPaymentInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(idempotencyKey)
                .setTaskQueue(taskQueue)
                .setWorkflowIdReusePolicy(
                        io.temporal.common.WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        BillPaymentWorkflow workflow = workflowClient.newWorkflowStub(
                BillPaymentWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return idempotencyKey;
    }

    public String startDuitNowTransferWorkflow(String idempotencyKey,
                                                DuitNowTransferWorkflow.DuitNowTransferInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(idempotencyKey)
                .setTaskQueue(taskQueue)
                .setWorkflowIdReusePolicy(
                        io.temporal.common.WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        DuitNowTransferWorkflow workflow = workflowClient.newWorkflowStub(
                DuitNowTransferWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return idempotencyKey;
    }

    public WorkflowStub getWorkflowStub(String workflowId) {
        return workflowClient.newUntypedWorkflowStub(workflowId);
    }
}