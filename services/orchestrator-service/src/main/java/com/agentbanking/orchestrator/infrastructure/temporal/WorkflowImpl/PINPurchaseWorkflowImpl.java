package com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.activity.ValidatePINInventoryActivity;
import com.agentbanking.orchestrator.application.activity.GeneratePINActivity;
import com.agentbanking.orchestrator.application.activity.BlockFloatActivity;
import com.agentbanking.orchestrator.application.activity.CommitFloatActivity;
import com.agentbanking.orchestrator.application.activity.ReleaseFloatActivity;
import com.agentbanking.orchestrator.application.activity.CreditAgentFloatActivity;
import com.agentbanking.orchestrator.application.workflow.PINPurchaseWorkflow;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatBlockInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatBlockResult;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCommitInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCreditInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatReleaseInput;
import com.agentbanking.orchestrator.domain.port.out.PINInventoryPort.PINGenerationResult;

import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@WorkflowImpl(taskQueues = "agent-banking-tasks")
public class PINPurchaseWorkflowImpl implements PINPurchaseWorkflow {

    private static final Logger log = Workflow.getLogger(PINPurchaseWorkflowImpl.class);

    private ValidatePINInventoryActivity validatePINInventoryActivity;
    private GeneratePINActivity generatePINActivity;
    private BlockFloatActivity blockFloatActivity;
    private CommitFloatActivity commitFloatActivity;
    private ReleaseFloatActivity releaseFloatActivity;
    private CreditAgentFloatActivity creditAgentFloatActivity;
    private PersistWorkflowResultActivity persistWorkflowResultActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    public PINPurchaseWorkflowImpl(
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
    public PINPurchaseWorkflowImpl() {
        this.validatePINInventoryActivity = Workflow.newActivityStub(ValidatePINInventoryActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
        this.generatePINActivity = Workflow.newActivityStub(GeneratePINActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
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
    public WorkflowResult execute(PINPurchaseInput input) {
        log.info("Workflow started: PINPurchase, agentId={}, provider={}, quantity={}", input.agentId(), input.provider(), input.quantity());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            BigDecimal totalAmount = input.faceValue().multiply(BigDecimal.valueOf(input.quantity()));
            UUID transactionId;
            PINGenerationResult pinResult;

            try {
                var inventoryResult = validatePINInventoryActivity.validate(input.provider(), input.faceValue());
                if (!inventoryResult.available()) {
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_PIN_INVENTORY_DEPLETED", "PIN inventory depleted", "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "PIN inventory depleted"));
                    return failResult;
                }
            } catch (ActivityFailure e) {
                log.error("validatePINInventoryActivity failed: {}", e.getMessage());
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILED", "PIN inventory validation failed: " + e.getMessage(), "RETRY");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "ActivityFailure: " + e.getMessage()));
                return failResult;
            }

            try {
                FloatBlockResult blockResult = blockFloatActivity.blockFloat(
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
            if (!blockResult.success()) {
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed(blockResult.errorCode(), "Float block failed", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Float block failed"));
                return failResult;
            }
            transactionId = blockResult.transactionId();
            } catch (ActivityFailure e) {
                log.error("blockFloatActivity failed: {}", e.getMessage());
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILED", "Float block failed: " + e.getMessage(), "RETRY");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "ActivityFailure: " + e.getMessage()));
                return failResult;
            }

            try {
                pinResult = generatePINActivity.generate(input.provider(), input.faceValue(), input.idempotencyKey());
                if (!pinResult.success()) {
                    releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed(pinResult.errorCode(), "PIN generation failed", "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "PIN generation failed"));
                    return failResult;
                }
            } catch (ActivityFailure e) {
                log.error("generatePINActivity failed: {}", e.getMessage());
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILED", "PIN generation failed: " + e.getMessage(), "RETRY");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "ActivityFailure: " + e.getMessage()));
                return failResult;
            }

            try {
                commitFloatActivity.commitFloat(new FloatCommitInput(input.agentId(), totalAmount, transactionId));
            } catch (ActivityFailure e) {
                log.error("commitFloatActivity failed: {}", e.getMessage());
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILED", "Float commit failed: " + e.getMessage(), "RETRY");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "ActivityFailure: " + e.getMessage()));
                return failResult;
            }

            try {
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
            } catch (ActivityFailure e) {
                log.error("creditAgentFloatActivity failed: {}", e.getMessage());
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILED", "Credit agent float failed: " + e.getMessage(), "RETRY");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "ActivityFailure: " + e.getMessage()));
                return failResult;
            }

            currentStatus = WorkflowStatus.COMPLETED;
            WorkflowResult completedResult = WorkflowResult.completed(transactionId, pinResult.pinCode(), totalAmount, BigDecimal.ZERO);
            persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                    Workflow.getInfo().getWorkflowId(), "COMPLETED", null, null, pinResult.pinCode(), BigDecimal.ZERO, pinResult.pinCode(), null));
            return completedResult;

        } catch (Exception e) {
            log.error("PINPurchase workflow failed: {}", e.getMessage());
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
