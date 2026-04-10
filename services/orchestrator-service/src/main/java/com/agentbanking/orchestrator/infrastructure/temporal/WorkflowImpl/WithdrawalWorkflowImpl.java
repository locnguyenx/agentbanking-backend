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
import io.temporal.activity.ActivityOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@WorkflowImpl(workers = "agent-banking-tasks")
public class WithdrawalWorkflowImpl implements WithdrawalWorkflow {

    private static final Logger log = Workflow.getLogger(WithdrawalWorkflowImpl.class);

    private final CheckVelocityActivity checkVelocityActivity;
    private final EvaluateStpActivity evaluateStpActivity;
    private final CalculateFeesActivity calculateFeesActivity;
    private final BlockFloatActivity blockFloatActivity;
    private final CommitFloatActivity commitFloatActivity;
    private final ReleaseFloatActivity releaseFloatActivity;
    private final AuthorizeAtSwitchActivity authorizeAtSwitchActivity;
    private final SendReversalToSwitchActivity sendReversalToSwitchActivity;
    private final PublishKafkaEventActivity publishKafkaEventActivity;
    private final SaveResolutionCaseActivity saveResolutionCaseActivity;
    private final PersistWorkflowResultActivity persistWorkflowResultActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    public WithdrawalWorkflowImpl() {
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

        ActivityOptions switchAuthOptions = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(25))
                .setRetryOptions(io.temporal.common.RetryOptions.newBuilder()
                        .setMaximumAttempts(1)
                        .build())
                .build();

        ActivityOptions reversalOptions = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(10))
                .setScheduleToCloseTimeout(Duration.ofSeconds(60))
                .setRetryOptions(io.temporal.common.RetryOptions.newBuilder()
                        .setMaximumAttempts(Integer.MAX_VALUE)
                        .setInitialInterval(Duration.ofSeconds(60))
                        .setMaximumInterval(Duration.ofSeconds(60))
                        .build())
                .build();

        this.checkVelocityActivity = Workflow.newActivityStub(CheckVelocityActivity.class, defaultOptions);
        this.evaluateStpActivity = Workflow.newActivityStub(EvaluateStpActivity.class, defaultOptions);
        this.calculateFeesActivity = Workflow.newActivityStub(CalculateFeesActivity.class, defaultOptions);
        this.blockFloatActivity = Workflow.newActivityStub(BlockFloatActivity.class, noRetryOptions);
        this.commitFloatActivity = Workflow.newActivityStub(CommitFloatActivity.class, defaultOptions);
        this.releaseFloatActivity = Workflow.newActivityStub(ReleaseFloatActivity.class, defaultOptions);
        this.authorizeAtSwitchActivity = Workflow.newActivityStub(AuthorizeAtSwitchActivity.class, switchAuthOptions);
        this.sendReversalToSwitchActivity = Workflow.newActivityStub(SendReversalToSwitchActivity.class, reversalOptions);
        this.saveResolutionCaseActivity = Workflow.newActivityStub(SaveResolutionCaseActivity.class, defaultOptions);
        this.publishKafkaEventActivity = Workflow.newActivityStub(PublishKafkaEventActivity.class, defaultOptions);
        this.persistWorkflowResultActivity = Workflow.newActivityStub(PersistWorkflowResultActivity.class, defaultOptions);
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
                        input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null));
                return failResult;
            }

            StpDecision stpDecision = evaluateStpActivity.evaluateStp(
                    "CASH_WITHDRAWAL",
                    input.agentId().toString(),
                    input.amount().toString(),
                    input.customerMykad());
            if (!stpDecision.approved()) {
                // Auto-create resolution case for non-STP transactions
                if (stpDecision.category().equals("NON_STP") || 
                    stpDecision.category().equals("CONDITIONAL_STP")) {
                    saveResolutionCaseActivity.saveResolutionCase(
                        new SaveResolutionCaseActivity.Input(
                            input.idempotencyKey(),
                            null, // transactionId not available yet
                            stpDecision.category(),
                            stpDecision.reason()
                        )
                    );
                }
                currentStatus = WorkflowStatus.PENDING_REVIEW;
                WorkflowResult stpResult = WorkflowResult.failed("ERR_STP_REVIEW", stpDecision.reason(), "REVIEW");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        input.idempotencyKey(), "PENDING_REVIEW", stpResult.errorCode(), stpResult.errorMessage(), null, null, null));
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
                            input.idempotencyKey(),
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
                        input.idempotencyKey(), "FAILED", blockFailResult.errorCode(), blockFailResult.errorMessage(), null, null, null));
                return blockFailResult;
            }
            UUID transactionId = blockResult.transactionId();

            SwitchAuthorizationResult authResult = authorizeAtSwitchActivity.authorize(
                    new SwitchAuthorizationInput(input.pan(), input.pinBlock(), input.amount(), transactionId));

            if (!authResult.approved()) {
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), totalAmount, transactionId));

                if ("TIMEOUT".equals(authResult.responseCode())) {
                    triggerSafetyReversal(transactionId);
                }

                String errorCode = authResult.errorCode() != null ? authResult.errorCode() : "ERR_SWITCH_DECLINED";
                String actionCode = "TIMEOUT".equals(authResult.responseCode()) ? "RETRY" : "DECLINE";
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult authFailResult = WorkflowResult.failed(errorCode, "Switch authorization failed", actionCode);
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        input.idempotencyKey(), "FAILED", authFailResult.errorCode(), authFailResult.errorMessage(), null, null, null));
                return authFailResult;
            }

            FloatCommitResult commitResult = commitFloatActivity.commitFloat(
                    new FloatCommitInput(input.agentId(), input.amount(), transactionId));
            if (!commitResult.success()) {
                triggerSafetyReversal(transactionId);
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult commitFailResult = WorkflowResult.failed(commitResult.errorCode(), "Float commit failed", "RETRY");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        input.idempotencyKey(), "FAILED", commitFailResult.errorCode(), commitFailResult.errorMessage(), null, null, null));
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
                    input.idempotencyKey(), "COMPLETED", null, null,
                    authResult.referenceCode(), fees.customerFee(), authResult.referenceCode()));
            return completedResult;

        } catch (Exception e) {
            log.error("Workflow failed with exception: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            WorkflowResult sysFailResult = WorkflowResult.failed("ERR_SYS_WORKFLOW_FAILED", e.getMessage(), "REVIEW");
            try {
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        input.idempotencyKey(), "FAILED", sysFailResult.errorCode(), sysFailResult.errorMessage(), null, null, null));
            } catch (Exception ex) {
                log.warn("Failed to persist workflow failure result: {}", ex.getMessage());
            }
            return sysFailResult;
        }
    }

    private void triggerSafetyReversal(UUID transactionId) {
        log.info("Triggering safety reversal for transaction: {}", transactionId);
        currentStatus = WorkflowStatus.COMPENSATING;
        try {
            sendReversalToSwitchActivity.sendReversal(new SwitchReversalInput(transactionId));
        } catch (Exception e) {
            log.error("Safety reversal failed: {}", e.getMessage());
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
