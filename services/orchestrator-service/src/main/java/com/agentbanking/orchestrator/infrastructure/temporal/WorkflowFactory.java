package com.agentbanking.orchestrator.infrastructure.temporal;

import com.agentbanking.orchestrator.application.workflow.*;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class WorkflowFactory {

    private final WorkflowClient workflowClient;

    @Value("${temporal.task-queue:agent-banking-tasks}")
    private String taskQueue;

    public WorkflowFactory(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    public String startWorkflow(String idempotencyKey, String transactionType, Object input) {
        String workflowId = idempotencyKey != null && !idempotencyKey.isBlank() 
                ? idempotencyKey 
                : UUID.randomUUID().toString();
        
        return switch (transactionType) {
            case "CASH_WITHDRAWAL" -> startWithdrawalWorkflow(workflowId, (WithdrawalWorkflow.WithdrawalInput) input);
            case "CASH_DEPOSIT" -> startDepositWorkflow(workflowId, (DepositWorkflow.DepositInput) input);
            case "BILL_PAYMENT" -> startBillPaymentWorkflow(workflowId, (BillPaymentWorkflow.BillPaymentInput) input);
            case "DUITNOW_TRANSFER" -> startDuitNowTransferWorkflow(workflowId, (DuitNowTransferWorkflow.DuitNowTransferInput) input);
            case "CASHLESS_PAYMENT" -> startCashlessPaymentWorkflow(workflowId, (CashlessPaymentWorkflow.CashlessPaymentInput) input);
            case "PIN_BASED_PURCHASE" -> startPinBasedPurchaseWorkflow(workflowId, (PinBasedPurchaseWorkflow.PinBasedPurchaseInput) input);
            case "PREPAID_TOPUP" -> startPrepaidTopupWorkflow(workflowId, (PrepaidTopupWorkflow.PrepaidTopupInput) input);
            case "EWALLET_WITHDRAWAL" -> startEWalletWithdrawalWorkflow(workflowId, (EWalletWithdrawalWorkflow.EWalletWithdrawalInput) input);
            case "EWALLET_TOPUP" -> startEWalletTopupWorkflow(workflowId, (EWalletTopupWorkflow.EWalletTopupInput) input);
            case "ESSP_PURCHASE" -> startESSPPurchaseWorkflow(workflowId, (ESSPPurchaseWorkflow.ESSPPurchaseInput) input);
            case "PIN_PURCHASE" -> startPINPurchaseWorkflow(workflowId, (PINPurchaseWorkflow.PINPurchaseInput) input);
            case "RETAIL_SALE" -> startRetailSaleWorkflow(workflowId, (RetailSaleWorkflow.RetailSaleInput) input);
            case "HYBRID_CASHBACK" -> startHybridCashbackWorkflow(workflowId, (HybridCashbackWorkflow.HybridCashbackInput) input);
            default -> throw new IllegalArgumentException("Unknown transaction type: " + transactionType);
        };
    }

    public String startWithdrawalWorkflow(String workflowId,
                                          WithdrawalWorkflow.WithdrawalInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(taskQueue)
                .setWorkflowExecutionTimeout(Duration.ofMinutes(1))
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        WithdrawalWorkflow workflow = workflowClient.newWorkflowStub(
                WithdrawalWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return workflowId;
    }

    public String startDepositWorkflow(String workflowId,
                                        DepositWorkflow.DepositInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(taskQueue)
                .setWorkflowExecutionTimeout(Duration.ofMinutes(1))
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        DepositWorkflow workflow = workflowClient.newWorkflowStub(
                DepositWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return workflowId;
    }

    public String startBillPaymentWorkflow(String workflowId,
                                            BillPaymentWorkflow.BillPaymentInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(taskQueue)
                .setWorkflowExecutionTimeout(Duration.ofMinutes(1))
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        BillPaymentWorkflow workflow = workflowClient.newWorkflowStub(
                BillPaymentWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return workflowId;
    }

    public String startDuitNowTransferWorkflow(String workflowId,
                                                DuitNowTransferWorkflow.DuitNowTransferInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(taskQueue)
                .setWorkflowExecutionTimeout(Duration.ofMinutes(1))
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        DuitNowTransferWorkflow workflow = workflowClient.newWorkflowStub(
                DuitNowTransferWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return workflowId;
    }

    public String startCashlessPaymentWorkflow(String workflowId,
                                                CashlessPaymentWorkflow.CashlessPaymentInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(taskQueue)
                .setWorkflowExecutionTimeout(Duration.ofMinutes(1))
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        CashlessPaymentWorkflow workflow = workflowClient.newWorkflowStub(
                CashlessPaymentWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return workflowId;
    }

    public String startPinBasedPurchaseWorkflow(String workflowId,
                                                  PinBasedPurchaseWorkflow.PinBasedPurchaseInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(taskQueue)
                .setWorkflowExecutionTimeout(Duration.ofMinutes(1))
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        PinBasedPurchaseWorkflow workflow = workflowClient.newWorkflowStub(
                PinBasedPurchaseWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return workflowId;
    }

    public String startPrepaidTopupWorkflow(String workflowId,
                                            PrepaidTopupWorkflow.PrepaidTopupInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(taskQueue)
                .setWorkflowExecutionTimeout(Duration.ofMinutes(1))
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        PrepaidTopupWorkflow workflow = workflowClient.newWorkflowStub(
                PrepaidTopupWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return workflowId;
    }

    public String startEWalletWithdrawalWorkflow(String workflowId,
                                                  EWalletWithdrawalWorkflow.EWalletWithdrawalInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(taskQueue)
                .setWorkflowExecutionTimeout(Duration.ofMinutes(1))
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        EWalletWithdrawalWorkflow workflow = workflowClient.newWorkflowStub(
                EWalletWithdrawalWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return workflowId;
    }

    public String startEWalletTopupWorkflow(String workflowId,
                                             EWalletTopupWorkflow.EWalletTopupInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(taskQueue)
                .setWorkflowExecutionTimeout(Duration.ofMinutes(1))
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        EWalletTopupWorkflow workflow = workflowClient.newWorkflowStub(
                EWalletTopupWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return workflowId;
    }

    public String startESSPPurchaseWorkflow(String workflowId,
                                              ESSPPurchaseWorkflow.ESSPPurchaseInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(taskQueue)
                .setWorkflowExecutionTimeout(Duration.ofMinutes(1))
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        ESSPPurchaseWorkflow workflow = workflowClient.newWorkflowStub(
                ESSPPurchaseWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return workflowId;
    }

    public String startPINPurchaseWorkflow(String workflowId,
                                            PINPurchaseWorkflow.PINPurchaseInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(taskQueue)
                .setWorkflowExecutionTimeout(Duration.ofMinutes(1))
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        PINPurchaseWorkflow workflow = workflowClient.newWorkflowStub(
                PINPurchaseWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return workflowId;
    }

    public String startRetailSaleWorkflow(String workflowId,
                                           RetailSaleWorkflow.RetailSaleInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(taskQueue)
                .setWorkflowExecutionTimeout(Duration.ofMinutes(1))
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        RetailSaleWorkflow workflow = workflowClient.newWorkflowStub(
                RetailSaleWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return workflowId;
    }

    public String startHybridCashbackWorkflow(String workflowId,
                                               HybridCashbackWorkflow.HybridCashbackInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(taskQueue)
                .setWorkflowExecutionTimeout(Duration.ofMinutes(1))
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        HybridCashbackWorkflow workflow = workflowClient.newWorkflowStub(
                HybridCashbackWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return workflowId;
    }

    public WorkflowStub getWorkflowStub(String workflowId) {
        return workflowClient.newUntypedWorkflowStub(workflowId);
    }
}
