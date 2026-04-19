package com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.workflow.DepositWorkflow;
import com.agentbanking.orchestrator.domain.model.ForceResolveSignal;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.out.CbsServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.EventPublisherPort.TransactionCompletedEvent;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.*;
import com.agentbanking.orchestrator.application.activity.ValidateAccountActivity;
import com.agentbanking.orchestrator.application.activity.CreditAgentFloatActivity;

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
public class DepositWorkflowImpl implements DepositWorkflow {

    private static final Logger log = Workflow.getLogger(DepositWorkflowImpl.class);

    private GetDailyMetricsActivity getDailyMetricsActivity;
    private CheckVelocityActivity checkVelocityActivity;
    private EvaluateStpActivity evaluateStpActivity;
    private CalculateFeesActivity calculateFeesActivity;
    private ValidateAccountActivity validateAccountActivity;
    private CreditAgentFloatActivity creditAgentFloatActivity;
    private ReleaseFloatActivity releaseFloatActivity;
    private PostToCBSActivity postToCBSActivity;
    private PublishKafkaEventActivity publishKafkaEventActivity;
    private SaveResolutionCaseActivity saveResolutionCaseActivity;
    private PersistWorkflowResultActivity persistWorkflowResultActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    public DepositWorkflowImpl(
            GetDailyMetricsActivity getDailyMetricsActivity,
            CheckVelocityActivity checkVelocityActivity,
            EvaluateStpActivity evaluateStpActivity,
            CalculateFeesActivity calculateFeesActivity,
            ValidateAccountActivity validateAccountActivity,
            CreditAgentFloatActivity creditAgentFloatActivity,
            ReleaseFloatActivity releaseFloatActivity,
            PostToCBSActivity postToCBSActivity,
            PublishKafkaEventActivity publishKafkaEventActivity,
            SaveResolutionCaseActivity saveResolutionCaseActivity,
            PersistWorkflowResultActivity persistWorkflowResultActivity) {
        this.getDailyMetricsActivity = getDailyMetricsActivity;
        this.checkVelocityActivity = checkVelocityActivity;
        this.evaluateStpActivity = evaluateStpActivity;
        this.calculateFeesActivity = calculateFeesActivity;
        this.validateAccountActivity = validateAccountActivity;
        this.creditAgentFloatActivity = creditAgentFloatActivity;
        this.releaseFloatActivity = releaseFloatActivity;
        this.postToCBSActivity = postToCBSActivity;
        this.publishKafkaEventActivity = publishKafkaEventActivity;
        this.saveResolutionCaseActivity = saveResolutionCaseActivity;
        this.persistWorkflowResultActivity = persistWorkflowResultActivity;
    }

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    public DepositWorkflowImpl() {
        this.getDailyMetricsActivity = Workflow.newActivityStub(GetDailyMetricsActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.checkVelocityActivity = Workflow.newActivityStub(CheckVelocityActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.evaluateStpActivity = Workflow.newActivityStub(EvaluateStpActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.calculateFeesActivity = Workflow.newActivityStub(CalculateFeesActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.validateAccountActivity = Workflow.newActivityStub(ValidateAccountActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.creditAgentFloatActivity = Workflow.newActivityStub(CreditAgentFloatActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.releaseFloatActivity = Workflow.newActivityStub(ReleaseFloatActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.postToCBSActivity = Workflow.newActivityStub(PostToCBSActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(60))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.publishKafkaEventActivity = Workflow.newActivityStub(PublishKafkaEventActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.saveResolutionCaseActivity = Workflow.newActivityStub(SaveResolutionCaseActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.persistWorkflowResultActivity = Workflow.newActivityStub(PersistWorkflowResultActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
    }

    @Override
    public WorkflowResult execute(DepositInput input) {
        log.info("Workflow started: Deposit, agentId={}, amount={}", input.agentId(), input.amount());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            // Task: Fetch metrics
            DailyMetricsResult metrics = getDailyMetricsActivity.getDailyMetrics(input.agentId());

            // Step 1: Check velocity
            VelocityCheckResult velocityResult = checkVelocityActivity.checkVelocity(
                    new VelocityCheckInput(
                        input.agentId(), 
                        "CASH_DEPOSIT",
                        input.amount(), 
                        input.customerMykad(),
                        metrics.transactionCountToday(),
                        metrics.amountToday()
                    ));
            
            if (!velocityResult.passed()) {
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed(velocityResult.errorCode(), "Velocity check failed", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Velocity check failed"));
                return failResult;
            }

            // Step 2: Evaluate STP
            StpDecision stpDecision;
            try {
                stpDecision = evaluateStpActivity.evaluateStp(
                        new EvaluateStpActivity.Input(
                                "CASH_DEPOSIT",
                                input.agentId().toString(),
                                input.amount().toString(),
                                input.customerMykad(),
                                input.agentTier(),
                                metrics.transactionCountToday(),
                                metrics.amountToday().toString(),
                                metrics.todayTotalAmount().toString()
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
                WorkflowResult reviewResult = WorkflowResult.failed("ERR_STP_REVIEW", stpDecision.reason(), "REVIEW");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "PENDING_REVIEW", reviewResult.errorCode(), reviewResult.errorMessage(), null, null, null, stpDecision.reason()));
                return reviewResult;
            }

            // Step 3: Calculate fees
            FeeCalculationResult fees = calculateFeesActivity.calculateFees(
                    new FeeCalculationInput("CASH_DEPOSIT", input.agentTier(), input.amount()));

            // Step 4: Validate account
            AccountValidationResult accountResult = validateAccountActivity.validateAccount(
                    new AccountValidationInput(input.destinationAccount()));
            if (!accountResult.valid()) {
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed(accountResult.errorCode(), "Invalid account", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Invalid account"));
                return failResult;
            }

            // Step 4: Credit agent float
            FloatCreditResult creditResult;
            try {
                creditResult = creditAgentFloatActivity.creditAgentFloat(
                        new FloatCreditInput(
                                input.agentId(),
                                input.amount(),
                                fees.customerFee(),
                                fees.agentCommission(),
                                fees.bankShare(),
                                input.idempotencyKey(),
                                input.destinationAccount(),
                                input.agentTier(),
                                input.targetBin(),
                                input.idempotencyKey(), // referenceNumber fallback
                                input.geofenceLat(),
                                input.geofenceLng(),
                                "CASH_DEPOSIT"
                        )
                );
            } catch (ActivityFailure e) {
                log.error("Float credit failed after retries: {}", e.getMessage());
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult creditFailResult = WorkflowResult.failed("ERR_FLOAT_CREDIT_UNAVAILABLE", "Float credit service unavailable after retries", "RETRY");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", creditFailResult.errorCode(), creditFailResult.errorMessage(), null, null, null, "Float credit unavailable: " + e.getMessage()));
                return creditFailResult;
            }
            if (!creditResult.success()) {
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed(creditResult.errorCode(), "Float credit failed", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Float credit failed"));
                return failResult;
            }

            // Step 5: Post to CBS
            CbsPostResult cbsResult;
            try {
                cbsResult = postToCBSActivity.postToCbs(
                        new CbsPostInput(input.destinationAccount(), input.amount()));
            } catch (ActivityFailure e) {
                log.error("CBS posting failed after retries: {}", e.getMessage());
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult cbsFailResult = WorkflowResult.failed("ERR_CBS_UNAVAILABLE", "CBS service unavailable after retries", "RETRY");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", cbsFailResult.errorCode(), cbsFailResult.errorMessage(), null, null, null, "CBS unavailable: " + e.getMessage()));
                return cbsFailResult;
            }
            if (!cbsResult.success()) {
                // Compensation: reverse the credit
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), input.amount(), null));
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed(cbsResult.errorCode(), "CBS posting failed", "RETRY");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "CBS posting failed"));
                return failResult;
            }

            // Step 6: Publish event (non-critical)
            try {
                publishKafkaEventActivity.publishCompleted(new TransactionCompletedEvent(
                        UUID.randomUUID(), input.agentId(), input.amount(), fees.customerFee(),
                        fees.agentCommission(), fees.bankShare(), "CASH_DEPOSIT",
                        null, cbsResult.reference(), cbsResult.reference()));
            } catch (Exception e) {
                log.warn("Failed to publish Kafka event: {}", e.getMessage());
            }

            currentStatus = WorkflowStatus.COMPLETED;
            UUID txId = UUID.randomUUID();
            WorkflowResult completedResult = WorkflowResult.completed(txId, cbsResult.reference(),
                    input.amount(), fees.customerFee());
            persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                    Workflow.getInfo().getWorkflowId(), "COMPLETED", null, null,
                    cbsResult.reference(), fees.customerFee(), cbsResult.reference(), null));
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
    public void forceResolve(ForceResolveSignal signal) {
        log.info("Force resolve signal received: action={}", signal.action());
    }

    @Override
    public WorkflowStatus getStatus() {
        return currentStatus;
    }
}