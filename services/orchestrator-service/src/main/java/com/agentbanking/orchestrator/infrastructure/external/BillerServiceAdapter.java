package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.BillerServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BillerServiceAdapter implements BillerServicePort {

    private static final Logger log = LoggerFactory.getLogger(BillerServiceAdapter.class);

    private final BillerServiceClient billerServiceClient;

    public BillerServiceAdapter(BillerServiceClient billerServiceClient) {
        this.billerServiceClient = billerServiceClient;
    }

    @Override
    public BillValidationResult validateBill(BillValidationInput input) {
        log.info("Validating bill for biller: {}", input.billerCode());
        return billerServiceClient.validateBill(input);
    }

    @Override
    public BillPaymentResult payBill(BillPaymentInput input) {
        log.info("Processing bill payment for biller: {}", input.billerCode());
        return billerServiceClient.payBill(input);
    }

    @Override
    public BillNotificationResult notifyBiller(BillNotificationInput input) {
        log.info("Sending biller notification for transaction: {}", input.internalTransactionId());
        return billerServiceClient.notifyBiller(input);
    }

    @Override
    public BillNotificationResult notifyBillerReversal(BillReversalInput input) {
        log.info("Sending biller reversal notification for biller: {}", input.billerCode());
        return billerServiceClient.notifyBillerReversal(input);
    }
}
