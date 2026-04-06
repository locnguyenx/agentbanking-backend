package com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.activity.ValidateEWalletActivity;
import com.agentbanking.orchestrator.application.activity.TopUpEWalletActivity;
import com.agentbanking.orchestrator.application.activity.BlockFloatActivity;
import com.agentbanking.orchestrator.application.activity.CommitFloatActivity;
import com.agentbanking.orchestrator.application.activity.ReleaseFloatActivity;
import com.agentbanking.orchestrator.application.workflow.EWalletTopupWorkflow;
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
public class EWalletTopupWorkflowImpl implements EWalletTopupWorkflow {

    private static final Logger log = Workflow.getLogger(EWalletTopupWorkflowImpl.class);

    private final ValidateEWalletActivity validateEWalletActivity;
    private final TopUpEWalletActivity topUpEWalletActivity;
    private final BlockFloatActivity blockFloatActivity;
    private final CommitFloatActivity commitFloatActivity;
    private final ReleaseFloatActivity releaseFloatActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    public EWalletTopupWorkflowImpl() {
        ActivityOptions defaultOptions = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .build();
        
        this.validateEWalletActivity = Workflow.newActivityStub(ValidateEWalletActivity.class, defaultOptions);
        this.topUpEWalletActivity = Workflow.newActivityStub(TopUpEWalletActivity.class, defaultOptions);
        this.blockFloatActivity = Workflow.newActivityStub(BlockFloatActivity.class, defaultOptions);
        this.commitFloatActivity = Workflow.newActivityStub(CommitFloatActivity.class, defaultOptions);
        this.releaseFloatActivity = Workflow.newActivityStub(ReleaseFloatActivity.class, defaultOptions);
    }

    @Override
    public WorkflowResult execute(EWalletTopupInput input) {
        log.info("Workflow started: EWalletTopup, agentId={}, provider={}", input.agentId(), input.provider());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            var validationResult = validateEWalletActivity.validate(input.provider(), input.walletId());
            if (!validationResult.valid()) {
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed("ERR_INVALID_EWALLET", "Invalid eWallet", "DECLINE");
            }

            FloatBlockResult blockResult = blockFloatActivity.blockFloat(
                    new FloatBlockInput(input.agentId(), input.amount(), input.idempotencyKey()));
            if (!blockResult.success()) {
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed(blockResult.errorCode(), "Float block failed", "DECLINE");
            }
            UUID transactionId = blockResult.transactionId();

            var topupResult = topUpEWalletActivity.topup(input.provider(), input.walletId(), input.amount(), input.idempotencyKey());
            if (!topupResult.success()) {
                releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), input.amount(), transactionId));
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed(topupResult.errorCode(), "Topup failed", "DECLINE");
            }

            commitFloatActivity.commitFloat(new FloatCommitInput(input.agentId(), input.amount(), transactionId));

            currentStatus = WorkflowStatus.COMPLETED;
            return WorkflowResult.completed(transactionId, topupResult.ewalletReference(), input.amount(), BigDecimal.ZERO);

        } catch (Exception e) {
            log.error("EWalletTopup workflow failed: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            return WorkflowResult.failed("ERR_SYS_WORKFLOW_FAILED", e.getMessage(), "REVIEW");
        }
    }

    @Override
    public WorkflowStatus getStatus() {
        return currentStatus;
    }
}
