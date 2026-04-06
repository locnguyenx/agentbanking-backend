package com.agentbanking.orchestrator.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "ledger-merchant-txn", url = "${ledger-service.url}")
public interface MerchantTransactionClient {

    @PostMapping("/internal/merchant-transaction")
    MerchantTransactionResponse createRecord(@RequestBody MerchantTransactionRequest request);

    record MerchantTransactionRequest(
        UUID transactionId,
        String merchantType,
        BigDecimal grossAmount,
        BigDecimal mdrRate,
        BigDecimal mdrAmount,
        BigDecimal netCreditToFloat,
        String receiptType
    ) {}
    record MerchantTransactionResponse(boolean success, UUID recordId, String errorCode) {}
}
