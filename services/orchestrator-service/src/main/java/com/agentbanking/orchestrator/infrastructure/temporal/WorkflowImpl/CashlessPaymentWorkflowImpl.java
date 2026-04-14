package com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.activity.ValidateFloatCapacityActivity.FloatCapacityResult;
import com.agentbanking.orchestrator.domain.port.out.QRPaymentPort.QRGenerationResult;
import com.agentbanking.orchestrator.domain.port.out.QRPaymentPort.QRPaymentStatus;
import com.agentbanking.orchestrator.application.workflow.CashlessPaymentWorkflow;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatBlockInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatBlockResult;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCommitInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatReleaseInput;
import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@WorkflowImpl(taskQueues = "agent-banking-tasks")
public class CashlessPaymentWorkflowImpl implements CashlessPaymentWorkflow {

    private static final Logger log = Workflow.getLogger(CashlessPaymentWorkflowImpl.class);

    private ValidateFloatCapacityActivity validateFloatCapacityActivity;
    private BlockFloatActivity blockFloatActivity;
    private CommitFloatActivity commitFloatActivity;
    private ReleaseFloatActivity releaseFloatActivity;
    private GenerateDynamicQRActivity generateDynamicQRActivity;
    private WaitForQRPaymentActivity waitForQRPaymentActivity;
    private SendRequestToPayActivity sendRequestToPayActivity;
    private WaitForRTPApprovalActivity waitForRTPApprovalActivity;
    private CreditAgentFloatActivity creditAgentFloatActivity;
    private PersistWorkflowResultActivity persistWorkflowResultActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    public CashlessPaymentWorkflowImpl(
            ValidateFloatCapacityActivity validateFloatCapacityActivity,
            BlockFloatActivity blockFloatActivity,
            CommitFloatActivity commitFloatActivity,
            ReleaseFloatActivity releaseFloatActivity,
            GenerateDynamicQRActivity generateDynamicQRActivity,
            WaitForQRPaymentActivity waitForQRPaymentActivity,
            SendRequestToPayActivity sendRequestToPayActivity,
            WaitForRTPApprovalActivity waitForRTPApprovalActivity,
            CreditAgentFloatActivity creditAgentFloatActivity,
            PersistWorkflowResultActivity persistWorkflowResultActivity) {
        this.validateFloatCapacityActivity = validateFloatCapacityActivity;
        this.blockFloatActivity = blockFloatActivity;
        this.commitFloatActivity = commitFloatActivity;
        this.releaseFloatActivity = releaseFloatActivity;
        this.generateDynamicQRActivity = generateDynamicQRActivity;
        this.waitForQRPaymentActivity = waitForQRPaymentActivity;
        this.sendRequestToPayActivity = sendRequestToPayActivity;
        this.waitForRTPApprovalActivity = waitForRTPApprovalActivity;
        this.creditAgentFloatActivity = creditAgentFloatActivity;
        this.persistWorkflowResultActivity = persistWorkflowResultActivity;
    }

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    public CashlessPaymentWorkflowImpl() {
        this.validateFloatCapacityActivity = Workflow.newActivityStub(ValidateFloatCapacityActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
        this.blockFloatActivity = Workflow.newActivityStub(BlockFloatActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
        this.commitFloatActivity = Workflow.newActivityStub(CommitFloatActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
        this.releaseFloatActivity = Workflow.newActivityStub(ReleaseFloatActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
        this.generateDynamicQRActivity = Workflow.newActivityStub(GenerateDynamicQRActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
        this.waitForQRPaymentActivity = Workflow.newActivityStub(WaitForQRPaymentActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(10))
                .build());
        this.sendRequestToPayActivity = Workflow.newActivityStub(SendRequestToPayActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
        this.waitForRTPApprovalActivity = Workflow.newActivityStub(WaitForRTPApprovalActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(10))
                .build());
        this.creditAgentFloatActivity = Workflow.newActivityStub(CreditAgentFloatActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
        this.persistWorkflowResultActivity = Workflow.newActivityStub(PersistWorkflowResultActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .build());
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
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Insufficient float"));
                return failResult;
            }

            FloatBlockResult blockResult;
            try {
                blockResult = blockFloatActivity.blockFloat(
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
            } catch (ActivityFailure e) {
                log.error("ActivityFailure during blockFloat: {}", e.getMessage());
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILURE", "Float block activity failed: " + e.getMessage(), "REVIEW");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "ActivityFailure: " + e.getMessage()));
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

            String reference = null;
            if ("QR".equalsIgnoreCase(input.paymentMethod())) {
                QRGenerationResult qrResult;
                try {
                    qrResult = generateDynamicQRActivity.generate(input.amount(), input.agentId(), input.idempotencyKey());
                } catch (ActivityFailure e) {
                    releaseFloatActivity.releaseFloat(
                            new FloatReleaseInput(input.agentId(), input.amount(), transactionId));
                    log.error("ActivityFailure during generateDynamicQR: {}", e.getMessage());
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILURE", "QR generation activity failed: " + e.getMessage(), "REVIEW");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "ActivityFailure: " + e.getMessage()));
                    return failResult;
                }
                if (qrResult.qrReference() == null) {
                    releaseFloatActivity.releaseFloat(
                            new FloatReleaseInput(input.agentId(), input.amount(), transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_QR_GENERATION_FAILED", "QR generation failed", "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "QR generation failed"));
                    return failResult;
                }
                reference = qrResult.qrReference();
                QRPaymentStatus paymentStatus;
                try {
                    paymentStatus = waitForQRPaymentActivity.waitForPayment(reference, 300);
                } catch (ActivityFailure e) {
                    releaseFloatActivity.releaseFloat(
                            new FloatReleaseInput(input.agentId(), input.amount(), transactionId));
                    log.error("ActivityFailure during waitForQRPayment: {}", e.getMessage());
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILURE", "QR payment wait activity failed: " + e.getMessage(), "REVIEW");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "ActivityFailure: " + e.getMessage()));
                    return failResult;
                }
                if (!"PAID".equals(paymentStatus.status())) {
                    releaseFloatActivity.releaseFloat(
                            new FloatReleaseInput(input.agentId(), input.amount(), transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_QR_PAYMENT_TIMEOUT", "QR payment timeout", "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "QR payment timeout"));
                    return failResult;
                }
            } else if ("RTP".equalsIgnoreCase(input.paymentMethod())) {
                var rtpResult = sendRequestToPayActivity.send(input.customerProxy(), input.amount(), input.idempotencyKey());
                if (rtpResult.rtpReference() == null) {
                    releaseFloatActivity.releaseFloat(
                            new FloatReleaseInput(input.agentId(), input.amount(), transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_RTP_SEND_FAILED", "RTP send failed", "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "RTP send failed"));
                    return failResult;
                }
                reference = rtpResult.rtpReference();
                var rtpStatus = waitForRTPApprovalActivity.waitForApproval(reference, 300);
                if (!"APPROVED".equals(rtpStatus.status())) {
                    releaseFloatActivity.releaseFloat(
                            new FloatReleaseInput(input.agentId(), input.amount(), transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_RTP_DECLINED", "RTP declined", "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "RTP declined"));
                    return failResult;
                }
            }

            commitFloatActivity.commitFloat(
                    new FloatCommitInput(input.agentId(), input.amount(), transactionId));

            currentStatus = WorkflowStatus.COMPLETED;
            WorkflowResult completedResult = WorkflowResult.completed(transactionId, reference, input.amount(), BigDecimal.ZERO);
            persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                    input.idempotencyKey(), "COMPLETED", null, null, reference, BigDecimal.ZERO, reference, null));
            return completedResult;

        } catch (Exception e) {
            log.error("CashlessPayment workflow failed: {}", e.getMessage());
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
    public WorkflowStatus getStatus() {
        return currentStatus;
    }
}