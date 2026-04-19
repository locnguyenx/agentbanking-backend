package com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.workflow.BillPaymentWorkflow;
import com.agentbanking.orchestrator.domain.model.ForceResolveSignal;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.EventPublisherPort.TransactionCompletedEvent;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.*;

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
public class BillPaymentWorkflowImpl implements BillPaymentWorkflow {

    private static final Logger log = Workflow.getLogger(BillPaymentWorkflowImpl.class);

    private GetDailyMetricsActivity getDailyMetricsActivity;
    private CheckVelocityActivity checkVelocityActivity;
    private EvaluateStpActivity evaluateStpActivity;
    private CalculateFeesActivity calculateFeesActivity;
    private BlockFloatActivity blockFloatActivity;
    private CommitFloatActivity commitFloatActivity;
    private ReleaseFloatActivity releaseFloatActivity;
    private ValidateBillActivity validateBillActivity;
    private PayBillerActivity payBillerActivity;
    private NotifyBillerActivity notifyBillerActivity;
    private PublishKafkaEventActivity publishKafkaEventActivity;
    private SaveResolutionCaseActivity saveResolutionCaseActivity;
    private PersistWorkflowResultActivity persistWorkflowResultActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    public BillPaymentWorkflowImpl(
            GetDailyMetricsActivity getDailyMetricsActivity,
            CheckVelocityActivity checkVelocityActivity,
            EvaluateStpActivity evaluateStpActivity,
            CalculateFeesActivity calculateFeesActivity,
            BlockFloatActivity blockFloatActivity,
            CommitFloatActivity commitFloatActivity,
            ReleaseFloatActivity releaseFloatActivity,
            ValidateBillActivity validateBillActivity,
            PayBillerActivity payBillerActivity,
            NotifyBillerActivity notifyBillerActivity,
            PublishKafkaEventActivity publishKafkaEventActivity,
            SaveResolutionCaseActivity saveResolutionCaseActivity,
            PersistWorkflowResultActivity persistWorkflowResultActivity) {
        this.getDailyMetricsActivity = getDailyMetricsActivity;
        this.checkVelocityActivity = checkVelocityActivity;
        this.evaluateStpActivity = evaluateStpActivity;
        this.calculateFeesActivity = calculateFeesActivity;
        this.blockFloatActivity = blockFloatActivity;
        this.commitFloatActivity = commitFloatActivity;
        this.releaseFloatActivity = releaseFloatActivity;
        this.validateBillActivity = validateBillActivity;
        this.payBillerActivity = payBillerActivity;
        this.notifyBillerActivity = notifyBillerActivity;
        this.publishKafkaEventActivity = publishKafkaEventActivity;
        this.saveResolutionCaseActivity = saveResolutionCaseActivity;
        this.persistWorkflowResultActivity = persistWorkflowResultActivity;
    }

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    public BillPaymentWorkflowImpl() {
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
        this.validateBillActivity = Workflow.newActivityStub(ValidateBillActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.payBillerActivity = Workflow.newActivityStub(PayBillerActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(60))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.notifyBillerActivity = Workflow.newActivityStub(NotifyBillerActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
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
    public WorkflowResult execute(BillPaymentInput input) {
        log.info("Workflow started: BillPayment, agentId={}, biller={}", input.agentId(), input.billerCode());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            // Task: Fetch metrics
            DailyMetricsResult metrics = getDailyMetricsActivity.getDailyMetrics(input.agentId());

            // Step 1: Check velocity
            VelocityCheckResult velocityResult = checkVelocityActivity.checkVelocity(
                    new VelocityCheckInput(
                        input.agentId(), 
                        "BILL_PAYMENT",
                        input.amount(), 
                        input.customerMykad(),
                        metrics.transactionCountToday(),
                        metrics.amountToday()
                    ));
            
            if (!velocityResult.passed()) {
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed(velocityResult.errorCode(), "Velocity check failed", "DECLINE");
            }

            // Step 2: Evaluate STP
            var stpDecision = evaluateStpActivity.evaluateStp(
                    new EvaluateStpActivity.Input(
                            "BILL_PAYMENT",
                            input.agentId().toString(),
                            input.amount().toString(),
                            input.customerMykad(),
                            input.agentTier(),
                            metrics.transactionCountToday(),
                            metrics.amountToday().toString(),
                            metrics.todayTotalAmount().toString()
                    )
            );
            if (!stpDecision.approved()) {
                currentStatus = WorkflowStatus.PENDING_REVIEW;
                return WorkflowResult.failed("ERR_STP_REVIEW", stpDecision.reason(), "REVIEW");
            }

            // Step 3: Calculate fees
            FeeCalculationResult fees = calculateFeesActivity.calculateFees(
                    new FeeCalculationInput("BILL_PAYMENT", input.agentTier(), input.amount()));

            BigDecimal totalAmount = input.amount().add(fees.customerFee());

            // Step 4: Block float
            FloatBlockResult blockResult;
            try {
                blockResult = blockFloatActivity.blockFloat(
                        new FloatBlockInput(
                                input.agentId(),
                                input.amount(),
                                fees.customerFee(),
                                fees.agentCommission(),
                                fees.bankShare(),
                                input.idempotencyKey(),
                                null,
                                input.geofenceLat(),
                                input.geofenceLng(),
                                input.agentTier(),
                                input.targetBin(),
                                "JOMPAY"
                        )
                );
            } catch (ActivityFailure e) {
                log.error("Activity failure in blockFloat: {}", e.getMessage());
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILED", "Float block activity failed: " + e.getMessage(), "RETRY");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, e.getMessage()));
                return failResult;
            }
            if (!blockResult.success()) {
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed(blockResult.errorCode(), "Float block failed", "DECLINE");
            }
            UUID transactionId = blockResult.transactionId();

            // Step 4: Validate bill
            BillValidationResult billResult;
            try {
                billResult = validateBillActivity.validateBill(
                        new BillValidationInput(input.billerCode(), input.ref1()));
            } catch (ActivityFailure e) {
                log.error("Activity failure in validateBill: {}", e.getMessage());
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILED", "Bill validation activity failed: " + e.getMessage(), "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, e.getMessage()));
                return failResult;
            }
            if (!billResult.valid()) {
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed(billResult.errorCode(), "Bill validation failed", "DECLINE");
            }

            // Step 5: Pay biller
            BillPaymentResult paymentResult;
            try {
                paymentResult = payBillerActivity.payBill(
                        new com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillPaymentInput(input.billerCode(), input.ref1(), input.ref2(),
                                input.amount(), input.idempotencyKey()));
            } catch (ActivityFailure e) {
                log.error("Activity failure in payBill: {}", e.getMessage());
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILED", "Bill payment activity failed: " + e.getMessage(), "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, e.getMessage()));
                return failResult;
            }
            if (!paymentResult.success()) {
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed(paymentResult.errorCode(), "Bill payment failed", "DECLINE");
            }

