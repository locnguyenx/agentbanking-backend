package com.agentbanking.ledger.infrastructure.external;

import com.agentbanking.common.exception.LedgerException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "switch-adapter-service", url = "${switch-adapter-service.url}")
public interface SwitchAdapterBalanceClient {

    @PostMapping("/internal/balance-inquiry")
    SwitchBalanceResponse getBalance(@RequestBody SwitchBalanceRequest request);

    record SwitchBalanceRequest(String encryptedCardData, String pinBlock) {}

    record SwitchBalanceResponse(
        String status,
        String responseCode,
        BigDecimal balance,
        String currency,
        String accountMasked
    ) {
        public boolean isSuccessful() {
            return "SUCCESS".equals(status) && "00".equals(responseCode);
        }
    }
}
