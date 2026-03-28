package com.agentbanking.ledger.infrastructure.web;

import com.agentbanking.ledger.domain.model.SettlementSummaryRecord;
import com.agentbanking.ledger.domain.service.SettlementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/settlement")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculateSettlement(
            @RequestParam UUID agentId,
            @RequestParam String date) {
        SettlementSummaryRecord result = settlementService.calculateNetSettlement(
                agentId, LocalDate.parse(date));

        return ResponseEntity.ok(mapToResponse(result));
    }

    @PostMapping("/eod")
    public ResponseEntity<List<Map<String, Object>>> runEodSettlement(
            @RequestParam String date) {
        List<SettlementSummaryRecord> results = settlementService.runEodSettlement(
                LocalDate.parse(date));

        return ResponseEntity.ok(results.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/cbs-file")
    public ResponseEntity<String> generateCbsFile(@RequestParam String date) {
        List<SettlementSummaryRecord> settlements = settlementService.runEodSettlement(
                LocalDate.parse(date));

        String csv = settlementService.generateCbsFile(settlements);
        return ResponseEntity.ok(csv);
    }

    private Map<String, Object> mapToResponse(SettlementSummaryRecord record) {
        return Map.ofEntries(
                Map.entry("settlementId", record.settlementId().toString()),
                Map.entry("agentId", record.agentId().toString()),
                Map.entry("settlementDate", record.settlementDate().toString()),
                Map.entry("totalWithdrawals", record.totalWithdrawals()),
                Map.entry("totalDeposits", record.totalDeposits()),
                Map.entry("totalBillPayments", record.totalBillPayments()),
                Map.entry("totalRetailSales", record.totalRetailSales()),
                Map.entry("totalCommissions", record.totalCommissions()),
                Map.entry("netAmount", record.netAmount()),
                Map.entry("direction", record.direction().name()),
                Map.entry("currency", record.currency())
        );
    }
}