            // Step 6: Commit float
            FloatCommitResult commitResult = commitFloatActivity.commitFloat(
                    new FloatCommitInput(input.agentId(), input.amount(), transactionId));
            if (!commitResult.success()) {
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed(commitResult.errorCode(), "Float commit failed", "RETRY");
            }

            // Step 7: Notify biller (non-critical)
            try {
                notifyBillerActivity.notifyBiller(
                        new BillNotificationInput(transactionId.toString(), input.amount()));
            } catch (Exception e) {
                log.warn("Failed to notify biller: {}", e.getMessage());
            }

            // Step 8: Publish event (non-critical)
            try {
                publishKafkaEventActivity.publishCompleted(new TransactionCompletedEvent(
                        transactionId, input.agentId(), input.amount(), fees.customerFee(),
                        fees.agentCommission(), fees.bankShare(), "BILL_PAYMENT",
                        null, paymentResult.billerReference(), paymentResult.billerReference()));
            } catch (Exception e) {
                log.warn("Failed to publish Kafka event: {}", e.getMessage());
            }

            currentStatus = WorkflowStatus.COMPLETED;
            WorkflowResult completedResult = WorkflowResult.completed(transactionId, paymentResult.billerReference(),
                    input.amount(), fees.customerFee());
            persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                    Workflow.getInfo().getWorkflowId(), "COMPLETED", null, null,
                    paymentResult.billerReference(), fees.customerFee(), paymentResult.billerReference(), null));
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

    @Override
    public void forceResolve(ForceResolveSignal signal) {
        log.info("Force resolve signal received: action={}", signal.action());
    }

    @Override
    public WorkflowStatus getStatus() {
        return currentStatus;
    }
}