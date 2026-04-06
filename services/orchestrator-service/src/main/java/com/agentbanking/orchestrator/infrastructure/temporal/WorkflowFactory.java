package com.agentbanking.orchestrator.infrastructure.temporal;

import com.agentbanking.orchestrator.application.workflow.*;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
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

    public String startWorkflow(String idempotencyKey, String transactionType, Object input) {
        return switch (transactionType) {
            case "CASH_WITHDRAWAL" -> startWithdrawalWorkflow(idempotencyKey, (WithdrawalWorkflow.WithdrawalInput) input);
            case "CASH_DEPOSIT" -> startDepositWorkflow(idempotencyKey, (DepositWorkflow.DepositInput) input);
            case "BILL_PAYMENT" -> startBillPaymentWorkflow(idempotencyKey, (BillPaymentWorkflow.BillPaymentInput) input);
            case "DUITNOW_TRANSFER" -> startDuitNowTransferWorkflow(idempotencyKey, (DuitNowTransferWorkflow.DuitNowTransferInput) input);
            case "CASHLESS_PAYMENT" -> startCashlessPaymentWorkflow(idempotencyKey, (CashlessPaymentWorkflow.CashlessPaymentInput) input);
            case "PIN_BASED_PURCHASE" -> startPinBasedPurchaseWorkflow(idempotencyKey, (PinBasedPurchaseWorkflow.PinBasedPurchaseInput) input);
            case "PREPAID_TOPUP" -> startPrepaidTopupWorkflow(idempotencyKey, (PrepaidTopupWorkflow.PrepaidTopupInput) input);
            case "EWALLET_WITHDRAWAL" -> startEWalletWithdrawalWorkflow(idempotencyKey, (EWalletWithdrawalWorkflow.EWalletWithdrawalInput) input);
            case "EWALLET_TOPUP" -> startEWalletTopupWorkflow(idempotencyKey, (EWalletTopupWorkflow.EWalletTopupInput) input);
            case "ESSP_PURCHASE" -> startESSPPurchaseWorkflow(idempotencyKey, (ESSPPurchaseWorkflow.ESSPPurchaseInput) input);
            case "PIN_PURCHASE" -> startPINPurchaseWorkflow(idempotencyKey, (PINPurchaseWorkflow.PINPurchaseInput) input);
            case "RETAIL_SALE" -> startRetailSaleWorkflow(idempotencyKey, (RetailSaleWorkflow.RetailSaleInput) input);
            case "HYBRID_CASHBACK" -> startHybridCashbackWorkflow(idempotencyKey, (HybridCashbackWorkflow.HybridCashbackInput) input);
            default -> throw new IllegalArgumentException("Unknown transaction type: " + transactionType);
        };
    }

    public String startWithdrawalWorkflow(String idempotencyKey,
                                          WithdrawalWorkflow.WithdrawalInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(idempotencyKey)
                .setTaskQueue(taskQueue)
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
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
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
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
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
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
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        DuitNowTransferWorkflow workflow = workflowClient.newWorkflowStub(
                DuitNowTransferWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return idempotencyKey;
    }

    public String startCashlessPaymentWorkflow(String idempotencyKey,
                                                CashlessPaymentWorkflow.CashlessPaymentInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(idempotencyKey)
                .setTaskQueue(taskQueue)
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        CashlessPaymentWorkflow workflow = workflowClient.newWorkflowStub(
                CashlessPaymentWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return idempotencyKey;
    }

    public String startPinBasedPurchaseWorkflow(String idempotencyKey,
                                                  PinBasedPurchaseWorkflow.PinBasedPurchaseInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(idempotencyKey)
                .setTaskQueue(taskQueue)
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        PinBasedPurchaseWorkflow workflow = workflowClient.newWorkflowStub(
                PinBasedPurchaseWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return idempotencyKey;
    }

    public String startPrepaidTopupWorkflow(String idempotencyKey,
                                            PrepaidTopupWorkflow.PrepaidTopupInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(idempotencyKey)
                .setTaskQueue(taskQueue)
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        PrepaidTopupWorkflow workflow = workflowClient.newWorkflowStub(
                PrepaidTopupWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return idempotencyKey;
    }

    public String startEWalletWithdrawalWorkflow(String idempotencyKey,
                                                  EWalletWithdrawalWorkflow.EWalletWithdrawalInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(idempotencyKey)
                .setTaskQueue(taskQueue)
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        EWalletWithdrawalWorkflow workflow = workflowClient.newWorkflowStub(
                EWalletWithdrawalWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return idempotencyKey;
    }

    public String startEWalletTopupWorkflow(String idempotencyKey,
                                             EWalletTopupWorkflow.EWalletTopupInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(idempotencyKey)
                .setTaskQueue(taskQueue)
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        EWalletTopupWorkflow workflow = workflowClient.newWorkflowStub(
                EWalletTopupWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return idempotencyKey;
    }

    public String startESSPPurchaseWorkflow(String idempotencyKey,
                                              ESSPPurchaseWorkflow.ESSPPurchaseInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(idempotencyKey)
                .setTaskQueue(taskQueue)
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        ESSPPurchaseWorkflow workflow = workflowClient.newWorkflowStub(
                ESSPPurchaseWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return idempotencyKey;
    }

    public String startPINPurchaseWorkflow(String idempotencyKey,
                                            PINPurchaseWorkflow.PINPurchaseInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(idempotencyKey)
                .setTaskQueue(taskQueue)
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        PINPurchaseWorkflow workflow = workflowClient.newWorkflowStub(
                PINPurchaseWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return idempotencyKey;
    }

    public String startRetailSaleWorkflow(String idempotencyKey,
                                           RetailSaleWorkflow.RetailSaleInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(idempotencyKey)
                .setTaskQueue(taskQueue)
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        RetailSaleWorkflow workflow = workflowClient.newWorkflowStub(
                RetailSaleWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return idempotencyKey;
    }

    public String startHybridCashbackWorkflow(String idempotencyKey,
                                               HybridCashbackWorkflow.HybridCashbackInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(idempotencyKey)
                .setTaskQueue(taskQueue)
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        HybridCashbackWorkflow workflow = workflowClient.newWorkflowStub(
                HybridCashbackWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return idempotencyKey;
    }

    public WorkflowStub getWorkflowStub(String workflowId) {
        return workflowClient.newUntypedWorkflowStub(workflowId);
    }
}
