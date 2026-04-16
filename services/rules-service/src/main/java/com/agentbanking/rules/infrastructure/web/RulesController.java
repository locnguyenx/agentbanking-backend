package com.agentbanking.rules.infrastructure.web;

import com.agentbanking.rules.domain.model.AgentTier;
import com.agentbanking.rules.domain.model.FeeType;
import com.agentbanking.rules.domain.model.TransactionType;
import com.agentbanking.rules.domain.port.in.CreateFeeConfigUseCase;
import com.agentbanking.rules.domain.port.in.FeeQueryUseCase;
import com.agentbanking.rules.domain.port.in.FeeQueryUseCase.FeeQueryResult;
import com.agentbanking.rules.domain.port.in.VelocityCheckUseCase;
import com.agentbanking.rules.domain.port.in.TransactionQuoteUseCase;
import com.agentbanking.rules.infrastructure.web.dto.TransactionQuoteRequest;
import com.agentbanking.rules.infrastructure.web.dto.TransactionQuoteResponse;
import com.agentbanking.common.exception.ErrorResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/internal")
public class RulesController {

    private final FeeQueryUseCase feeQueryUseCase;
    private final VelocityCheckUseCase velocityCheckUseCase;
    private final CreateFeeConfigUseCase createFeeConfigUseCase;
    private final TransactionQuoteUseCase transactionQuoteUseCase;

    public RulesController(FeeQueryUseCase feeQueryUseCase,
                           VelocityCheckUseCase velocityCheckUseCase,
                           CreateFeeConfigUseCase createFeeConfigUseCase,
                           TransactionQuoteUseCase transactionQuoteUseCase) {
        this.feeQueryUseCase = feeQueryUseCase;
        this.velocityCheckUseCase = velocityCheckUseCase;
        this.createFeeConfigUseCase = createFeeConfigUseCase;
        this.transactionQuoteUseCase = transactionQuoteUseCase;
    }

    @PostMapping("/fees/calculate")
    public ResponseEntity<Map<String, Object>> calculateFees(
            @RequestParam String transactionType,
            @RequestParam String agentTier,
            @RequestParam BigDecimal amount) {
        TransactionType txType = TransactionType.fromFrontend(transactionType);
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

    @PostMapping("/fees")
    public ResponseEntity<Map<String, Object>> createFeeConfig(@RequestBody Map<String, Object> request) {
        try {
            TransactionType transactionType = TransactionType.fromFrontend((String) request.get("transactionType"));
            AgentTier agentTier = AgentTier.valueOf((String) request.get("agentTier"));
            FeeType feeType = FeeType.valueOf((String) request.get("feeType"));
            BigDecimal customerFeeValue = new BigDecimal(request.get("customerFeeValue").toString());
            BigDecimal agentCommissionValue = new BigDecimal(request.get("agentCommissionValue").toString());
            BigDecimal bankShareValue = new BigDecimal(request.get("bankShareValue").toString());
            BigDecimal dailyLimitAmount = new BigDecimal(request.get("dailyLimitAmount").toString());
            Integer dailyLimitCount = (Integer) request.get("dailyLimitCount");
            LocalDate effectiveFrom = LocalDate.parse((String) request.get("effectiveFrom"));
            LocalDate effectiveTo = request.containsKey("effectiveTo") && request.get("effectiveTo") != null 
                ? LocalDate.parse((String) request.get("effectiveTo")) 
                : null;

            CreateFeeConfigUseCase.CreateFeeConfigCommand command = new CreateFeeConfigUseCase.CreateFeeConfigCommand(
                transactionType, agentTier, feeType, customerFeeValue, 
                agentCommissionValue, bankShareValue, dailyLimitAmount, 
                dailyLimitCount, effectiveFrom, effectiveTo
            );

            CreateFeeConfigUseCase.CreateFeeConfigResult result = createFeeConfigUseCase.createFeeConfig(command);

            return ResponseEntity.status(201).body(Map.of(
                "feeConfigId", result.feeConfigId().toString(),
                "transactionType", result.transactionType().name(),
                "agentTier", result.agentTier().name(),
                "status", result.status()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_FEE_CONFIG_CREATE_FAILED", "message", e.getMessage())
            ));
        }
    }

    @PostMapping("/check-velocity")
    public ResponseEntity<Map<String, Object>> checkVelocity(
            @RequestBody Map<String, Object> request) {
        int transactionCountToday = request.get("transactionCountToday") != null 
            ? (Integer) request.get("transactionCountToday") : 0;
        BigDecimal amountToday = request.get("amountToday") != null 
            ? new BigDecimal(request.get("amountToday").toString()) 
            : BigDecimal.ZERO;

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

    @PostMapping("/transactions/quote")
    public ResponseEntity<?> getQuote(@Valid @RequestBody TransactionQuoteRequest request,
                                       @RequestHeader(value = "X-Agent-Id", required = false) String agentId,
                                       @RequestHeader(value = "X-Agent-Tier", required = false) String agentTier) {
        try {
            TransactionQuoteUseCase.QuoteResult result = transactionQuoteUseCase.calculateQuote(
                agentId != null ? agentId : "unknown",
                agentTier != null ? agentTier : "STANDARD",
                request.amount(),
                request.serviceCode(),
                request.fundingSource(),
                request.billerRouting()
            );

            return ResponseEntity.ok(TransactionQuoteResponse.from(result));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(
                "ERR_BIZ_QUOTE_CALCULATION_FAILED",
                e.getMessage(),
                "RETRY"
            ));
        }
    }
}
