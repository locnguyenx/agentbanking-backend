package com.agentbanking.ledger.infrastructure.web;

import com.agentbanking.ledger.domain.model.DiscrepancyCase;
import com.agentbanking.ledger.domain.port.in.ProcessDiscrepancyUseCase;
import com.agentbanking.ledger.domain.service.ReconciliationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;
    private final ProcessDiscrepancyUseCase processDiscrepancyUseCase;

    public ReconciliationController(ReconciliationService reconciliationService,
                                     ProcessDiscrepancyUseCase processDiscrepancyUseCase) {
        this.reconciliationService = reconciliationService;
        this.processDiscrepancyUseCase = processDiscrepancyUseCase;
    }

    @PostMapping("/run")
    public ResponseEntity<List<Map<String, Object>>> runReconciliation(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> internalTxns = (List<Map<String, Object>>) request.get("internalTransactions");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> networkTxns = (List<Map<String, Object>>) request.get("networkTransactions");

        List<DiscrepancyCase> discrepancies = reconciliationService.reconcile(internalTxns, networkTxns);

        return ResponseEntity.ok(discrepancies.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    @PostMapping("/discrepancy/maker-propose")
    public ResponseEntity<Map<String, Object>> makerPropose(@RequestBody Map<String, Object> request) {
        ProcessDiscrepancyUseCase.MakerCommand command = new ProcessDiscrepancyUseCase.MakerCommand(
                (String) request.get("caseId"),
                (String) request.get("action"),
                (String) request.get("userId"),
                (String) request.get("reason")
        );

        DiscrepancyCase result = processDiscrepancyUseCase.makerPropose(command);
        return ResponseEntity.ok(mapToResponse(result));
    }

    @PostMapping("/discrepancy/checker-approve")
    public ResponseEntity<Map<String, Object>> checkerApprove(@RequestBody Map<String, Object> request) {
        ProcessDiscrepancyUseCase.CheckerCommand command = new ProcessDiscrepancyUseCase.CheckerCommand(
                (String) request.get("caseId"),
                (String) request.get("userId"),
                (String) request.get("reason")
        );

        DiscrepancyCase result = processDiscrepancyUseCase.checkerApprove(command);
        return ResponseEntity.ok(mapToResponse(result));
    }

    @PostMapping("/discrepancy/checker-reject")
    public ResponseEntity<Map<String, Object>> checkerReject(@RequestBody Map<String, Object> request) {
        ProcessDiscrepancyUseCase.CheckerCommand command = new ProcessDiscrepancyUseCase.CheckerCommand(
                (String) request.get("caseId"),
                (String) request.get("userId"),
                (String) request.get("reason")
        );

        DiscrepancyCase result = processDiscrepancyUseCase.checkerReject(command);
        return ResponseEntity.ok(mapToResponse(result));
    }

    private Map<String, Object> mapToResponse(DiscrepancyCase d) {
        return Map.of(
                "caseId", d.caseId().toString(),
                "transactionId", d.transactionId(),
                "discrepancyType", d.discrepancyType().name(),
                "internalAmount", d.internalAmount() != null ? d.internalAmount() : "null",
                "networkAmount", d.networkAmount() != null ? d.networkAmount() : "null",
                "status", d.status().name(),
                "createdAt", d.createdAt().toString()
        );
    }
}
