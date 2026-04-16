package com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.activity.CalculateMDRActivity;
import com.agentbanking.orchestrator.application.activity.ValidateFloatCapacityActivity;
import com.agentbanking.orchestrator.application.activity.GenerateDynamicQRActivity;
import com.agentbanking.orchestrator.application.activity.WaitForQRPaymentActivity;
import com.agentbanking.orchestrator.application.activity.SendRequestToPayActivity;
import com.agentbanking.orchestrator.application.activity.WaitForRTPApprovalActivity;
import com.agentbanking.orchestrator.application.activity.CreateMerchantTransactionRecordActivity;
import com.agentbanking.orchestrator.application.activity.BlockFloatActivity;
import com.agentbanking.orchestrator.application.activity.CommitFloatActivity;
import com.agentbanking.orchestrator.application.activity.ReleaseFloatActivity;
import com.agentbanking.orchestrator.application.activity.CreditAgentFloatActivity;
import com.agentbanking.orchestrator.application.workflow.HybridCashbackWorkflow;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.QRPaymentPort.QRGenerationResult;
import com.agentbanking.orchestrator.domain.port.out.QRPaymentPort.QRPaymentStatus;
import com.agentbanking.orchestrator.domain.port.out.RequestToPayPort.RTPResult;
import com.agentbanking.orchestrator.domain.port.out.RequestToPayPort.RTPStatus;

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
public class HybridCashbackWorkflowImpl implements HybridCashbackWorkflow {

    private static final Logger log = Workflow.getLogger(HybridCashbackWorkflowImpl.class);

