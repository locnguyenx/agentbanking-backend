package com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.activity.ValidatePINInventoryActivity;
import com.agentbanking.orchestrator.application.activity.GeneratePINActivity;
import com.agentbanking.orchestrator.application.activity.BlockFloatActivity;
import com.agentbanking.orchestrator.application.activity.CommitFloatActivity;
import com.agentbanking.orchestrator.application.activity.ReleaseFloatActivity;
import com.agentbanking.orchestrator.application.activity.CreditAgentFloatActivity;
import com.agentbanking.orchestrator.application.workflow.PinBasedPurchaseWorkflow;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.PINInventoryPort.PINInventoryResult;
import com.agentbanking.orchestrator.domain.port.out.PINInventoryPort.PINGenerationResult;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.CanceledFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@WorkflowImpl(taskQueues = "agent-banking-tasks")
public class PinBasedPurchaseWorkflowImpl implements PinBasedPurchaseWorkflow {

    private static final Logger log = Workflow.getLogger(PinBasedPurchaseWorkflowImpl.class);

    private ValidatePINInventoryActivity validatePINInventoryActivity;
    private GeneratePINActivity generatePINActivity;
    private BlockFloatActivity blockFloatActivity;
    private CommitFloatActivity commitFloatActivity;
    private ReleaseFloatActivity releaseFloatActivity;
    private CreditAgentFloatActivity creditAgentFloatActivity;
    private PersistWorkflowResultActivity persistWorkflowResultActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    public PinBasedPurchaseWorkflowImpl(
            ValidatePINInventoryActivity validatePINInventoryActivity,
            GeneratePINActivity generatePINActivity,
            BlockFloatActivity blockFloatActivity,
            CommitFloatActivity commitFloatActivity,
            ReleaseFloatActivity releaseFloatActivity,
            CreditAgentFloatActivity creditAgentFloatActivity,
            PersistWorkflowResultActivity persistWorkflowResultActivity) {
        this.validatePINInventoryActivity = validatePINInventoryActivity;
        this.generatePINActivity = generatePINActivity;
        this.blockFloatActivity = blockFloatActivity;
        this.commitFloatActivity = commitFloatActivity;
        this.releaseFloatActivity = releaseFloatActivity;
        this.creditAgentFloatActivity = creditAgentFloatActivity;
        this.persistWorkflowResultActivity = persistWorkflowResultActivity;
    }

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    public PinBasedPurchaseWorkflowImpl() {
        this.validatePINInventoryActivity = Workflow.newActivityStub(ValidatePINInventoryActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.generatePINActivity = Workflow.newActivityStub(GeneratePINActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(60))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.blockFloatActivity = Workflow.newActivityStub(BlockFloatActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.commitFloatActivity = Workflow.newActivityStub(CommitFloatActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.releaseFloatActivity = Workflow.newActivityStub(ReleaseFloatActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.creditAgentFloatActivity = Workflow.newActivityStub(CreditAgentFloatActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.persistWorkflowResultActivity = Workflow.newActivityStub(PersistWorkflowResultActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
    }

    @Override
    public WorkflowResult execute(PinBasedPurchaseInput input) {
        log.info("Workflow started: PinBasedPurchase, agentId={}, provider={}, qty={}", input.agentId(), input.provider(), input.quantity());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            BigDecimal totalAmount = input.faceValue().multiply(BigDecimal.valueOf(input.quantity()));
            
            PINInventoryResult inventoryResult;
            try {
                inventoryResult = validatePINInventoryActivity.validate(input.provider(), input.faceValue());
            } catch (ActivityFailure e) {
                log.error("ActivityFailure in validatePINInventory: {}", e.getMessage());
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILED", "PIN inventory validation activity failed: " + e.getMessage(), "RETRY");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "ActivityFailure: " + e.getMessage()));
                return failResult;
            }
            if (!inventoryResult.available()) {
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_PIN_INVENTORY_DEPLETED", "PIN inventory depleted", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "PIN inventory depleted"));
                return failResult;
            }

            FloatBlockResult blockResult;
            try {
                blockResult = blockFloatActivity.blockFloat(
                        new FloatBlockInput(
                                input.agentId(),
                                totalAmount,
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
                WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILED", "Float block activity failed: " + e.getMessage(), "RETRY");
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

            PINGenerationResult pinResult;
            try {
                pinResult = generatePINActivity.generate(input.provider(), input.faceValue(), input.idempotencyKey());
            } catch (ActivityFailure e) {
                log.error("ActivityFailure in generatePIN: {}", e.getMessage());
                releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILED", "PIN generation activity failed: " + e.getMessage(), "RETRY");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "ActivityFailure: " + e.getMessage()));
                return failResult;
            }
            if (!pinResult.success()) {
                releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed(pinResult.errorCode(), "PIN generation failed", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "PIN generation failed"));
                return failResult;
            }

            commitFloatActivity.commitFloat(new FloatCommitInput(input.agentId(), totalAmount, transactionId));
            creditAgentFloatActivity.creditAgentFloat(
                    new FloatCreditInput(
                            input.agentId(),
                            totalAmount,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            input.idempotencyKey(),
                            null,
                            input.agentTier(),
                            input.targetBin(),
                            pinResult.pinCode(),
                            input.geofenceLat(),
                            input.geofenceLng()
                    )
            );

            currentStatus = WorkflowStatus.COMPLETED;
            WorkflowResult completedResult = WorkflowResult.completed(transactionId, pinResult.pinCode(), totalAmount, BigDecimal.ZERO);
            persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                    Workflow.getInfo().getWorkflowId(), "COMPLETED", null, null, pinResult.pinCode(), BigDecimal.ZERO, pinResult.pinCode(), null));
            return completedResult;

        } catch (CanceledFailure e) {
            log.error("Workflow timed out: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            try {
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", "ERR_WORKFLOW_TIMEOUT", "Workflow timed out - maximum execution time exceeded", null, null, null, "Workflow timeout"));
            } catch (Exception ex) {
                log.warn("Failed to persist timeout status: {}", ex.getMessage());
            }
            throw e;
        } catch (Exception e) {
            log.error("PinBasedPurchase workflow failed: {}", e.getMessage());
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
