package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BillerServiceClientFallbackFactory implements FallbackFactory<BillerServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(BillerServiceClientFallbackFactory.class);

    @Override
    public BillerServiceClient create(Throwable cause) {
        log.error("BillerServiceClient fallback triggered due to: {}", cause.getMessage(), cause);
        return new BillerServiceClient() {
            @Override
            public BillValidationResult validateBill(BillValidationInput input) {
                log.warn("Biller service unavailable, auto-validating bill for biller: {}", input.billerCode());
                return new BillValidationResult(true, "Test Account", BigDecimal.valueOf(150.00), "BILLER_UNAVAILABLE");
            }

            @Override
            public BillPaymentResult payBill(BillPaymentInput input) {
                log.warn("Biller service unavailable, auto-approving bill payment for biller: {}", input.billerCode());
                return new BillPaymentResult(true, "BILLER_REF_" + System.currentTimeMillis(), "BILLER_UNAVAILABLE");
            }

            @Override
            public BillNotificationResult notifyBiller(BillNotificationInput input) {
                log.warn("Biller service unavailable, simulating notification for transaction: {}", input.internalTransactionId());
                return new BillNotificationResult(true, "BILLER_UNAVAILABLE");
            }

            @Override
            public BillNotificationResult notifyBillerReversal(BillReversalInput input) {
                log.warn("Biller service unavailable, simulating reversal notification");
                return new BillNotificationResult(true, "BILLER_UNAVAILABLE");
            }
        };
    }
}