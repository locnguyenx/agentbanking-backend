package com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.workflow.WithdrawalWorkflow;
import com.agentbanking.orchestrator.domain.model.ForceResolveSignal;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.out.EventPublisherPort.TransactionCompletedEvent;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.*;
import io.temporal.failure.CanceledFailure;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@WorkflowImpl(taskQueues = "agent-banking-tasks")
public class WithdrawalWorkflowImpl implements WithdrawalWorkflow {

    private static final Logger log = Workflow.getLogger(WithdrawalWorkflowImpl.class);

    private CheckVelocityActivity checkVelocityActivity;
    private EvaluateStpActivity evaluateStpActivity;
    private CalculateFeesActivity calculateFeesActivity;
    private BlockFloatActivity blockFloatActivity;
    private CommitFloatActivity commitFloatActivity;
    private ReleaseFloatActivity releaseFloatActivity;
    private AuthorizeAtSwitchActivity authorizeAtSwitchActivity;
    private SendReversalToSwitchActivity sendReversalToSwitchActivity;
    private PublishKafkaEventActivity publishKafkaEventActivity;
    private SaveResolutionCaseActivity saveResolutionCaseActivity;
    private PersistWorkflowResultActivity persistWorkflowResultActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    public WithdrawalWorkflowImpl(
            CheckVelocityActivity checkVelocityActivity,
            EvaluateStpActivity evaluateStpActivity,
            CalculateFeesActivity calculateFeesActivity,
            BlockFloatActivity blockFloatActivity,
            CommitFloatActivity commitFloatActivity,
            ReleaseFloatActivity releaseFloatActivity,
            AuthorizeAtSwitchActivity authorizeAtSwitchActivity,
            SendReversalToSwitchActivity sendReversalToSwitchActivity,
            PublishKafkaEventActivity publishKafkaEventActivity,
            SaveResolutionCaseActivity saveResolutionCaseActivity,
            PersistWorkflowResultActivity persistWorkflowResultActivity) {
        this.checkVelocityActivity = checkVelocityActivity;
        this.evaluateStpActivity = evaluateStpActivity;
        this.calculateFeesActivity = calculateFeesActivity;
        this.blockFloatActivity = blockFloatActivity;
        this.commitFloatActivity = commitFloatActivity;
        this.releaseFloatActivity = releaseFloatActivity;
        this.authorizeAtSwitchActivity = authorizeAtSwitchActivity;
        this.sendReversalToSwitchActivity = sendReversalToSwitchActivity;
        this.publishKafkaEventActivity = publishKafkaEventActivity;
        this.saveResolutionCaseActivity = saveResolutionCaseActivity;
        this.persistWorkflowResultActivity = persistWorkflowResultActivity;
    }

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    public WithdrawalWorkflowImpl() {
        // CheckVelocity: StartToClose 3s | Retry 1x (for testing)
        this.checkVelocityActivity = Workflow.newActivityStub(CheckVelocityActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(3)
                        .build())
                .build());

