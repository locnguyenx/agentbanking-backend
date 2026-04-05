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
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

public class DuitNowTransferWorkflowImpl implements DuitNowTransferWorkflow {

    private static final Logger log = Workflow.getLogger(DuitNowTransferWorkflowImpl.class);

    private final CheckVelocityActivity checkVelocityActivity;
    private final CalculateFeesActivity calculateFeesActivity;
    private final BlockFloatActivity blockFloatActivity;
    private final CommitFloatActivity commitFloatActivity;
    private final ReleaseFloatActivity releaseFloatActivity;
    private final ProxyEnquiryActivity proxyEnquiryActivity;
    private final SendDuitNowTransferActivity sendDuitNowTransferActivity;
    private final SendReversalToSwitchActivity sendReversalToSwitchActivity;
    private final PublishKafkaEventActivity publishKafkaEventActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    public DuitNowTransferWorkflowImpl() {
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

        ActivityOptions duitNowOptions = ActivityOptions.newBuilder()
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
        this.calculateFeesActivity = Workflow.newActivityStub(CalculateFeesActivity.class, defaultOptions);
        this.blockFloatActivity = Workflow.newActivityStub(BlockFloatActivity.class, noRetryOptions);
        this.commitFloatActivity = Workflow.newActivityStub(CommitFloatActivity.class, defaultOptions);
        this.releaseFloatActivity = Workflow.newActivityStub(ReleaseFloatActivity.class, defaultOptions);
        this.proxyEnquiryActivity = Workflow.newActivityStub(ProxyEnquiryActivity.class, defaultOptions);
        this.sendDuitNowTransferActivity = Workflow.newActivityStub(SendDuitNowTransferActivity.class, duitNowOptions);
        this.sendReversalToSwitchActivity = Workflow.newActivityStub(SendReversalToSwitchActivity.class, reversalOptions);
        this.publishKafkaEventActivity = Workflow.newActivityStub(PublishKafkaEventActivity.class, defaultOptions);
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
                return WorkflowResult.failed(velocityResult.errorCode(), "Velocity check failed", "DECLINE");
            }

            // Step 2: Calculate fees
            FeeCalculationResult fees = calculateFeesActivity.calculateFees(
                    new FeeCalculationInput("DUITNOW_TRANSFER", input.agentTier(), input.amount()));

            BigDecimal totalAmount = input.amount().add(fees.customerFee());

            // Step 3: Block float
            FloatBlockResult blockResult = blockFloatActivity.blockFloat(
                    new FloatBlockInput(input.agentId(), totalAmount, input.idempotencyKey()));
            if (!blockResult.success()) {
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed(blockResult.errorCode(), "Float block failed", "DECLINE");
            }
            UUID transactionId = blockResult.transactionId();

            // Step 4: Proxy enquiry
            ProxyEnquiryResult proxyResult = proxyEnquiryActivity.proxyEnquiry(
                    new ProxyEnquiryInput(input.proxyType(), input.proxyValue()));
            if (!proxyResult.valid()) {
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed(proxyResult.errorCode(), "Proxy not found", "DECLINE");
            }

            // Step 5: Send DuitNow transfer
            DuitNowTransferResult transferResult = sendDuitNowTransferActivity.sendDuitNowTransfer(
                    new DuitNowTransferInput(proxyResult.bankCode(), proxyResult.recipientName(),
                            input.amount(), transactionId));
            if (!transferResult.success()) {
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), totalAmount, transactionId));

                if ("TIMEOUT".equals(transferResult.errorCode())) {
                    triggerSafetyReversal(transactionId);
                }

                String errorCode = transferResult.errorCode() != null ? transferResult.errorCode() : "ERR_DUITNOW_FAILED";
                String actionCode = "TIMEOUT".equals(transferResult.errorCode()) ? "RETRY" : "DECLINE";
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed(errorCode, "DuitNow transfer failed", actionCode);
            }

            // Step 6: Commit float
            FloatCommitResult commitResult = commitFloatActivity.commitFloat(
                    new FloatCommitInput(input.agentId(), input.amount(), transactionId));
            if (!commitResult.success()) {
                triggerSafetyReversal(transactionId);
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed(commitResult.errorCode(), "Float commit failed", "RETRY");
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
            return WorkflowResult.completed(transactionId, transferResult.paynetReference(),
                    input.amount(), fees.customerFee());

        } catch (Exception e) {
            log.error("Workflow failed with exception: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            return WorkflowResult.failed("ERR_SYS_WORKFLOW_FAILED", e.getMessage(), "REVIEW");
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