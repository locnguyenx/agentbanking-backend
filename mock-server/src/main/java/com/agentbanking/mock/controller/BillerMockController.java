package com.agentbanking.mock.controller;

import com.agentbanking.mock.data.TestDataService;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/mock/billers")
public class BillerMockController {

    private final TestDataService dataService;

    public BillerMockController(TestDataService dataService) {
        this.dataService = dataService;
    }

    @PostMapping("/{billerCode}/validate")
    public Map<String, Object> validate(@PathVariable String billerCode, @RequestBody Map<String, String> request) {
        String ref1 = request.get("ref1");
        boolean valid = dataService.isValidBillerRef(billerCode.toUpperCase(), ref1);
        if (valid) {
            return Map.of("valid", true, "amount", new BigDecimal("150.00"), "customerName", "MOCK CUSTOMER");
        }
        return Map.of("valid", false, "reason", "Reference not found");
    }

    @PostMapping("/{billerCode}/pay")
    public Map<String, Object> pay(@PathVariable String billerCode, @RequestBody Map<String, Object> request) {
        return Map.of(
            "status", "PAID",
            "receiptNo", billerCode.toUpperCase() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            "amount", request.get("amount")
        );
    }

    @PostMapping("/celcom/topup")
    public Map<String, Object> celcomTopup(@RequestBody Map<String, Object> request) {
        return Map.of(
            "status", "COMPLETED",
            "transactionId", "CEL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
    }

    @PostMapping("/m1/topup")
    public Map<String, Object> m1Topup(@RequestBody Map<String, Object> request) {
        return Map.of(
            "status", "COMPLETED",
            "transactionId", "M1-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
    }
}
