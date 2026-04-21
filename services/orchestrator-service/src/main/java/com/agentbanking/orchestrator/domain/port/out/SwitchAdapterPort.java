package com.agentbanking.orchestrator.domain.port.out;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

public interface SwitchAdapterPort {

    SwitchAuthorizationResult authorizeTransaction(SwitchAuthorizationInput input);

    SwitchReversalResult sendReversal(SwitchReversalInput input);

    ProxyEnquiryResult proxyEnquiry(ProxyEnquiryInput input);

    DuitNowTransferResult sendDuitNowTransfer(DuitNowTransferInput input);

    record SwitchAuthorizationInput(
        String pan,
        String pinBlock,
        BigDecimal amount,
        UUID internalTransactionId
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SwitchAuthorizationResult(
        String status,
        String reference,
        String responseCode,
        String errorCode
    ) {
        public boolean isApproved() {
            return "SUCCESS".equalsIgnoreCase(status);
        }
    }

    record SwitchReversalInput(
        UUID internalTransactionId
    ) {}

    record SwitchReversalResult(
        boolean success,
        String errorCode
    ) {}

    record ProxyEnquiryInput(
        String proxyType,
        String proxyValue
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProxyEnquiryResult(
        boolean valid,
        String recipientName,
        String bankCode,
        String errorCode
    ) {}

    record DuitNowTransferInput(
        String recipientBank,
        String recipientAccount,
        BigDecimal amount,
        UUID internalTransactionId
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DuitNowTransferResult(
        String status,
        String reference,
        String errorCode
    ) {
        public boolean isSuccess() {
            return "SUCCESS".equalsIgnoreCase(status);
        }
    }
}
