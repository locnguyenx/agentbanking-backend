package com.agentbanking.ledger.infrastructure.web;

import com.agentbanking.ledger.domain.port.in.ProcessMyKadWithdrawalUseCase;
import com.agentbanking.ledger.domain.port.in.ProcessCardDepositUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/ledger/my-kad")
public class MyKadWithdrawalController {

    private final ProcessMyKadWithdrawalUseCase processMyKadWithdrawalUseCase;

    public MyKadWithdrawalController(ProcessMyKadWithdrawalUseCase processMyKadWithdrawalUseCase) {
        this.processMyKadWithdrawalUseCase = processMyKadWithdrawalUseCase;
    }

    @PostMapping("/withdrawal")
    public ResponseEntity<Map<String, Object>> withdrawal(@RequestBody Map<String, Object> request) {
        try {
            UUID agentId = UUID.fromString((String) request.get("agentId"));
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String currency = (String) request.get("currency");
            String idempotencyKey = (String) request.get("idempotencyKey");
            String customerMykad = (String) request.get("customerMykad");
            BigDecimal geofenceLat = new BigDecimal(request.get("geofenceLat").toString());
            BigDecimal geofenceLng = new BigDecimal(request.get("geofenceLng").toString());

            ProcessMyKadWithdrawalUseCase.TransactionResult result = processMyKadWithdrawalUseCase.processMyKadWithdrawal(
                    new ProcessMyKadWithdrawalUseCase.MyKadWithdrawalCommand(
                            agentId,
                            amount,
                            currency,
                            idempotencyKey,
                            customerMykad,
                            geofenceLat,
                            geofenceLng
                    )
            );

            return ResponseEntity.ok(Map.of(
                    "status", result.status(),
                    "transactionId", result.transactionId().toString(),
                    "amount", result.amount(),
                    "customerFee", result.customerFee(),
                    "referenceNumber", result.referenceNumber()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "error", Map.of("code", "ERR_MYKAD_WITHDRAWAL_FAILED", "message", e.getMessage())
            ));
        }
    }
}