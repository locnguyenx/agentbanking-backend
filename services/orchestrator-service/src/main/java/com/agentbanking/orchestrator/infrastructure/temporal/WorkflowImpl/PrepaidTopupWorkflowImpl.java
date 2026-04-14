package com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.activity.ValidatePhoneNumberActivity;
import com.agentbanking.orchestrator.application.activity.TopUpTelcoActivity;
import com.agentbanking.orchestrator.application.activity.BlockFloatActivity;
import com.agentbanking.orchestrator.application.activity.CommitFloatActivity;
import com.agentbanking.orchestrator.application.activity.ReleaseFloatActivity;
import com.agentbanking.orchestrator.application.activity.CreditAgentFloatActivity;
import com.agentbanking.orchestrator.application.workflow.PrepaidTopupWorkflow;
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
public class PrepaidTopupWorkflowImpl implements PrepaidTopupWorkflow {

    private static final Logger log = Workflow.getLogger(PrepaidTopupWorkflowImpl.class);

    private ValidatePhoneNumberActivity validatePhoneNumberActivity;
    private TopUpTelcoActivity topUpTelcoActivity;
    private BlockFloatActivity blockFloatActivity;
    private CommitFloatActivity commitFloatActivity;
    private ReleaseFloatActivity releaseFloatActivity;
    private CreditAgentFloatActivity creditAgentFloatActivity;
    private PersistWorkflowResultActivity persistWorkflowResultActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    public PrepaidTopupWorkflowImpl(
            ValidatePhoneNumberActivity validatePhoneNumberActivity,
            TopUpTelcoActivity topUpTelcoActivity,
            BlockFloatActivity blockFloatActivity,
            CommitFloatActivity commitFloatActivity,
            ReleaseFloatActivity releaseFloatActivity,
            CreditAgentFloatActivity creditAgentFloatActivity,
            PersistWorkflowResultActivity persistWorkflowResultActivity) {
        this.validatePhoneNumberActivity = validatePhoneNumberActivity;
        this.topUpTelcoActivity = topUpTelcoActivity;
        this.blockFloatActivity = blockFloatActivity;
        this.commitFloatActivity = commitFloatActivity;
        this.releaseFloatActivity = releaseFloatActivity;
        this.creditAgentFloatActivity = creditAgentFloatActivity;
        this.persistWorkflowResultActivity = persistWorkflowResultActivity;
    }

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    public PrepaidTopupWorkflowImpl() {
        this.validatePhoneNumberActivity = Workflow.newActivityStub(ValidatePhoneNumberActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
        this.topUpTelcoActivity = Workflow.newActivityStub(TopUpTelcoActivity.class, ActivityOptions.newBuilder()
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
    public WorkflowResult execute(PrepaidTopupInput input) {
        log.info("Workflow started: PrepaidTopup, agentId={}, telco={}", input.agentId(), input.telcoProvider());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            var validationResult = validatePhoneNumberActivity.validate(input.phoneNumber(), input.telcoProvider());
            if (!validationResult.valid()) {
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_INVALID_PHONE_NUMBER", "Invalid phone number", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Invalid phone number"));
                return failResult;
            }
        } catch (ActivityFailure e) {
            log.error("ValidatePhone activity failed: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILURE", "Phone validation failed: " + e.getMessage(), "RETRY");
            try {
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Activity failure: " + e.getMessage()));
            } catch (Exception ex) {
                log.warn("Failed to persist fail result: {}", ex.getMessage());
            }
            return failResult;
        }

        FloatBlockResult blockResult = null;
        UUID transactionId = null;
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
            if (!blockResult.success()) {
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed(blockResult.errorCode(), "Float block failed", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Float block failed"));
                return failResult;
            }
            transactionId = blockResult.transactionId();
        } catch (ActivityFailure e) {
            log.error("BlockFloat activity failed: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILURE", "Float block failed: " + e.getMessage(), "RETRY");
            try {
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Activity failure: " + e.getMessage()));
            } catch (Exception ex) {
                log.warn("Failed to persist fail result: {}", ex.getMessage());
            }
            return failResult;
        }

        var topupResult = topUpTelcoActivity.topup(input.telcoProvider(), input.phoneNumber(), input.amount(), input.idempotencyKey());
        try {
            if (!topupResult.success()) {
                releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), input.amount(), transactionId));
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed(topupResult.errorCode(), "Topup failed", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Switch authorization failed"));
                return failResult;
            }
        } catch (ActivityFailure e) {
            log.error("TopupTelco activity failed: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILURE", "Topup failed: " + e.getMessage(), "RETRY");
            try {
                releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), input.amount(), transactionId));
            } catch (Exception ex) {
                log.warn("Failed to release float after topup failure: {}", ex.getMessage());
            }
            try {
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Activity failure: " + e.getMessage()));
            } catch (Exception ex) {
                log.warn("Failed to persist fail result: {}", ex.getMessage());
            }
            return failResult;
        }

        try {
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
                            topupResult.telcoReference(),
                            input.geofenceLat(),
                            input.geofenceLng()
                    )
            );
        } catch (ActivityFailure e) {
            log.error("Commit/CreditFloat activity failed: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILURE", "Float commit/credit failed: " + e.getMessage(), "REVIEW");
            try {
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Activity failure: " + e.getMessage()));
            } catch (Exception ex) {
                log.warn("Failed to persist fail result: {}", ex.getMessage());
            }
            return failResult;
        }

        currentStatus = WorkflowStatus.COMPLETED;
        WorkflowResult completedResult = WorkflowResult.completed(transactionId, topupResult.telcoReference(), input.amount(), BigDecimal.ZERO);
        try {
            persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                    Workflow.getInfo().getWorkflowId(), "COMPLETED", null, null, topupResult.telcoReference(), BigDecimal.ZERO, topupResult.telcoReference(), null));
        } catch (Exception ex) {
            log.warn("Failed to persist completed result: {}", ex.getMessage());
        }
        return completedResult;
    }

    @Override
    public WorkflowStatus getStatus() {
        return currentStatus;
    }
}
