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
import io.temporal.activity.ActivityOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@WorkflowImpl(workers = "agent-banking-tasks")
public class PinBasedPurchaseWorkflowImpl implements PinBasedPurchaseWorkflow {

    private static final Logger log = Workflow.getLogger(PinBasedPurchaseWorkflowImpl.class);

    private final ValidatePINInventoryActivity validatePINInventoryActivity;
    private final GeneratePINActivity generatePINActivity;
    private final BlockFloatActivity blockFloatActivity;
    private final CommitFloatActivity commitFloatActivity;
    private final ReleaseFloatActivity releaseFloatActivity;
    private final CreditAgentFloatActivity creditAgentFloatActivity;
    private final PersistWorkflowResultActivity persistWorkflowResultActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    public PinBasedPurchaseWorkflowImpl() {
        ActivityOptions defaultOptions = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .build();
        
        this.validatePINInventoryActivity = Workflow.newActivityStub(ValidatePINInventoryActivity.class, defaultOptions);
        this.generatePINActivity = Workflow.newActivityStub(GeneratePINActivity.class, defaultOptions);
        this.blockFloatActivity = Workflow.newActivityStub(BlockFloatActivity.class, defaultOptions);
        this.commitFloatActivity = Workflow.newActivityStub(CommitFloatActivity.class, defaultOptions);
        this.releaseFloatActivity = Workflow.newActivityStub(ReleaseFloatActivity.class, defaultOptions);
        this.creditAgentFloatActivity = Workflow.newActivityStub(CreditAgentFloatActivity.class, defaultOptions);
        this.persistWorkflowResultActivity = Workflow.newActivityStub(PersistWorkflowResultActivity.class, defaultOptions);
    }

    @Override
    public WorkflowResult execute(PinBasedPurchaseInput input) {
        log.info("Workflow started: PinBasedPurchase, agentId={}, provider={}, qty={}", input.agentId(), input.provider(), input.quantity());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            BigDecimal totalAmount = input.faceValue().multiply(BigDecimal.valueOf(input.quantity()));
            
            var inventoryResult = validatePINInventoryActivity.validate(input.provider(), input.faceValue());
            if (!inventoryResult.available()) {
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_PIN_INVENTORY_DEPLETED", "PIN inventory depleted", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null));
                return failResult;
            }

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
                        input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null));
                return failResult;
            }
            UUID transactionId = blockResult.transactionId();

            var pinResult = generatePINActivity.generate(input.provider(), input.faceValue(), input.idempotencyKey());
            if (!pinResult.success()) {
                releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed(pinResult.errorCode(), "PIN generation failed", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null));
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
                    input.idempotencyKey(), "COMPLETED", null, null, pinResult.pinCode(), BigDecimal.ZERO, pinResult.pinCode()));
            return completedResult;

        } catch (Exception e) {
            log.error("PinBasedPurchase workflow failed: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            WorkflowResult failResult = WorkflowResult.failed("ERR_SYS_WORKFLOW_FAILED", e.getMessage(), "REVIEW");
            try {
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null));
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
