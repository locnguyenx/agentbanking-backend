package com.agentbanking.biller.infrastructure.web;

import com.agentbanking.biller.domain.port.in.ProcessEsspUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/essp")
public class EsspController {

    private final ProcessEsspUseCase processEsspUseCase;

    public EsspController(ProcessEsspUseCase processEsspUseCase) {
        this.processEsspUseCase = processEsspUseCase;
    }

    @PostMapping("/purchase")
    public ResponseEntity<Map<String, Object>> purchase(@RequestBody Map<String, Object> request) {
        try {
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            UUID internalTxId = UUID.fromString((String) request.get("internalTransactionId"));

            ProcessEsspUseCase.EsspTransactionResult result = processEsspUseCase.processEsspPurchase(
                    new ProcessEsspUseCase.EsspTransactionCommand(
                            amount,
                            internalTxId
                    )
            );

            return ResponseEntity.ok(Map.of(
                    "status", result.status(),
                    "transactionId", result.transactionId(),
                    "esspCertificateNumber", result.esspCertificateNumber()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "error", Map.of("code", "ERR_ESSP_TRANSACTION_FAILED", "message", e.getMessage())
            ));
        }
    }
}