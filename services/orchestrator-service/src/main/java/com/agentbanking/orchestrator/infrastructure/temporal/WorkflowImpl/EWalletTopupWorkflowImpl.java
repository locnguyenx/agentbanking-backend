package com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.activity.ValidateEWalletActivity;
import com.agentbanking.orchestrator.domain.port.out.EWalletProviderPort.EWalletTopupResult;
import com.agentbanking.orchestrator.application.activity.TopUpEWalletActivity;
import com.agentbanking.orchestrator.application.activity.BlockFloatActivity;
import com.agentbanking.orchestrator.application.activity.CommitFloatActivity;
import com.agentbanking.orchestrator.application.activity.ReleaseFloatActivity;
import com.agentbanking.orchestrator.application.workflow.EWalletTopupWorkflow;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.*;

import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@WorkflowImpl(taskQueues = "agent-banking-tasks")
public class EWalletTopupWorkflowImpl implements EWalletTopupWorkflow {

    private static final Logger log = Workflow.getLogger(EWalletTopupWorkflowImpl.class);

    private ValidateEWalletActivity validateEWalletActivity;
    private TopUpEWalletActivity topUpEWalletActivity;
    private BlockFloatActivity blockFloatActivity;
    private CommitFloatActivity commitFloatActivity;
    private ReleaseFloatActivity releaseFloatActivity;
    private PersistWorkflowResultActivity persistWorkflowResultActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    public EWalletTopupWorkflowImpl(
            ValidateEWalletActivity validateEWalletActivity,
            TopUpEWalletActivity topUpEWalletActivity,
            BlockFloatActivity blockFloatActivity,
            CommitFloatActivity commitFloatActivity,
            ReleaseFloatActivity releaseFloatActivity,
            PersistWorkflowResultActivity persistWorkflowResultActivity) {
        this.validateEWalletActivity = validateEWalletActivity;
        this.topUpEWalletActivity = topUpEWalletActivity;
        this.blockFloatActivity = blockFloatActivity;
        this.commitFloatActivity = commitFloatActivity;
        this.releaseFloatActivity = releaseFloatActivity;
        this.persistWorkflowResultActivity = persistWorkflowResultActivity;
    }

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    public EWalletTopupWorkflowImpl() {
        this.validateEWalletActivity = Workflow.newActivityStub(ValidateEWalletActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
        this.topUpEWalletActivity = Workflow.newActivityStub(TopUpEWalletActivity.class, ActivityOptions.newBuilder()
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
        this.persistWorkflowResultActivity = Workflow.newActivityStub(PersistWorkflowResultActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
    }

    @Override
    public WorkflowResult execute(EWalletTopupInput input) {
        log.info("Workflow started: EWalletTopup, agentId={}, provider={}", input.agentId(), input.provider());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            var validationResult = validateEWalletActivity.validate(input.provider(), input.walletId());
            if (!validationResult.valid()) {
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_INVALID_EWALLET", "Invalid eWallet", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Validation failed"));
                return failResult;
            }

            FloatBlockResult blockResult;
            try {
                blockResult = blockFloatActivity.blockFloat(
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
            } catch (ActivityFailure e) {
                log.error("ActivityFailure in blockFloat: {}", e.getMessage());
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILURE", "Float block failed: " + e.getMessage(), "RETRY");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "ActivityFailure: " + e.getMessage()));
                return failResult;
            }
            if (!blockResult.success()) {
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed(blockResult.errorCode(), "Float block failed", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Float block failed"));
                return failResult;
            }
            UUID transactionId = blockResult.transactionId();

            EWalletTopupResult topupResult;
            try {
                topupResult = topUpEWalletActivity.topup(input.provider(), input.walletId(), input.amount(), input.idempotencyKey());
            } catch (ActivityFailure e) {
                log.error("ActivityFailure in topup: {}", e.getMessage());
                releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), input.amount(), transactionId));
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILURE", "Topup failed: " + e.getMessage(), "RETRY");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "ActivityFailure: " + e.getMessage()));
                return failResult;
            }
            if (!topupResult.success()) {
                releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), input.amount(), transactionId));
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed(topupResult.errorCode(), "Topup failed", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Switch authorization failed"));
                return failResult;
            }

            try {
                commitFloatActivity.commitFloat(new FloatCommitInput(input.agentId(), input.amount(), transactionId));
            } catch (ActivityFailure e) {
                log.error("ActivityFailure in commitFloat: {}", e.getMessage());
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILURE", "Float commit failed: " + e.getMessage(), "REVIEW");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "ActivityFailure: " + e.getMessage()));
                return failResult;
            }

            currentStatus = WorkflowStatus.COMPLETED;
            WorkflowResult completedResult = WorkflowResult.completed(transactionId, topupResult.ewalletReference(), input.amount(), BigDecimal.ZERO);
            persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                    Workflow.getInfo().getWorkflowId(), "COMPLETED", null, null, topupResult.ewalletReference(), BigDecimal.ZERO, topupResult.ewalletReference(), null));
            return completedResult;

        } catch (Exception e) {
            log.error("EWalletTopup workflow failed: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            WorkflowResult failResult = WorkflowResult.failed("ERR_SYS_WORKFLOW_FAILED", e.getMessage(), "REVIEW");
            try {
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Workflow exception: " + e.getMessage()));
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
