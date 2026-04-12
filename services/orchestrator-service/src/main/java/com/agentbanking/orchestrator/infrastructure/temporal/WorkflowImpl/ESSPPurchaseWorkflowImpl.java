package com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.activity.ValidateESSPPurchaseActivity;
import com.agentbanking.orchestrator.application.activity.PurchaseESSPActivity;
import com.agentbanking.orchestrator.application.activity.BlockFloatActivity;
import com.agentbanking.orchestrator.application.activity.CommitFloatActivity;
import com.agentbanking.orchestrator.application.activity.ReleaseFloatActivity;
import com.agentbanking.orchestrator.application.activity.CreditAgentFloatActivity;
import com.agentbanking.orchestrator.application.workflow.ESSPPurchaseWorkflow;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.*;

import io.temporal.activity.ActivityOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@WorkflowImpl(taskQueues = "agent-banking-tasks")
public class ESSPPurchaseWorkflowImpl implements ESSPPurchaseWorkflow {

    private static final Logger log = Workflow.getLogger(ESSPPurchaseWorkflowImpl.class);

    private ValidateESSPPurchaseActivity validateESSPPurchaseActivity;
    private PurchaseESSPActivity purchaseESSPActivity;
    private BlockFloatActivity blockFloatActivity;
    private CommitFloatActivity commitFloatActivity;
    private ReleaseFloatActivity releaseFloatActivity;
    private CreditAgentFloatActivity creditAgentFloatActivity;
    private PersistWorkflowResultActivity persistWorkflowResultActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    public ESSPPurchaseWorkflowImpl(
            ValidateESSPPurchaseActivity validateESSPPurchaseActivity,
            PurchaseESSPActivity purchaseESSPActivity,
            BlockFloatActivity blockFloatActivity,
            CommitFloatActivity commitFloatActivity,
            ReleaseFloatActivity releaseFloatActivity,
            CreditAgentFloatActivity creditAgentFloatActivity,
            PersistWorkflowResultActivity persistWorkflowResultActivity) {
        this.validateESSPPurchaseActivity = validateESSPPurchaseActivity;
        this.purchaseESSPActivity = purchaseESSPActivity;
        this.blockFloatActivity = blockFloatActivity;
        this.commitFloatActivity = commitFloatActivity;
        this.releaseFloatActivity = releaseFloatActivity;
        this.creditAgentFloatActivity = creditAgentFloatActivity;
        this.persistWorkflowResultActivity = persistWorkflowResultActivity;
    }

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    public ESSPPurchaseWorkflowImpl() {
        this.validateESSPPurchaseActivity = Workflow.newActivityStub(ValidateESSPPurchaseActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
        this.purchaseESSPActivity = Workflow.newActivityStub(PurchaseESSPActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(5))
                .build());
        this.blockFloatActivity = Workflow.newActivityStub(BlockFloatActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
        this.commitFloatActivity = Workflow.newActivityStub(CommitFloatActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
        this.releaseFloatActivity = Workflow.newActivityStub(ReleaseFloatActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
        this.creditAgentFloatActivity = Workflow.newActivityStub(CreditAgentFloatActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
        this.persistWorkflowResultActivity = Workflow.newActivityStub(PersistWorkflowResultActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
    }

    @Override
    public WorkflowResult execute(ESSPPurchaseInput input) {
        log.info("Workflow started: ESSPPurchase, agentId={}, amount={}", input.agentId(), input.amount());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            var validationResult = validateESSPPurchaseActivity.validate(input.amount());
            if (!validationResult.valid()) {
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_ESSP_INVALID_AMOUNT", "Invalid ESSP amount", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Validation failed"));
                return failResult;
            }

            FloatBlockResult blockResult = blockFloatActivity.blockFloat(
                    new FloatBlockInput(
                            input.agentId(),
                            input.amount(),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            input.idempotencyKey(),
                            null,
                            input.geofenceLat(),
                            input.geofenceLng(),
                            input.agentTier(),
                            input.targetBin()
                    )
            );
            if (!blockResult.success()) {
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed(blockResult.errorCode(), "Float block failed", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Float block failed"));
                return failResult;
            }
            UUID transactionId = blockResult.transactionId();

            var purchaseResult = purchaseESSPActivity.purchase(input.amount(), input.customerMykad(), input.idempotencyKey());
            if (!purchaseResult.success()) {
                releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), input.amount(), transactionId));
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed(purchaseResult.errorCode(), "ESSP purchase failed", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Switch authorization failed"));
                return failResult;
            }

            commitFloatActivity.commitFloat(new FloatCommitInput(input.agentId(), input.amount(), transactionId));
            creditAgentFloatActivity.creditAgentFloat(
                    new FloatCreditInput(
                            input.agentId(),
                            input.amount(),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            input.idempotencyKey(),
                            null,
                            input.agentTier(),
                            input.targetBin(),
                            purchaseResult.certificateNumber(),
                            input.geofenceLat(),
                            input.geofenceLng()
                    )
            );

            currentStatus = WorkflowStatus.COMPLETED;
            WorkflowResult completedResult = WorkflowResult.completed(transactionId, purchaseResult.certificateNumber(), input.amount(), BigDecimal.ZERO);
            persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                    input.idempotencyKey(), "COMPLETED", null, null, purchaseResult.certificateNumber(), BigDecimal.ZERO, purchaseResult.certificateNumber(), null));
            return completedResult;

        } catch (Exception e) {
            log.error("ESSPPurchase workflow failed: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            WorkflowResult failResult = WorkflowResult.failed("ERR_SYS_WORKFLOW_FAILED", e.getMessage(), "REVIEW");
            try {
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Workflow exception: " + e.getMessage()));
            } catch (Exception ex) {
                log.warn("Failed to persist fail result: {}", ex.getMessage());
            }
            return failResult;
        }
    }

    @Override
    public WorkflowStatus getStatus() {
        return currentStatus;
    }
}
