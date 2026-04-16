package com.agentbanking.ledger.infrastructure.web;

import com.agentbanking.ledger.application.usecase.CreateAgentFloatUseCaseImpl;
import com.agentbanking.ledger.application.usecase.GetAgentFloatUseCaseImpl;
import com.agentbanking.ledger.application.usecase.GetAgentFloatTransactionsUseCaseImpl;
import com.agentbanking.ledger.domain.model.AgentFloatRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/backoffice/float")
public class BackofficeFloatController {

    private final GetAgentFloatUseCaseImpl getAgentFloatUseCase;
    private final GetAgentFloatTransactionsUseCaseImpl getAgentFloatTransactionsUseCase;
    private final CreateAgentFloatUseCaseImpl createAgentFloatUseCase;

    public BackofficeFloatController(
            GetAgentFloatUseCaseImpl getAgentFloatUseCase,
            GetAgentFloatTransactionsUseCaseImpl getAgentFloatTransactionsUseCase,
            CreateAgentFloatUseCaseImpl createAgentFloatUseCase) {
        this.getAgentFloatUseCase = getAgentFloatUseCase;
        this.getAgentFloatTransactionsUseCase = getAgentFloatTransactionsUseCase;
        this.createAgentFloatUseCase = createAgentFloatUseCase;
    }

    @GetMapping("/{agentId}")
    public ResponseEntity<Map<String, Object>> getAgentFloat(@PathVariable UUID agentId) {
        AgentFloatRecord floatRecord = getAgentFloatUseCase.getAgentFloat(agentId);

        if (floatRecord == null) {
            return ResponseEntity.ok(Map.of("exists", false));
        }

        BigDecimal availableBalance = floatRecord.balance().subtract(floatRecord.reservedBalance());

        Map<String, Object> floatMap = new HashMap<>();
        floatMap.put("floatId", floatRecord.floatId().toString());
        floatMap.put("balance", floatRecord.balance());
        floatMap.put("reservedBalance", floatRecord.reservedBalance());
        floatMap.put("availableBalance", availableBalance);
        floatMap.put("currency", floatRecord.currency());
        floatMap.put("gpsLat", floatRecord.merchantGpsLat());
        floatMap.put("gpsLng", floatRecord.merchantGpsLng());

        Map<String, Object> response = new HashMap<>();
        response.put("exists", true);
        response.put("float", floatMap);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{agentId}/float/transactions")
    public ResponseEntity<Map<String, Object>> getAgentFloatTransactions(
            @PathVariable UUID agentId,
            @RequestParam(required = false, defaultValue = "current") String period) {

        YearMonth yearMonth = switch (period.toLowerCase()) {
            case "current" -> YearMonth.now();
            case "previous" -> YearMonth.now().minusMonths(1);
            default -> YearMonth.parse(period);
        };

        var summary = getAgentFloatTransactionsUseCase.getSummary(agentId, yearMonth);

        Map<String, Object> response = new HashMap<>();
        response.put("agentId", summary.agentId().toString());
        response.put("period", summary.period().toString());
        response.put("totalCount", summary.totalCount());
        response.put("totalVolume", summary.totalVolume());
        response.put("byType", summary.byType().stream()
                .map(tb -> Map.of(
                        "type", tb.type(),
                        "count", tb.count(),
                        "volume", tb.volume()
                ))
                .toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{agentId}")
    public ResponseEntity<Map<String, Object>> createAgentFloat(
            @PathVariable UUID agentId,
            @Valid @RequestBody CreateAgentFloatRequest request) {

        try {
            AgentFloatRecord created = createAgentFloatUseCase.createAgentFloat(
                    agentId,
                    request.initialBalance(),
                    request.currency()
            );

            BigDecimal availableBalance = created.balance().subtract(created.reservedBalance());

            Map<String, Object> response = new HashMap<>();
            response.put("exists", true);
            response.put("float", Map.of(
                    "floatId", created.floatId().toString(),
                    "balance", created.balance(),
                    "reservedBalance", created.reservedBalance(),
                    "availableBalance", availableBalance,
                    "currency", created.currency()
            ));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "error", Map.of(
                            "code", "ERR_AGENT_FLOAT_EXISTS",
                            "message", e.getMessage()
                    )
            ));
        }
    }

    public record CreateAgentFloatRequest(
            @NotNull @Positive BigDecimal initialBalance,
            @NotBlank String currency
    ) {}
}