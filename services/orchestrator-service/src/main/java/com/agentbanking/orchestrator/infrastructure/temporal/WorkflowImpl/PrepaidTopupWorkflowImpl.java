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
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@WorkflowImpl(workers = "agent-banking-tasks")
public class PrepaidTopupWorkflowImpl implements PrepaidTopupWorkflow {

    private static final Logger log = Workflow.getLogger(PrepaidTopupWorkflowImpl.class);

    private final ValidatePhoneNumberActivity validatePhoneNumberActivity;
    private final TopUpTelcoActivity topUpTelcoActivity;
    private final BlockFloatActivity blockFloatActivity;
    private final CommitFloatActivity commitFloatActivity;
    private final ReleaseFloatActivity releaseFloatActivity;
    private final CreditAgentFloatActivity creditAgentFloatActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    public PrepaidTopupWorkflowImpl() {
        ActivityOptions defaultOptions = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .build();
        
        this.validatePhoneNumberActivity = Workflow.newActivityStub(ValidatePhoneNumberActivity.class, defaultOptions);
        this.topUpTelcoActivity = Workflow.newActivityStub(TopUpTelcoActivity.class, defaultOptions);
        this.blockFloatActivity = Workflow.newActivityStub(BlockFloatActivity.class, defaultOptions);
        this.commitFloatActivity = Workflow.newActivityStub(CommitFloatActivity.class, defaultOptions);
        this.releaseFloatActivity = Workflow.newActivityStub(ReleaseFloatActivity.class, defaultOptions);
        this.creditAgentFloatActivity = Workflow.newActivityStub(CreditAgentFloatActivity.class, defaultOptions);
    }

    @Override
    public WorkflowResult execute(PrepaidTopupInput input) {
        log.info("Workflow started: PrepaidTopup, agentId={}, telco={}", input.agentId(), input.telcoProvider());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            var validationResult = validatePhoneNumberActivity.validate(input.phoneNumber(), input.telcoProvider());
            if (!validationResult.valid()) {
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed("ERR_INVALID_PHONE_NUMBER", "Invalid phone number", "DECLINE");
            }

            FloatBlockResult blockResult = blockFloatActivity.blockFloat(
                    new FloatBlockInput(input.agentId(), input.amount(), input.idempotencyKey()));
            if (!blockResult.success()) {
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed(blockResult.errorCode(), "Float block failed", "DECLINE");
            }
            UUID transactionId = blockResult.transactionId();

            var topupResult = topUpTelcoActivity.topup(input.telcoProvider(), input.phoneNumber(), input.amount(), input.idempotencyKey());
            if (!topupResult.success()) {
                releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), input.amount(), transactionId));
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed(topupResult.errorCode(), "Topup failed", "DECLINE");
            }

            commitFloatActivity.commitFloat(new FloatCommitInput(input.agentId(), input.amount(), transactionId));
            creditAgentFloatActivity.creditAgentFloat(new FloatCreditInput(input.agentId(), input.amount()));

            currentStatus = WorkflowStatus.COMPLETED;
            return WorkflowResult.completed(transactionId, topupResult.telcoReference(), input.amount(), BigDecimal.ZERO);

        } catch (Exception e) {
            log.error("PrepaidTopup workflow failed: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            return WorkflowResult.failed("ERR_SYS_WORKFLOW_FAILED", e.getMessage(), "REVIEW");
        }
    }

    @Override
    public WorkflowStatus getStatus() {
        return currentStatus;
    }
}
