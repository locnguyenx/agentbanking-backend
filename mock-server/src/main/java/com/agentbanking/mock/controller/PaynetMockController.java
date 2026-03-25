package com.agentbanking.mock.controller;

import com.agentbanking.mock.config.MockConfig;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/mock/paynet")
public class PaynetMockController {

    private final MockConfig config;

    public PaynetMockController(MockConfig config) {
        this.config = config;
    }

    @PostMapping("/iso8583/auth")
    public Map<String, Object> cardAuth(@RequestBody Map<String, Object> request) {
        String defaultResponse = config.getPaynet().getDefaultResponse();
        if ("APPROVE".equals(defaultResponse)) {
            return Map.of(
                "responseCode", "00",
                "status", "APPROVED",
                "referenceId", "PAYNET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
            );
        } else {
            return Map.of(
                "responseCode", config.getPaynet().getDeclineCodes().get(0),
                "status", "DECLINED",
                "reason", "Insufficient funds"
            );
        }
    }

    @PostMapping("/iso8583/reversal")
    public Map<String, Object> reversal(@RequestBody Map<String, Object> request) {
        return Map.of(
            "status", "ACKNOWLEDGED",
            "referenceId", "REV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
    }

    @PostMapping("/iso20022/transfer")
    public Map<String, Object> duitNowTransfer(@RequestBody Map<String, Object> request) {
        return Map.of(
            "status", "SETTLED",
            "transactionId", "DN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            "settlementTime", System.currentTimeMillis()
        );
    }
}