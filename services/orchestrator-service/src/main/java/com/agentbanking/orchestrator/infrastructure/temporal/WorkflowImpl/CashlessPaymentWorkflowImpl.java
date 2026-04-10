package com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.activity.ValidateFloatCapacityActivity.FloatCapacityResult;
import com.agentbanking.orchestrator.application.workflow.CashlessPaymentWorkflow;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatBlockInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatBlockResult;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCommitInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatReleaseInput;
import io.temporal.activity.ActivityOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@WorkflowImpl(workers = "agent-banking-tasks")
public class CashlessPaymentWorkflowImpl implements CashlessPaymentWorkflow {

    private static final Logger log = Workflow.getLogger(CashlessPaymentWorkflowImpl.class);

    private final ValidateFloatCapacityActivity validateFloatCapacityActivity;
    private final BlockFloatActivity blockFloatActivity;
    private final CommitFloatActivity commitFloatActivity;
    private final ReleaseFloatActivity releaseFloatActivity;
    private final GenerateDynamicQRActivity generateDynamicQRActivity;
    private final WaitForQRPaymentActivity waitForQRPaymentActivity;
    private final SendRequestToPayActivity sendRequestToPayActivity;
    private final WaitForRTPApprovalActivity waitForRTPApprovalActivity;
    private final CreditAgentFloatActivity creditAgentFloatActivity;
    private final PersistWorkflowResultActivity persistWorkflowResultActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    public CashlessPaymentWorkflowImpl() {
        ActivityOptions defaultOptions = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .build();
        
        this.validateFloatCapacityActivity = Workflow.newActivityStub(ValidateFloatCapacityActivity.class, defaultOptions);
        this.blockFloatActivity = Workflow.newActivityStub(BlockFloatActivity.class, defaultOptions);
        this.commitFloatActivity = Workflow.newActivityStub(CommitFloatActivity.class, defaultOptions);
        this.releaseFloatActivity = Workflow.newActivityStub(ReleaseFloatActivity.class, defaultOptions);
        this.generateDynamicQRActivity = Workflow.newActivityStub(GenerateDynamicQRActivity.class, defaultOptions);
        this.waitForQRPaymentActivity = Workflow.newActivityStub(WaitForQRPaymentActivity.class, defaultOptions);
        this.sendRequestToPayActivity = Workflow.newActivityStub(SendRequestToPayActivity.class, defaultOptions);
        this.waitForRTPApprovalActivity = Workflow.newActivityStub(WaitForRTPApprovalActivity.class, defaultOptions);
        this.creditAgentFloatActivity = Workflow.newActivityStub(CreditAgentFloatActivity.class, defaultOptions);
        this.persistWorkflowResultActivity = Workflow.newActivityStub(PersistWorkflowResultActivity.class, defaultOptions);
    }

    @Override
    public WorkflowResult execute(CashlessPaymentInput input) {
        log.info("Workflow started: CashlessPayment, agentId={}, method={}", input.agentId(), input.paymentMethod());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            FloatCapacityResult capacityResult = validateFloatCapacityActivity.validate(input.agentId(), input.amount());
            if (!capacityResult.sufficient()) {
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_INSUFFICIENT_FLOAT", "Insufficient float", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null));
                return failResult;
            }

            FloatBlockResult blockResult = blockFloatActivity.blockFloat(
                    new FloatBlockInput(
                            input.agentId(),
                            input.amount(),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            input.idempotencyKey(),
                            null,
                            input.geofenceLat(),
                            input.geofenceLng(),
                            input.agentTier(),
                            input.targetBin()
                    )
            );
            if (!blockResult.success()) {
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed(blockResult.errorCode(), "Float block failed", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null));
                return failResult;
            }
            UUID transactionId = blockResult.transactionId();

            String reference = null;
            if ("QR".equalsIgnoreCase(input.paymentMethod())) {
                var qrResult = generateDynamicQRActivity.generate(input.amount(), input.agentId(), input.idempotencyKey());
                if (qrResult.qrReference() == null) {
                    releaseFloatActivity.releaseFloat(
                            new com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatReleaseInput(
                                    input.agentId(), input.amount(), transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_QR_GENERATION_FAILED", "QR generation failed", "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null));
                    return failResult;
                }
                reference = qrResult.qrReference();
                var paymentStatus = waitForQRPaymentActivity.waitForPayment(reference, 300);
                if (!"PAID".equals(paymentStatus.status())) {
                    releaseFloatActivity.releaseFloat(
                            new com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatReleaseInput(
                                    input.agentId(), input.amount(), transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_QR_PAYMENT_TIMEOUT", "QR payment timeout", "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null));
                    return failResult;
                }
            } else if ("RTP".equalsIgnoreCase(input.paymentMethod())) {
                var rtpResult = sendRequestToPayActivity.send(input.customerProxy(), input.amount(), input.idempotencyKey());
                if (rtpResult.rtpReference() == null) {
                    releaseFloatActivity.releaseFloat(
                            new com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatReleaseInput(
                                    input.agentId(), input.amount(), transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_RTP_SEND_FAILED", "RTP send failed", "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null));
                    return failResult;
                }
                reference = rtpResult.rtpReference();
                var rtpStatus = waitForRTPApprovalActivity.waitForApproval(reference, 300);
                if (!"APPROVED".equals(rtpStatus.status())) {
                    releaseFloatActivity.releaseFloat(
                            new com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatReleaseInput(
                                    input.agentId(), input.amount(), transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_RTP_DECLINED", "RTP declined", "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null));
                    return failResult;
                }
            }

            commitFloatActivity.commitFloat(
                    new com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCommitInput(
                            input.agentId(), input.amount(), transactionId));

            currentStatus = WorkflowStatus.COMPLETED;
            WorkflowResult completedResult = WorkflowResult.completed(transactionId, reference, input.amount(), BigDecimal.ZERO);
            persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                    input.idempotencyKey(), "COMPLETED", null, null, reference, BigDecimal.ZERO, reference));
            return completedResult;

        } catch (Exception e) {
            log.error("CashlessPayment workflow failed: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            WorkflowResult failResult = WorkflowResult.failed("ERR_SYS_WORKFLOW_FAILED", e.getMessage(), "REVIEW");
            try {
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null));
            } catch (Exception ex) {
                log.warn("Failed to persist fail result: {}", ex.getMessage());
            }
            return failResult;
        }
    }

    @Override
    public WorkflowStatus getStatus() {
        return currentStatus;
    }
}
