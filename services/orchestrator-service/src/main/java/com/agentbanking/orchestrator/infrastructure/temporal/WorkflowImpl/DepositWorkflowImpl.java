package com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.workflow.DepositWorkflow;
import com.agentbanking.orchestrator.domain.model.ForceResolveSignal;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.out.CbsServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.EventPublisherPort.TransactionCompletedEvent;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.AccountValidationInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.AccountValidationResult;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatBlockInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatBlockResult;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCommitInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCreditInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCreditResult;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatReleaseInput;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.VelocityCheckInput;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.VelocityCheckResult;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.FeeCalculationInput;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.FeeCalculationResult;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.*;
import com.agentbanking.orchestrator.application.activity.ValidateAccountActivity;
import com.agentbanking.orchestrator.application.activity.CreditAgentFloatActivity;

import io.temporal.activity.ActivityOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@WorkflowImpl(taskQueues = "agent-banking-tasks")
public class DepositWorkflowImpl implements DepositWorkflow {

    private static final Logger log = Workflow.getLogger(DepositWorkflowImpl.class);

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
        this.checkVelocityActivity = Workflow.newActivityStub(CheckVelocityActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(5))
                .build());
        this.evaluateStpActivity = Workflow.newActivityStub(EvaluateStpActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(3))
                .build());
        this.calculateFeesActivity = Workflow.newActivityStub(CalculateFeesActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(1))
                .build());
        this.validateAccountActivity = Workflow.newActivityStub(ValidateAccountActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
        this.creditAgentFloatActivity = Workflow.newActivityStub(CreditAgentFloatActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
        this.releaseFloatActivity = Workflow.newActivityStub(ReleaseFloatActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
        this.postToCBSActivity = Workflow.newActivityStub(PostToCBSActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(5))
                .build());
        this.publishKafkaEventActivity = Workflow.newActivityStub(PublishKafkaEventActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(1))
                .build());
        this.saveResolutionCaseActivity = Workflow.newActivityStub(SaveResolutionCaseActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
        this.persistWorkflowResultActivity = Workflow.newActivityStub(PersistWorkflowResultActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
    }

    @Override
    public WorkflowResult execute(DepositInput input) {
        log.info("Workflow started: Deposit, agentId={}, amount={}", input.agentId(), input.amount());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            // Step 1: Check velocity
            VelocityCheckResult velocityResult = checkVelocityActivity.checkVelocity(
                    new VelocityCheckInput(input.agentId(), input.amount(), input.customerMykad()));
            if (!velocityResult.passed()) {
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed(velocityResult.errorCode(), "Velocity check failed", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Velocity check failed"));
                return failResult;
            }

            // Step 2: Evaluate STP
            var stpDecision = evaluateStpActivity.evaluateStp(
                    new EvaluateStpActivity.Input(
                            "CASH_DEPOSIT",
                            input.agentId().toString(),
                            input.amount().toString(),
                            input.customerMykad(),
                            input.agentTier(),
                            0,
                            "0",
                            "0"
                    )
            );
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
                WorkflowResult reviewResult = WorkflowResult.failed("ERR_STP_REVIEW", stpDecision.reason(), "REVIEW");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        input.idempotencyKey(), "PENDING_REVIEW", reviewResult.errorCode(), reviewResult.errorMessage(), null, null, null, stpDecision.reason()));
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
                        input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Invalid account"));
                return failResult;
            }

            // Step 4: Credit agent float
            FloatCreditResult creditResult = creditAgentFloatActivity.creditAgentFloat(
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
                            input.geofenceLng()
                    )
            );
            if (!creditResult.success()) {
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed(creditResult.errorCode(), "Float credit failed", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Float credit failed"));
                return failResult;
            }

            // Step 5: Post to CBS
            CbsPostResult cbsResult = postToCBSActivity.postToCbs(
                    new CbsPostInput(input.destinationAccount(), input.amount()));
            if (!cbsResult.success()) {
                // Compensation: reverse the credit
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), input.amount(), null));
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed(cbsResult.errorCode(), "CBS posting failed", "RETRY");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "CBS posting failed"));
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
                    input.idempotencyKey(), "COMPLETED", null, null,
                    cbsResult.reference(), fees.customerFee(), cbsResult.reference(), null));
            return completedResult;

        } catch (Exception e) {
            log.error("Workflow failed with exception: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            WorkflowResult failResult = WorkflowResult.failed("ERR_SYS_WORKFLOW_FAILED", e.getMessage(), "REVIEW");
            try {
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Workflow exception: " + e.getMessage()));
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