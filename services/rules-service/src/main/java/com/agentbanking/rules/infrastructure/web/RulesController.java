package com.agentbanking.rules.infrastructure.web;

import com.agentbanking.rules.domain.model.AgentTier;
import com.agentbanking.rules.domain.model.TransactionType;
import com.agentbanking.rules.domain.service.FeeCalculationService;
import com.agentbanking.rules.domain.service.FeeCalculationService.FeeCalculationResult;
import com.agentbanking.rules.domain.service.LimitEnforcementService;
import com.agentbanking.rules.domain.service.VelocityCheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/internal")
public class RulesController {

    private final FeeCalculationService feeCalculationService;
    private final LimitEnforcementService limitEnforcementService;
    private final VelocityCheckService velocityCheckService;

    public RulesController(FeeCalculationService feeCalculationService,
                           LimitEnforcementService limitEnforcementService,
                           VelocityCheckService velocityCheckService) {
        this.feeCalculationService = feeCalculationService;
        this.limitEnforcementService = limitEnforcementService;
        this.velocityCheckService = velocityCheckService;
    }

    @PostMapping("/fees/calculate")
    public ResponseEntity<Map<String, Object>> calculateFees(
            @RequestParam String transactionType,
            @RequestParam String agentTier,
            @RequestParam BigDecimal amount) {
        TransactionType txType = TransactionType.valueOf(transactionType);
        AgentTier tier = AgentTier.valueOf(agentTier);

        FeeCalculationResult result = feeCalculationService.calculate(amount, txType, tier);

        return ResponseEntity.ok(Map.of(
            "customerFee", result.customerFee(),
            "agentCommission", result.agentCommission(),
            "bankShare", result.bankShare(),
            "transactionType", transactionType,
            "agentTier", agentTier
        ));
    }

    @GetMapping("/fees/{transactionType}/{agentTier}")
    public ResponseEntity<Map<String, Object>> getFeeConfig(
            @PathVariable String transactionType,
            @PathVariable String agentTier) {
        // Stub response - would normally query repository
        return ResponseEntity.ok(Map.of(
            "transactionType", transactionType,
            "agentTier", agentTier,
            "feeType", "FIXED",
            "customerFeeValue", "1.00",
            "agentCommissionValue", "0.20",
            "bankShareValue", "0.80",
            "dailyLimitAmount", "10000.00",
            "dailyLimitCount", 10
        ));
    }

    @PostMapping("/check-velocity")
    public ResponseEntity<Map<String, Object>> checkVelocity(
            @RequestBody Map<String, Object> request) {
        // Stub response - would normally check velocity rules
        return ResponseEntity.ok(Map.of(
            "passed", true,
            "message", "Velocity check passed"
        ));
    }

    @GetMapping("/limits/{transactionType}/{agentTier}")
    public ResponseEntity<Map<String, Object>> getLimits(
            @PathVariable String transactionType,
            @PathVariable String agentTier) {
        // Stub response
        return ResponseEntity.ok(Map.of(
            "transactionType", transactionType,
            "agentTier", agentTier,
            "dailyLimitAmount", "10000.00",
            "dailyLimitCount", 10
        ));
    }
}
