package com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.workflow.WithdrawalOnUsWorkflow;
import com.agentbanking.orchestrator.domain.model.ForceResolveSignal;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.out.CbsServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.EventPublisherPort.TransactionCompletedEvent;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.*;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

public class WithdrawalOnUsWorkflowImpl implements WithdrawalOnUsWorkflow {

    private static final Logger log = Workflow.getLogger(WithdrawalOnUsWorkflowImpl.class);

    private final CheckVelocityActivity checkVelocityActivity;
    private final CalculateFeesActivity calculateFeesActivity;
    private final BlockFloatActivity blockFloatActivity;
    private final CommitFloatActivity commitFloatActivity;
    private final ReleaseFloatActivity releaseFloatActivity;
    private final PostToCBSActivity postToCBSActivity;
    private final PublishKafkaEventActivity publishKafkaEventActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    public WithdrawalOnUsWorkflowImpl() {
        ActivityOptions defaultOptions = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(10))
                .setRetryOptions(io.temporal.common.RetryOptions.newBuilder()
                        .setMaximumAttempts(3)
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setMaximumInterval(Duration.ofSeconds(4))
                        .build())
                .build();

        ActivityOptions noRetryOptions = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(10))
                .setRetryOptions(io.temporal.common.RetryOptions.newBuilder()
                        .setMaximumAttempts(1)
                        .build())
                .build();

        ActivityOptions cbsOptions = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(15))
                .setRetryOptions(io.temporal.common.RetryOptions.newBuilder()
                        .setMaximumAttempts(1)
                        .build())
                .build();

        this.checkVelocityActivity = Workflow.newActivityStub(CheckVelocityActivity.class, defaultOptions);
        this.calculateFeesActivity = Workflow.newActivityStub(CalculateFeesActivity.class, defaultOptions);
        this.blockFloatActivity = Workflow.newActivityStub(BlockFloatActivity.class, noRetryOptions);
        this.commitFloatActivity = Workflow.newActivityStub(CommitFloatActivity.class, defaultOptions);
        this.releaseFloatActivity = Workflow.newActivityStub(ReleaseFloatActivity.class, defaultOptions);
        this.postToCBSActivity = Workflow.newActivityStub(PostToCBSActivity.class, cbsOptions);
        this.publishKafkaEventActivity = Workflow.newActivityStub(PublishKafkaEventActivity.class, defaultOptions);
    }

    @Override
    public WorkflowResult execute(WithdrawalOnUsInput input) {
        log.info("Workflow started: WithdrawalOnUs, agentId={}, amount={}", input.agentId(), input.amount());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            // Step 1: Check velocity
            VelocityCheckResult velocityResult = checkVelocityActivity.checkVelocity(
                    new VelocityCheckInput(input.agentId(), input.amount(), input.customerMykad()));
            if (!velocityResult.passed()) {
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed(velocityResult.errorCode(), "Velocity check failed", "DECLINE");
            }

            // Step 2: Calculate fees
            FeeCalculationResult fees = calculateFeesActivity.calculateFees(
                    new FeeCalculationInput("CASH_WITHDRAWAL", input.agentTier(), input.amount()));

            BigDecimal totalAmount = input.amount().add(fees.customerFee());

            // Step 3: Block float
            FloatBlockResult blockResult = blockFloatActivity.blockFloat(
                    new FloatBlockInput(input.agentId(), totalAmount, input.idempotencyKey()));
            if (!blockResult.success()) {
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed(blockResult.errorCode(), "Float block failed", "DECLINE");
            }
            UUID transactionId = blockResult.transactionId();

            // Step 4: Authorize at CBS (no safety reversal — CBS returns definitive result)
            CbsPostResult cbsResult = postToCBSActivity.postToCbs(
                    new CbsPostInput(input.customerAccount(), input.amount()));
            if (!cbsResult.success()) {
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed(cbsResult.errorCode(), "CBS authorization failed", "DECLINE");
            }

            // Step 5: Commit float
            FloatCommitResult commitResult = commitFloatActivity.commitFloat(
                    new FloatCommitInput(input.agentId(), input.amount(), transactionId));
            if (!commitResult.success()) {
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed(commitResult.errorCode(), "Float commit failed", "RETRY");
            }

            // Step 6: Publish event (non-critical)
            try {
                publishKafkaEventActivity.publishCompleted(new TransactionCompletedEvent(
                        transactionId, input.agentId(), input.amount(), fees.customerFee(),
                        fees.agentCommission(), fees.bankShare(), "CASH_WITHDRAWAL",
                        input.customerCardMasked(), cbsResult.reference(), cbsResult.reference()));
            } catch (Exception e) {
                log.warn("Failed to publish Kafka event: {}", e.getMessage());
            }

            currentStatus = WorkflowStatus.COMPLETED;
            return WorkflowResult.completed(transactionId, cbsResult.reference(),
                    input.amount(), fees.customerFee());

        } catch (Exception e) {
            log.error("Workflow failed with exception: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            return WorkflowResult.failed("ERR_SYS_WORKFLOW_FAILED", e.getMessage(), "REVIEW");
        }
    }

    @Override
    public void forceResolve(ForceResolveSignal signal) {
        log.info("Force resolve signal received: action={}", signal.action());
    }

    @Override
    public WorkflowStatus getStatus() {
        return currentStatus;
    }
}