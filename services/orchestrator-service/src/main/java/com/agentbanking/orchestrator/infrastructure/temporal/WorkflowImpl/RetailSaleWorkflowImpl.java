package com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.activity.CalculateMDRActivity;
import com.agentbanking.orchestrator.domain.port.out.QRPaymentPort.QRGenerationResult;
import com.agentbanking.orchestrator.domain.port.out.QRPaymentPort.QRPaymentStatus;
import com.agentbanking.orchestrator.domain.port.out.RequestToPayPort.RTPResult;
import com.agentbanking.orchestrator.domain.port.out.RequestToPayPort.RTPStatus;
import com.agentbanking.orchestrator.application.activity.GenerateDynamicQRActivity;
import com.agentbanking.orchestrator.application.activity.WaitForQRPaymentActivity;
import com.agentbanking.orchestrator.application.activity.SendRequestToPayActivity;
import com.agentbanking.orchestrator.application.activity.WaitForRTPApprovalActivity;
import com.agentbanking.orchestrator.application.activity.CreateMerchantTransactionRecordActivity;
import com.agentbanking.orchestrator.application.activity.BlockFloatActivity;
import com.agentbanking.orchestrator.application.activity.CommitFloatActivity;
import com.agentbanking.orchestrator.application.activity.ReleaseFloatActivity;
import com.agentbanking.orchestrator.application.workflow.RetailSaleWorkflow;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.*;

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
public class RetailSaleWorkflowImpl implements RetailSaleWorkflow {

    private static final Logger log = Workflow.getLogger(RetailSaleWorkflowImpl.class);

