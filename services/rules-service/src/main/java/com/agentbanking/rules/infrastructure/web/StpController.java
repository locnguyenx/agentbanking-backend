package com.agentbanking.rules.infrastructure.web;

import com.agentbanking.rules.domain.model.FeeConfigRecord;
import com.agentbanking.rules.domain.model.StpCategory;
import com.agentbanking.rules.domain.port.in.StpEvaluationUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/internal/stp")
public class StpController {

    private final StpEvaluationUseCase stpEvaluationUseCase;

    public StpController(StpEvaluationUseCase stpEvaluationUseCase) {
        this.stpEvaluationUseCase = stpEvaluationUseCase;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<Map<String, Object>> evaluateStp(
            @RequestBody Map<String, Object> request) {
        String transactionType = (String) request.get("transactionType");
        String agentId = (String) request.get("agentId");
        String customerMykad = (String) request.get("customerMykad");
        BigDecimal amount = request.get("amount") != null
            ? new BigDecimal(request.get("amount").toString())
            : BigDecimal.ZERO;
        String agentTier = (String) request.get("agentTier");
        if (agentTier == null || agentTier.isBlank()) {
            agentTier = "STANDARD";
        }
        int transactionCountToday = request.containsKey("transactionCountToday")
                ? ((Number) request.get("transactionCountToday")).intValue() : 0;
        BigDecimal amountToday = request.containsKey("amountToday")
                ? new BigDecimal(request.get("amountToday").toString()) : BigDecimal.ZERO;
        BigDecimal todayTotalAmount = request.containsKey("todayTotalAmount")
                ? new BigDecimal(request.get("todayTotalAmount").toString()) : BigDecimal.ZERO;

        StpEvaluationUseCase.StpEvaluationCommand command = new StpEvaluationUseCase.StpEvaluationCommand(
                transactionType, agentId, customerMykad, amount, agentTier,
                transactionCountToday, amountToday, null, todayTotalAmount
        );

        StpEvaluationUseCase.StpEvaluationResponse response = stpEvaluationUseCase.evaluate(command);

        return ResponseEntity.ok(Map.of(
                "category", response.category().name(),
                "approved", response.approved(),
                "reason", response.reason()
        ));
    }

    @GetMapping("/micro-auto-approval")
    public ResponseEntity<Map<String, Object>> checkMicroAutoApproval(
            @RequestParam String agentTier,
            @RequestParam BigDecimal amount) {
        boolean eligible = "MICRO".equals(agentTier)
                && amount.compareTo(new BigDecimal("500.00")) <= 0;

        return ResponseEntity.ok(Map.of(
                "eligible", eligible,
                "agentTier", agentTier,
                "amount", amount
        ));
    }
}
