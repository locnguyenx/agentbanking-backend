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

@WorkflowImpl(workers = "agent-banking-tasks")
public class ESSPPurchaseWorkflowImpl implements ESSPPurchaseWorkflow {

    private static final Logger log = Workflow.getLogger(ESSPPurchaseWorkflowImpl.class);

    private final ValidateESSPPurchaseActivity validateESSPPurchaseActivity;
    private final PurchaseESSPActivity purchaseESSPActivity;
    private final BlockFloatActivity blockFloatActivity;
    private final CommitFloatActivity commitFloatActivity;
    private final ReleaseFloatActivity releaseFloatActivity;
    private final CreditAgentFloatActivity creditAgentFloatActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    public ESSPPurchaseWorkflowImpl() {
        ActivityOptions defaultOptions = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .build();
        
        this.validateESSPPurchaseActivity = Workflow.newActivityStub(ValidateESSPPurchaseActivity.class, defaultOptions);
        this.purchaseESSPActivity = Workflow.newActivityStub(PurchaseESSPActivity.class, defaultOptions);
        this.blockFloatActivity = Workflow.newActivityStub(BlockFloatActivity.class, defaultOptions);
        this.commitFloatActivity = Workflow.newActivityStub(CommitFloatActivity.class, defaultOptions);
        this.releaseFloatActivity = Workflow.newActivityStub(ReleaseFloatActivity.class, defaultOptions);
        this.creditAgentFloatActivity = Workflow.newActivityStub(CreditAgentFloatActivity.class, defaultOptions);
    }

    @Override
    public WorkflowResult execute(ESSPPurchaseInput input) {
        log.info("Workflow started: ESSPPurchase, agentId={}, amount={}", input.agentId(), input.amount());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            var validationResult = validateESSPPurchaseActivity.validate(input.amount());
            if (!validationResult.valid()) {
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed("ERR_ESSP_INVALID_AMOUNT", "Invalid ESSP amount", "DECLINE");
            }

            FloatBlockResult blockResult = blockFloatActivity.blockFloat(
                    new FloatBlockInput(input.agentId(), input.amount(), input.idempotencyKey()));
            if (!blockResult.success()) {
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed(blockResult.errorCode(), "Float block failed", "DECLINE");
            }
            UUID transactionId = blockResult.transactionId();

            var purchaseResult = purchaseESSPActivity.purchase(input.amount(), input.customerMykad(), input.idempotencyKey());
            if (!purchaseResult.success()) {
                releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), input.amount(), transactionId));
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed(purchaseResult.errorCode(), "ESSP purchase failed", "DECLINE");
            }

            commitFloatActivity.commitFloat(new FloatCommitInput(input.agentId(), input.amount(), transactionId));
            creditAgentFloatActivity.creditAgentFloat(new FloatCreditInput(input.agentId(), input.amount()));

            currentStatus = WorkflowStatus.COMPLETED;
            return WorkflowResult.completed(transactionId, purchaseResult.certificateNumber(), input.amount(), BigDecimal.ZERO);

        } catch (Exception e) {
            log.error("ESSPPurchase workflow failed: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            return WorkflowResult.failed("ERR_SYS_WORKFLOW_FAILED", e.getMessage(), "REVIEW");
        }
    }

    @Override
    public WorkflowStatus getStatus() {
        return currentStatus;
    }
}
