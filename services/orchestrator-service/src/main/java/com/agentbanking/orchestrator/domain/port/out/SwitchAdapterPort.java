package com.agentbanking.orchestrator.domain.port.out;

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

    record SwitchAuthorizationResult(
        boolean approved,
        String referenceCode,
        String responseCode,
        String errorCode
    ) {}

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

    record DuitNowTransferResult(
        boolean success,
        String paynetReference,
        String errorCode
    ) {}
}