    private CalculateMDRActivity calculateMDRActivity;
    private GenerateDynamicQRActivity generateDynamicQRActivity;
    private WaitForQRPaymentActivity waitForQRPaymentActivity;
    private SendRequestToPayActivity sendRequestToPayActivity;
    private WaitForRTPApprovalActivity waitForRTPApprovalActivity;
    private CreateMerchantTransactionRecordActivity createMerchantTransactionRecordActivity;
    private BlockFloatActivity blockFloatActivity;
    private CommitFloatActivity commitFloatActivity;
    private ReleaseFloatActivity releaseFloatActivity;
    private PersistWorkflowResultActivity persistWorkflowResultActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    public RetailSaleWorkflowImpl(
            CalculateMDRActivity calculateMDRActivity,
            GenerateDynamicQRActivity generateDynamicQRActivity,
            WaitForQRPaymentActivity waitForQRPaymentActivity,
            SendRequestToPayActivity sendRequestToPayActivity,
            WaitForRTPApprovalActivity waitForRTPApprovalActivity,
            CreateMerchantTransactionRecordActivity createMerchantTransactionRecordActivity,
            BlockFloatActivity blockFloatActivity,
            CommitFloatActivity commitFloatActivity,
            ReleaseFloatActivity releaseFloatActivity,
            PersistWorkflowResultActivity persistWorkflowResultActivity) {
        this.calculateMDRActivity = calculateMDRActivity;
        this.generateDynamicQRActivity = generateDynamicQRActivity;
        this.waitForQRPaymentActivity = waitForQRPaymentActivity;
        this.sendRequestToPayActivity = sendRequestToPayActivity;
        this.waitForRTPApprovalActivity = waitForRTPApprovalActivity;
        this.createMerchantTransactionRecordActivity = createMerchantTransactionRecordActivity;
        this.blockFloatActivity = blockFloatActivity;
        this.commitFloatActivity = commitFloatActivity;
        this.releaseFloatActivity = releaseFloatActivity;
        this.persistWorkflowResultActivity = persistWorkflowResultActivity;
    }

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    public RetailSaleWorkflowImpl() {
        this.calculateMDRActivity = Workflow.newActivityStub(CalculateMDRActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.generateDynamicQRActivity = Workflow.newActivityStub(GenerateDynamicQRActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(60))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.waitForQRPaymentActivity = Workflow.newActivityStub(WaitForQRPaymentActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(60))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.sendRequestToPayActivity = Workflow.newActivityStub(SendRequestToPayActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(60))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.waitForRTPApprovalActivity = Workflow.newActivityStub(WaitForRTPApprovalActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(60))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.createMerchantTransactionRecordActivity = Workflow.newActivityStub(CreateMerchantTransactionRecordActivity.class, ActivityOptions.newBuilder()
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
                .setStartToCloseTimeout(Duration.ofSeconds(60))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.persistWorkflowResultActivity = Workflow.newActivityStub(PersistWorkflowResultActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
    }

    @Override
    public WorkflowResult execute(RetailSaleInput input) {
        log.info("Workflow started: RetailSale, agentId={}, method={}", input.agentId(), input.paymentMethod());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            var mdrResult = calculateMDRActivity.calculate("RETAIL_SALE", input.paymentMethod(), input.amount());
            
            FloatBlockResult blockResult;
            try {
                blockResult = blockFloatActivity.blockFloat(
                        new FloatBlockInput(
                                input.agentId(),
                                input.amount(),
                                BigDecimal.ZERO,
                                mdrResult.mdrAmount(),
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
                log.error("Activity failed in blockFloat: {}", e.getMessage());
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILED", "Block float activity failed: " + e.getMessage(), "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Activity failure: " + e.getMessage()));
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
                    log.error("Activity failed in generateQR: {}", e.getMessage());
                    releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), input.amount(), transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILED", "QR generation activity failed: " + e.getMessage(), "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Activity failure: " + e.getMessage()));
                    return failResult;
                }
                if (qrResult.qrReference() == null) {
                    releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), input.amount(), transactionId));
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
                    log.error("Activity failed in waitForPayment: {}", e.getMessage());
                    releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), input.amount(), transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILED", "Wait for payment activity failed: " + e.getMessage(), "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Activity failure: " + e.getMessage()));
                    return failResult;
                }
                if (!"PAID".equals(paymentStatus.status())) {
                    releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), input.amount(), transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_QR_PAYMENT_TIMEOUT", "QR payment timeout", "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "QR payment timeout"));
                    return failResult;
                }
            } else if ("RTP".equalsIgnoreCase(input.paymentMethod())) {
                RTPResult rtpResult;
                try {
                    rtpResult = sendRequestToPayActivity.send(input.customerProxy(), input.amount(), input.idempotencyKey());
                } catch (ActivityFailure e) {
                    log.error("Activity failed in sendRTP: {}", e.getMessage());
                    releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), input.amount(), transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILED", "RTP send activity failed: " + e.getMessage(), "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Activity failure: " + e.getMessage()));
                    return failResult;
                }
                if (rtpResult.rtpReference() == null) {
                    releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), input.amount(), transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_RTP_SEND_FAILED", "RTP send failed", "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "RTP send failed"));
                    return failResult;
                }
                reference = rtpResult.rtpReference();
                RTPStatus rtpStatus;
                try {
                    rtpStatus = waitForRTPApprovalActivity.waitForApproval(reference, 300);
                } catch (ActivityFailure e) {
                    log.error("Activity failed in waitForApproval: {}", e.getMessage());
                    releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), input.amount(), transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILED", "Wait for RTP approval activity failed: " + e.getMessage(), "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Activity failure: " + e.getMessage()));
                    return failResult;
                }
                if (!"APPROVED".equals(rtpStatus.status())) {
                    releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), input.amount(), transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_RTP_DECLINED", "RTP declined", "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "RTP declined"));
                    return failResult;
                }
            }

            var recordResult = createMerchantTransactionRecordActivity.create(
                    transactionId, input.merchantType(), input.amount(), mdrResult.mdrRate(), 
                    mdrResult.mdrAmount(), mdrResult.netAmount(), "RECEIPT");

            try {
                commitFloatActivity.commitFloat(new FloatCommitInput(input.agentId(), mdrResult.netAmount(), transactionId));
            } catch (ActivityFailure e) {
                log.error("Activity failed in commitFloat: {}", e.getMessage());
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_ACTIVITY_FAILED", "Commit float activity failed: " + e.getMessage(), "REVIEW");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        input.idempotencyKey(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Activity failure: " + e.getMessage()));
                return failResult;
            }

            currentStatus = WorkflowStatus.COMPLETED;
            WorkflowResult completedResult = WorkflowResult.completed(transactionId, reference, input.amount(), mdrResult.mdrAmount());
            persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                    input.idempotencyKey(), "COMPLETED", null, null, reference, mdrResult.mdrAmount(), reference, null));
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
            log.error("RetailSale workflow failed: {}", e.getMessage());
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