    private ValidateFloatCapacityActivity validateFloatCapacityActivity;
    private CalculateMDRActivity calculateMDRActivity;
    private GenerateDynamicQRActivity generateDynamicQRActivity;
    private WaitForQRPaymentActivity waitForQRPaymentActivity;
    private SendRequestToPayActivity sendRequestToPayActivity;
    private WaitForRTPApprovalActivity waitForRTPApprovalActivity;
    private CreateMerchantTransactionRecordActivity createMerchantTransactionRecordActivity;
    private BlockFloatActivity blockFloatActivity;
    private CommitFloatActivity commitFloatActivity;
    private ReleaseFloatActivity releaseFloatActivity;
    private CreditAgentFloatActivity creditAgentFloatActivity;
    private PersistWorkflowResultActivity persistWorkflowResultActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    public HybridCashbackWorkflowImpl(
            ValidateFloatCapacityActivity validateFloatCapacityActivity,
            CalculateMDRActivity calculateMDRActivity,
            GenerateDynamicQRActivity generateDynamicQRActivity,
            WaitForQRPaymentActivity waitForQRPaymentActivity,
            SendRequestToPayActivity sendRequestToPayActivity,
            WaitForRTPApprovalActivity waitForRTPApprovalActivity,
            CreateMerchantTransactionRecordActivity createMerchantTransactionRecordActivity,
            BlockFloatActivity blockFloatActivity,
            CommitFloatActivity commitFloatActivity,
            ReleaseFloatActivity releaseFloatActivity,
            CreditAgentFloatActivity creditAgentFloatActivity,
            PersistWorkflowResultActivity persistWorkflowResultActivity) {
        this.validateFloatCapacityActivity = validateFloatCapacityActivity;
        this.calculateMDRActivity = calculateMDRActivity;
        this.generateDynamicQRActivity = generateDynamicQRActivity;
        this.waitForQRPaymentActivity = waitForQRPaymentActivity;
        this.sendRequestToPayActivity = sendRequestToPayActivity;
        this.waitForRTPApprovalActivity = waitForRTPApprovalActivity;
        this.createMerchantTransactionRecordActivity = createMerchantTransactionRecordActivity;
        this.blockFloatActivity = blockFloatActivity;
        this.commitFloatActivity = commitFloatActivity;
        this.releaseFloatActivity = releaseFloatActivity;
        this.creditAgentFloatActivity = creditAgentFloatActivity;
        this.persistWorkflowResultActivity = persistWorkflowResultActivity;
    }

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    public HybridCashbackWorkflowImpl() {
        this.validateFloatCapacityActivity = Workflow.newActivityStub(ValidateFloatCapacityActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.calculateMDRActivity = Workflow.newActivityStub(CalculateMDRActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.generateDynamicQRActivity = Workflow.newActivityStub(GenerateDynamicQRActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.waitForQRPaymentActivity = Workflow.newActivityStub(WaitForQRPaymentActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(10))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.sendRequestToPayActivity = Workflow.newActivityStub(SendRequestToPayActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.waitForRTPApprovalActivity = Workflow.newActivityStub(WaitForRTPApprovalActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(10))
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
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.creditAgentFloatActivity = Workflow.newActivityStub(CreditAgentFloatActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
        this.persistWorkflowResultActivity = Workflow.newActivityStub(PersistWorkflowResultActivity.class, ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                .build());
    }

    @Override
    public WorkflowResult execute(HybridCashbackInput input) {
        log.info("Workflow started: HybridCashback, agentId={}, method={}", input.agentId(), input.paymentMethod());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            BigDecimal totalAmount = input.purchaseAmount().add(input.cashbackAmount());
            
            var capacityResult = validateFloatCapacityActivity.validate(input.agentId(), totalAmount);
            if (!capacityResult.sufficient()) {
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_INSUFFICIENT_FLOAT", "Insufficient float for cashback", "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Insufficient float for cashback"));
                return failResult;
            }

            var mdrResult = calculateMDRActivity.calculate("HYBRID_CASHBACK", input.paymentMethod(), input.purchaseAmount());
            
            FloatBlockResult blockResult;
            try {
                blockResult = blockFloatActivity.blockFloat(
                        new FloatBlockInput(
                            input.agentId(), 
                            totalAmount, 
                            BigDecimal.ZERO,
                            mdrResult.mdrAmount(),
                            BigDecimal.ZERO,
                            Workflow.getInfo().getWorkflowId(),
                            null,
                            input.geofenceLat(),
                            input.geofenceLng(),
                            input.agentTier(),
                            input.targetBin()
                        ));
            } catch (ActivityFailure e) {
                log.error("Block float activity failed: {}", e.getMessage());
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_FLOAT_BLOCK_FAILED", "Float block failed: " + e.getMessage(), "DECLINE");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Block float activity failed"));
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
                    qrResult = generateDynamicQRActivity.generate(input.purchaseAmount(), input.agentId(), Workflow.getInfo().getWorkflowId());
                } catch (ActivityFailure e) {
                    log.error("Generate QR activity failed: {}", e.getMessage());
                    releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_QR_GENERATION_FAILED", "QR generation failed: " + e.getMessage(), "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Generate QR activity failed"));
                    return failResult;
                }
                if (qrResult.qrReference() == null) {
                    releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_QR_GENERATION_FAILED", "QR generation failed", "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "QR generation failed"));
                    return failResult;
                }
                reference = qrResult.qrReference();
                QRPaymentStatus paymentStatus;
                try {
                    paymentStatus = waitForQRPaymentActivity.waitForPayment(reference, 300);
                } catch (ActivityFailure e) {
                    log.error("Wait for QR payment activity failed: {}", e.getMessage());
                    releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_QR_PAYMENT_FAILED", "QR payment failed: " + e.getMessage(), "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Wait for QR payment activity failed"));
                    return failResult;
                }
                if (!"PAID".equals(paymentStatus.status())) {
                    releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_QR_PAYMENT_TIMEOUT", "QR payment timeout", "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "QR payment timeout"));
                    return failResult;
                }
            } else if ("RTP".equalsIgnoreCase(input.paymentMethod())) {
                RTPResult rtpResult;
                try {
                    rtpResult = sendRequestToPayActivity.send(input.customerProxy(), input.purchaseAmount(), Workflow.getInfo().getWorkflowId());
                } catch (ActivityFailure e) {
                    log.error("Send RTP activity failed: {}", e.getMessage());
                    releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_RTP_SEND_FAILED", "RTP send failed: " + e.getMessage(), "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Send RTP activity failed"));
                    return failResult;
                }
                if (rtpResult.rtpReference() == null) {
                    releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_RTP_SEND_FAILED", "RTP send failed", "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "RTP send failed"));
                    return failResult;
                }
                reference = rtpResult.rtpReference();
                RTPStatus rtpStatus;
                try {
                    rtpStatus = waitForRTPApprovalActivity.waitForApproval(reference, 300);
                } catch (ActivityFailure e) {
                    log.error("Wait for RTP approval activity failed: {}", e.getMessage());
                    releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_RTP_APPROVAL_FAILED", "RTP approval failed: " + e.getMessage(), "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Wait for RTP approval activity failed"));
                    return failResult;
                }
                if (!"APPROVED".equals(rtpStatus.status())) {
                    releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    WorkflowResult failResult = WorkflowResult.failed("ERR_RTP_DECLINED", "RTP declined", "DECLINE");
                    persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                            Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "RTP declined"));
                    return failResult;
                }
            }

            var recordResult = createMerchantTransactionRecordActivity.create(
                    transactionId, "HYBRID", input.purchaseAmount(), mdrResult.mdrRate(), 
                    mdrResult.mdrAmount(), mdrResult.netAmount(), "CASHBACK_RECEIPT");

            try {
                commitFloatActivity.commitFloat(new FloatCommitInput(input.agentId(), mdrResult.netAmount(), transactionId));
            } catch (ActivityFailure e) {
                log.error("Commit float activity failed: {}", e.getMessage());
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_FLOAT_COMMIT_FAILED", "Float commit failed: " + e.getMessage(), "REVIEW");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Commit float activity failed"));
                return failResult;
            }
            try {
                creditAgentFloatActivity.creditAgentFloat(new FloatCreditInput(
                        input.agentId(), 
                        input.cashbackAmount(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        Workflow.getInfo().getWorkflowId(),
                        input.customerProxy(),
                        input.agentTier(),
                        input.targetBin(),
                        null,
                        input.geofenceLat(),
                        input.geofenceLng()
                ));
            } catch (ActivityFailure e) {
                log.error("Credit agent float activity failed: {}", e.getMessage());
                currentStatus = WorkflowStatus.FAILED;
                WorkflowResult failResult = WorkflowResult.failed("ERR_CASHBACK_CREDIT_FAILED", "Cashback credit failed: " + e.getMessage(), "REVIEW");
                persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        Workflow.getInfo().getWorkflowId(), "FAILED", failResult.errorCode(), failResult.errorMessage(), null, null, null, "Credit agent float activity failed"));
                return failResult;
            }

            currentStatus = WorkflowStatus.COMPLETED;
            WorkflowResult completedResult = WorkflowResult.completed(transactionId, reference, input.purchaseAmount(), mdrResult.mdrAmount());
            persistWorkflowResultActivity.persistResult(new PersistWorkflowResultActivity.Input(
                    Workflow.getInfo().getWorkflowId(), "COMPLETED", null, null, reference, mdrResult.mdrAmount(), reference, null));
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
            log.error("HybridCashback workflow failed: {}", e.getMessage());
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
