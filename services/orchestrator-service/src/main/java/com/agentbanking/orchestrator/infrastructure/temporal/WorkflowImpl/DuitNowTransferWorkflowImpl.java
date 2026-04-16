package com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.workflow.DuitNowTransferWorkflow;
import com.agentbanking.orchestrator.domain.model.ForceResolveSignal;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.out.EventPublisherPort.TransactionCompletedEvent;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.*;

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
public class DuitNowTransferWorkflowImpl implements DuitNowTransferWorkflow {

    private static final Logger log = Workflow.getLogger(DuitNowTransferWorkflowImpl.class);

    private CheckVelocityActivity checkVelocityActivity;
    private EvaluateStpActivity evaluateStpActivity;
    private CalculateFeesActivity calculateFeesActivity;
    private BlockFloatActivity blockFloatActivity;
    private CommitFloatActivity commitFloatActivity;
    private ReleaseFloatActivity releaseFloatActivity;
    private ProxyEnquiryActivity proxyEnquiryActivity;
    private SendDuitNowTransferActivity sendDuitNowTransferActivity;
    private SendReversalToSwitchActivity sendReversalToSwitchActivity;
    private PublishKafkaEventActivity publishKafkaEventActivity;
    private SaveResolutionCaseActivity saveResolutionCaseActivity;
    private PersistWorkflowResultActivity persistWorkflowResultActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    public DuitNowTransferWorkflowImpl(
            CheckVelocityActivity checkVelocityActivity,
            EvaluateStpActivity evaluateStpActivity,
            CalculateFeesActivity calculateFeesActivity,
            BlockFloatActivity blockFloatActivity,
            CommitFloatActivity commitFloatActivity,
            ReleaseFloatActivity releaseFloatActivity,
            ProxyEnquiryActivity proxyEnquiryActivity,
            SendDuitNowTransferActivity sendDuitNowTransferActivity,
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
        this.proxyEnquiryActivity = proxyEnquiryActivity;
        this.sendDuitNowTransferActivity = sendDuitNowTransferActivity;
        this.sendReversalToSwitchActivity = sendReversalToSwitchActivity;
        this.publishKafkaEventActivity = publishKafkaEventActivity;
        this.saveResolutionCaseActivity = saveResolutionCaseActivity;
        this.persistWorkflowResultActivity = persistWorkflowResultActivity;
    }

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    public DuitNowTransferWorkflowImpl() {
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
        this.proxyEnquiryActivity = Workflow.newActivityStub(ProxyEnquiryActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(60))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.sendDuitNowTransferActivity = Workflow.newActivityStub(SendDuitNowTransferActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(60))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.sendReversalToSwitchActivity = Workflow.newActivityStub(SendReversalToSwitchActivity.class, ActivityOptions.newBuilder()
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
    public WorkflowResult execute(DuitNowTransferInput input) {
        log.info("Workflow started: DuitNowTransfer, agentId={}, amount={}", input.agentId(), input.amount());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            // Step 1: Check velocity
            VelocityCheckResult velocityResult = checkVelocityActivity.checkVelocity(
                    new VelocityCheckInput(input.agentId(), input.amount(), input.customerMykad()));
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
                                "DUITNOW_TRANSFER",
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
                log.error("Evaluate STP activity failed: {}", e.getMessage());
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_STP_ACTIVITY", e.getMessage(), "RETRY");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Evaluate STP activity failed: " + e.getMessage()));
                return failResult;
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
                    new FeeCalculationInput("DUITNOW_TRANSFER", input.agentTier(), input.amount()));

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
                                Workflow.getInfo().getWorkflowId(),
                                null,
                                input.geofenceLat(),
                                input.geofenceLng(),
                                input.agentTier(),
                                input.targetBin()
                        )
                );
            } catch (ActivityFailure e) {
                log.error("Block float activity failed: {}", e.getMessage());
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_FLOAT_BLOCK_ACTIVITY", e.getMessage(), "RETRY");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Block float activity failed: " + e.getMessage()));
                return failResult;
            }
            if (!blockResult.success()) {
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed(blockResult.errorCode(), "Float block failed", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Float block failed"));
                return failResult;
            }
            UUID transactionId = blockResult.transactionId();

            // Step 4: Proxy enquiry
            ProxyEnquiryResult proxyResult = proxyEnquiryActivity.proxyEnquiry(
                    new ProxyEnquiryInput(input.proxyType(), input.proxyValue()));
            if (!proxyResult.valid()) {
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed(proxyResult.errorCode(), "Proxy not found", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Proxy not found"));
                return failResult;
            }

            // Step 5: Send DuitNow transfer (authorize)
            DuitNowTransferResult transferResult;
            try {
                transferResult = sendDuitNowTransferActivity.sendDuitNowTransfer(
                        new com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.DuitNowTransferInput(proxyResult.bankCode(), proxyResult.recipientName(),
                                input.amount(), transactionId));
            } catch (ActivityFailure e) {
                log.error("Send DuitNow transfer activity failed: {}", e.getMessage());
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_AUTHORIZE_ACTIVITY", e.getMessage(), "RETRY");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Authorize activity failed: " + e.getMessage()));
                return failResult;
            }
            if (!transferResult.success()) {
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), totalAmount, transactionId));

                if ("TIMEOUT".equals(transferResult.errorCode())) {
                    triggerSafetyReversal(transactionId);
                }

                String errorCode = transferResult.errorCode() != null ? transferResult.errorCode() : "ERR_DUITNOW_FAILED";
                String actionCode = "TIMEOUT".equals(transferResult.errorCode()) ? "RETRY" : "DECLINE";
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed(errorCode, "DuitNow transfer failed", actionCode);
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "DuitNow transfer failed"));
                return failResult;
            }

            // Step 6: Commit float
            FloatCommitResult commitResult = commitFloatActivity.commitFloat(
                    new FloatCommitInput(input.agentId(), input.amount(), transactionId));
            if (!commitResult.success()) {
                triggerSafetyReversal(transactionId);
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed(commitResult.errorCode(), "Float commit failed", "RETRY");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Float commit failed"));
                return failResult;
            }

            // Step 7: Publish event (non-critical)
            try {
                publishKafkaEventActivity.publishCompleted(new TransactionCompletedEvent(
                        transactionId, input.agentId(), input.amount(), fees.customerFee(),
                        fees.agentCommission(), fees.bankShare(), "DUITNOW_TRANSFER",
                        null, transferResult.paynetReference(), transferResult.paynetReference()));
            } catch (Exception e) {
                log.warn("Failed to publish Kafka event: {}", e.getMessage());
            }

            currentStatus = WorkflowStatus.COMPLETED;
            WorkflowResult completedResult = WorkflowResult.completed(transactionId, transferResult.paynetReference(),
                    input.amount(), fees.customerFee());
            persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                    Workflow.getInfo().getWorkflowId(), "COMPLETED", null, null,
                    transferResult.paynetReference(), fees.customerFee(), transferResult.paynetReference(), null));
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
