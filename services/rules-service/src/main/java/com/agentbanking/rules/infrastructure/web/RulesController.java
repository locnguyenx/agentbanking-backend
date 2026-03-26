package com.agentbanking.rules.infrastructure.web;

import com.agentbanking.rules.domain.model.AgentTier;
import com.agentbanking.rules.domain.model.TransactionType;
import com.agentbanking.rules.domain.port.in.FeeQueryUseCase;
import com.agentbanking.rules.domain.port.in.FeeQueryUseCase.FeeQueryResult;
import com.agentbanking.rules.domain.port.in.VelocityCheckUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/internal")
public class RulesController {

    private final FeeQueryUseCase feeQueryUseCase;
    private final VelocityCheckUseCase velocityCheckUseCase;

    public RulesController(FeeQueryUseCase feeQueryUseCase,
                           VelocityCheckUseCase velocityCheckUseCase) {
        this.feeQueryUseCase = feeQueryUseCase;
        this.velocityCheckUseCase = velocityCheckUseCase;
    }

    @PostMapping("/fees/calculate")
    public ResponseEntity<Map<String, Object>> calculateFees(
            @RequestParam String transactionType,
            @RequestParam String agentTier,
            @RequestParam BigDecimal amount) {
        TransactionType txType = TransactionType.valueOf(transactionType);
        AgentTier tier = AgentTier.valueOf(agentTier);

        FeeQueryResult result = feeQueryUseCase.calculate(amount, txType, tier);

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
        int transactionCountToday = (Integer) request.get("transactionCountToday");
        BigDecimal amountToday = new BigDecimal(request.get("amountToday").toString());

        var result = velocityCheckUseCase.check(transactionCountToday, amountToday);

        return ResponseEntity.ok(Map.of(
            "passed", result.passed(),
            "errorCode", result.errorCode() != null ? result.errorCode() : ""
        ));
    }

    @GetMapping("/limits/{transactionType}/{agentTier}")
    public ResponseEntity<Map<String, Object>> getLimits(
            @PathVariable String transactionType,
            @PathVariable String agentTier) {
        return ResponseEntity.ok(Map.of(
            "transactionType", transactionType,
            "agentTier", agentTier,
            "dailyLimitAmount", "10000.00",
            "dailyLimitCount", 10
        ));
    }
}
