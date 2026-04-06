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
import io.temporal.activity.ActivityOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@WorkflowImpl(workers = "agent-banking-tasks")
public class HybridCashbackWorkflowImpl implements HybridCashbackWorkflow {

    private static final Logger log = Workflow.getLogger(HybridCashbackWorkflowImpl.class);

    private final ValidateFloatCapacityActivity validateFloatCapacityActivity;
    private final CalculateMDRActivity calculateMDRActivity;
    private final GenerateDynamicQRActivity generateDynamicQRActivity;
    private final WaitForQRPaymentActivity waitForQRPaymentActivity;
    private final SendRequestToPayActivity sendRequestToPayActivity;
    private final WaitForRTPApprovalActivity waitForRTPApprovalActivity;
    private final CreateMerchantTransactionRecordActivity createMerchantTransactionRecordActivity;
    private final BlockFloatActivity blockFloatActivity;
    private final CommitFloatActivity commitFloatActivity;
    private final ReleaseFloatActivity releaseFloatActivity;
    private final CreditAgentFloatActivity creditAgentFloatActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    public HybridCashbackWorkflowImpl() {
        ActivityOptions defaultOptions = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .build();
        
        this.validateFloatCapacityActivity = Workflow.newActivityStub(ValidateFloatCapacityActivity.class, defaultOptions);
        this.calculateMDRActivity = Workflow.newActivityStub(CalculateMDRActivity.class, defaultOptions);
        this.generateDynamicQRActivity = Workflow.newActivityStub(GenerateDynamicQRActivity.class, defaultOptions);
        this.waitForQRPaymentActivity = Workflow.newActivityStub(WaitForQRPaymentActivity.class, defaultOptions);
        this.sendRequestToPayActivity = Workflow.newActivityStub(SendRequestToPayActivity.class, defaultOptions);
        this.waitForRTPApprovalActivity = Workflow.newActivityStub(WaitForRTPApprovalActivity.class, defaultOptions);
        this.createMerchantTransactionRecordActivity = Workflow.newActivityStub(CreateMerchantTransactionRecordActivity.class, defaultOptions);
        this.blockFloatActivity = Workflow.newActivityStub(BlockFloatActivity.class, defaultOptions);
        this.commitFloatActivity = Workflow.newActivityStub(CommitFloatActivity.class, defaultOptions);
        this.releaseFloatActivity = Workflow.newActivityStub(ReleaseFloatActivity.class, defaultOptions);
        this.creditAgentFloatActivity = Workflow.newActivityStub(CreditAgentFloatActivity.class, defaultOptions);
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
                return WorkflowResult.failed("ERR_INSUFFICIENT_FLOAT", "Insufficient float for cashback", "DECLINE");
            }

            var mdrResult = calculateMDRActivity.calculate("HYBRID_CASHBACK", input.paymentMethod(), input.purchaseAmount());
            
            FloatBlockResult blockResult = blockFloatActivity.blockFloat(
                    new FloatBlockInput(input.agentId(), totalAmount, input.idempotencyKey()));
            if (!blockResult.success()) {
                currentStatus = WorkflowStatus.FAILED;
                return WorkflowResult.failed(blockResult.errorCode(), "Float block failed", "DECLINE");
            }
            UUID transactionId = blockResult.transactionId();

            String reference = null;
            if ("QR".equalsIgnoreCase(input.paymentMethod())) {
                var qrResult = generateDynamicQRActivity.generate(input.purchaseAmount(), input.agentId(), input.idempotencyKey());
                if (qrResult.qrReference() == null) {
                    releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    return WorkflowResult.failed("ERR_QR_GENERATION_FAILED", "QR generation failed", "DECLINE");
                }
                reference = qrResult.qrReference();
                var paymentStatus = waitForQRPaymentActivity.waitForPayment(reference, 300);
                if (!"PAID".equals(paymentStatus.status())) {
                    releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    return WorkflowResult.failed("ERR_QR_PAYMENT_TIMEOUT", "QR payment timeout", "DECLINE");
                }
            } else if ("RTP".equalsIgnoreCase(input.paymentMethod())) {
                var rtpResult = sendRequestToPayActivity.send(input.customerProxy(), input.purchaseAmount(), input.idempotencyKey());
                if (rtpResult.rtpReference() == null) {
                    releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    return WorkflowResult.failed("ERR_RTP_SEND_FAILED", "RTP send failed", "DECLINE");
                }
                reference = rtpResult.rtpReference();
                var rtpStatus = waitForRTPApprovalActivity.waitForApproval(reference, 300);
                if (!"APPROVED".equals(rtpStatus.status())) {
                    releaseFloatActivity.releaseFloat(new FloatReleaseInput(input.agentId(), totalAmount, transactionId));
                    currentStatus = WorkflowStatus.FAILED;
                    return WorkflowResult.failed("ERR_RTP_DECLINED", "RTP declined", "DECLINE");
                }
            }

            var recordResult = createMerchantTransactionRecordActivity.create(
                    transactionId, "HYBRID", input.purchaseAmount(), mdrResult.mdrRate(), 
                    mdrResult.mdrAmount(), mdrResult.netAmount(), "CASHBACK_RECEIPT");

            commitFloatActivity.commitFloat(new FloatCommitInput(input.agentId(), mdrResult.netAmount(), transactionId));
            creditAgentFloatActivity.creditAgentFloat(new FloatCreditInput(input.agentId(), input.cashbackAmount()));

            currentStatus = WorkflowStatus.COMPLETED;
            return WorkflowResult.completed(transactionId, reference, input.purchaseAmount(), mdrResult.mdrAmount());

        } catch (Exception e) {
            log.error("HybridCashback workflow failed: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            return WorkflowResult.failed("ERR_SYS_WORKFLOW_FAILED", e.getMessage(), "REVIEW");
        }
    }

    @Override
    public WorkflowStatus getStatus() {
        return currentStatus;
    }
}