        // EvaluateStp: StartToClose 2s | Retry 1x (for testing)
        this.evaluateStpActivity = Workflow.newActivityStub(EvaluateStpActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(3)
                        .build())
                .build());

        // CalculateFees: StartToClose 2s | Retry 1x (for testing)
        this.calculateFeesActivity = Workflow.newActivityStub(CalculateFeesActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(3)
                        .build())
                .build());

        // BlockFloat: StartToClose 3s | Retry 0x (financial, idempotent - fail fast)
        this.blockFloatActivity = Workflow.newActivityStub(BlockFloatActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(3)
                        .build())
                .build());

        // CommitFloat: StartToClose 3s | Retry 1x (for testing)
        this.commitFloatActivity = Workflow.newActivityStub(CommitFloatActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(3)
                        .build())
                .build());

        // ReleaseFloat: StartToClose 3s | Retry 1x (for testing)
        this.releaseFloatActivity = Workflow.newActivityStub(ReleaseFloatActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(3)
                        .build())
                .build());

        // AuthorizeAtSwitch: StartToClose 5s | Retry 0x (financial)
        this.authorizeAtSwitchActivity = Workflow.newActivityStub(AuthorizeAtSwitchActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(3)
                        .build())
                .build());

        // SendReversalToSwitch: StartToClose 10s, ScheduleToClose 60s | Retry infinite, 60s interval (Store & Forward)
        this.sendReversalToSwitchActivity = Workflow.newActivityStub(SendReversalToSwitchActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(60))
                .setScheduleToCloseTimeout(Duration.ofSeconds(60))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(60))
                        .setMaximumInterval(Duration.ofSeconds(60))
                        .setBackoffCoefficient(1)
                        .setMaximumAttempts(Integer.MAX_VALUE) // Infinite retry until PayNet acknowledges
                        .build())
                .build());

        // PublishKafkaEvent: StartToClose 3s | Retry 3x (1s→2s→4s)
        // Non-critical: log and continue on failure - don't block workflow completion
        this.publishKafkaEventActivity = Workflow.newActivityStub(PublishKafkaEventActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setMaximumInterval(Duration.ofSeconds(2))
                        .setBackoffCoefficient(2)
                        .setMaximumAttempts(3) // Only 1 attempt - fail fast, don't retry
                        .build())
                .build());

        // SaveResolutionCase: StartToClose 2s | Retry 3x (1s→2s→4s)
        this.saveResolutionCaseActivity = Workflow.newActivityStub(SaveResolutionCaseActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setMaximumInterval(Duration.ofSeconds(4))
                        .setBackoffCoefficient(2)
                        .setMaximumAttempts(3)
                        .build())
                .build());

        // PersistWorkflowResult: StartToClose 2s | Retry 3x (1s→2s→4s) - critical for status updates
        this.persistWorkflowResultActivity = Workflow.newActivityStub(PersistWorkflowResultActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setMaximumInterval(Duration.ofSeconds(4))
                        .setBackoffCoefficient(2)
                        .setMaximumAttempts(3)
                        .build())
                .build());
    }

    @Override
    public WorkflowResult execute(WithdrawalInput input) {
        log.info("Workflow started: Withdrawal, agentId={}, amount={}", input.agentId(), input.amount());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            VelocityCheckResult velocityResult = checkVelocityActivity.checkVelocity(
                    new VelocityCheckInput(input.agentId(), input.amount(), input.customerMykad()));
            if (!velocityResult.passed()) {
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed(velocityResult.errorCode(), "Velocity check failed", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Velocity check failed"));
                return failResult;
            }

            StpDecision stpDecision;
            try {
                stpDecision = evaluateStpActivity.evaluateStp(
                        new EvaluateStpActivity.Input(
                                "CASH_WITHDRAWAL",
                                input.agentId().toString(),
                                input.amount().toString(),
                                input.customerMykad(),
                                input.agentTier(),
                                0,
                                "0",
                                "0"
                        )
                );
            } catch (ActivityFailure e) {
                log.error("STP evaluation failed after retries: {}", e.getMessage());
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult stpFailResult = WorkflowResult.failed("ERR_STP_UNAVAILABLE", "STP service unavailable after retries", "RETRY");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", stpFailResult.errorCode(), stpFailResult.errorMessage(), null, null, null, "STP service unavailable: " + e.getMessage()));
                return stpFailResult;
            }
            if (!stpDecision.approved()) {
                // Auto-create resolution case for non-STP transactions
                if (stpDecision.category().equals("NON_STP") || 
                    stpDecision.category().equals("CONDITIONAL_STP")) {
                    saveResolutionCaseActivity.saveResolutionCase(
                        new SaveResolutionCaseActivity.Input(
                            Workflow.getInfo().getWorkflowId(),
                            null, // transactionId not available yet
                            stpDecision.category(),
                            stpDecision.reason()
                        )
                    );
                }
                currentStatus = WorkflowStatus.PENDING_REVIEW;
                WorkflowResult stpResult = WorkflowResult.failed("ERR_STP_REVIEW", stpDecision.reason(), "REVIEW");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "PENDING_REVIEW", stpResult.errorCode(), stpResult.errorMessage(), null, null, null, stpDecision.reason()));
                return stpResult;
            }

            FeeCalculationResult fees = calculateFeesActivity.calculateFees(
                    new FeeCalculationInput("CASH_WITHDRAWAL", input.agentTier(), input.amount()));

            BigDecimal totalAmount = input.amount().add(fees.customerFee());

            FloatBlockResult blockResult = blockFloatActivity.blockFloat(
                    new FloatBlockInput(
                            input.agentId(),
                            input.amount(),
                            fees.customerFee(),
                            fees.agentCommission(),
                            fees.bankShare(),
                            Workflow.getInfo().getWorkflowId(),
                            input.customerCardMasked(),
                            input.geofenceLat(),
                            input.geofenceLng(),
                            input.agentTier(),
                            input.targetBin()
                    )
            );
            if (!blockResult.success()) {
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult blockFailResult = WorkflowResult.failed(blockResult.errorCode(), "Float block failed", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", blockFailResult.errorCode(), blockFailResult.errorMessage(), null, null, null, "Float block failed"));
                return blockFailResult;
            }
            UUID transactionId = blockResult.transactionId();

            SwitchAuthorizationResult authResult = authorizeAtSwitchActivity.authorize(
                    new SwitchAuthorizationInput(input.pan(), input.pinBlock(), input.amount(), transactionId));

            if (!authResult.approved()) {
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), totalAmount, transactionId));

                if ("TIMEOUT".equals(authResult.responseCode())) {
                    triggerSafetyReversal(transactionId, input.agentId(), totalAmount);
                }

                String errorCode = authResult.errorCode() != null ? authResult.errorCode() : "ERR_SWITCH_DECLINED";
                String actionCode = "TIMEOUT".equals(authResult.responseCode()) ? "RETRY" : "DECLINE";
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult authFailResult = WorkflowResult.failed(errorCode, "Switch authorization failed", actionCode);
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", authFailResult.errorCode(), authFailResult.errorMessage(), null, null, null, "Switch authorization failed"));
                return authFailResult;
            }

            FloatCommitResult commitResult = commitFloatActivity.commitFloat(
                    new FloatCommitInput(input.agentId(), input.amount(), transactionId));
            if (!commitResult.success()) {
                triggerSafetyReversal(transactionId, input.agentId(), totalAmount);
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult commitFailResult = WorkflowResult.failed(commitResult.errorCode(), "Float commit failed", "RETRY");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", commitFailResult.errorCode(), commitFailResult.errorMessage(), null, null, null, "Float commit failed"));
                return commitFailResult;
            }

            try {
                publishKafkaEventActivity.publishCompleted(new TransactionCompletedEvent(
                        transactionId, input.agentId(), input.amount(), fees.customerFee(),
                        fees.agentCommission(), fees.bankShare(), "CASH_WITHDRAWAL",
                        input.customerCardMasked(), authResult.referenceCode(), authResult.referenceCode()));
            } catch (Exception e) {
                log.warn("Failed to publish Kafka event: {}", e.getMessage());
            }

            currentStatus = WorkflowStatus.COMPLETED;
            WorkflowResult completedResult = WorkflowResult.completed(transactionId, authResult.referenceCode(),
                    input.amount(), fees.customerFee());
            persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                    Workflow.getInfo().getWorkflowId(), "COMPLETED", null, null,
                    authResult.referenceCode(), fees.customerFee(), authResult.referenceCode(), null,
                    java.time.LocalDateTime.now()));
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
            log.error("Workflow failed with exception: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            WorkflowResult sysFailResult = WorkflowResult.failed("ERR_SYS_WORKFLOW_FAILED", e.getMessage(), "REVIEW");
            try {
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", sysFailResult.errorCode(), sysFailResult.errorMessage(), null, null, null, "Workflow exception: " + e.getMessage()));
            } catch (Exception ex) {
                log.warn("Failed to persist workflow failure result: {}", ex.getMessage());
            }
            return sysFailResult;
        }
    }

    private void triggerSafetyReversal(UUID transactionId, UUID agentId, BigDecimal amount) {
        log.info("Triggering safety reversal for transaction: {}", transactionId);
        currentStatus = WorkflowStatus.COMPENSATING;
        try {
            var result = sendReversalToSwitchActivity.sendReversalWithRetry(new SwitchReversalInput(transactionId));
            if (result.success()) {
                log.info("Safety reversal succeeded for transaction: {}", transactionId);
                // Proceed with float release
                releaseFloatActivity.releaseFloat(new FloatReleaseInput(agentId, amount, transactionId));
                // Update transaction status to FAILED
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                    Workflow.getInfo().getWorkflowId(), "FAILED", null, null, null, null, null, "Safety reversal completed"));
                currentStatus = WorkflowStatus.FAILED;
            } else if (result.flaggedForManualIntervention()) {
                log.warn("Safety reversal flagged for manual intervention for transaction: {}", transactionId);
                // Flag for manual intervention - keep in COMPENSATING status
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                    Workflow.getInfo().getWorkflowId(), "REVERSAL_STUCK", "REVERSAL_TIMEOUT",
                    "Safety reversal failed after 24 hours", null, null, null, "Requires manual intervention"));
            } else {
                log.error("Safety reversal failed after {} retries for transaction: {}",
                    result.retryCount(), transactionId);
                // Keep in COMPENSATING status for retry
            }
        } catch (Exception e) {
            log.error("Safety reversal threw exception for transaction {}: {}", transactionId, e.getMessage());
            // Keep in COMPENSATING status
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
