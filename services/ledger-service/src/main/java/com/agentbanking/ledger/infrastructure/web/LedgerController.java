package com.agentbanking.ledger.infrastructure.web;

import com.agentbanking.ledger.domain.model.Transaction;
import com.agentbanking.ledger.domain.service.LedgerService;
import com.agentbanking.ledger.infrastructure.web.dto.DepositRequest;
import com.agentbanking.ledger.infrastructure.web.dto.WithdrawalRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping("/debit")
    public ResponseEntity<Map<String, Object>> debit(@RequestBody WithdrawalRequest request) {
        try {
            Transaction txn = ledgerService.processWithdrawal(
                request.agentId(),
                request.amount(),
                request.customerFee(),
                request.agentCommission(),
                request.bankShare(),
                request.idempotencyKey(),
                request.customerCardMasked()
            );

            return ResponseEntity.ok(Map.of(
                "status", "COMPLETED",
                "transactionId", txn.getTransactionId().toString(),
                "amount", txn.getAmount(),
                "balance", ledgerService.getBalance(request.agentId())
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_AGENT_FLOAT_NOT_FOUND", "message", e.getMessage())
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_INSUFFICIENT_FLOAT", "message", e.getMessage())
            ));
        }
    }

    @PostMapping("/credit")
    public ResponseEntity<Map<String, Object>> credit(@RequestBody DepositRequest request) {
        try {
            Transaction txn = ledgerService.processDeposit(
                request.agentId(),
                request.amount(),
                request.customerFee(),
                request.agentCommission(),
                request.bankShare(),
                request.idempotencyKey(),
                request.destinationAccount()
            );

            return ResponseEntity.ok(Map.of(
                "status", "COMPLETED",
                "transactionId", txn.getTransactionId().toString(),
                "amount", txn.getAmount(),
                "balance", ledgerService.getBalance(request.agentId())
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_AGENT_FLOAT_NOT_FOUND", "message", e.getMessage())
            ));
        }
    }

    @GetMapping("/balance/{agentId}")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable UUID agentId) {
        try {
            BigDecimal balance = ledgerService.getBalance(agentId);
            return ResponseEntity.ok(Map.of(
                "agentId", agentId.toString(),
                "balance", balance,
                "currency", "MYR"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_AGENT_FLOAT_NOT_FOUND", "message", e.getMessage())
            ));
        }
    }

    @PostMapping("/reverse/{transactionId}")
    public ResponseEntity<Map<String, Object>> reverse(@PathVariable UUID transactionId) {
        // Stub for reversal logic
        return ResponseEntity.ok(Map.of(
            "status", "REVERSED",
            "transactionId", transactionId.toString()
        ));
    }
}
