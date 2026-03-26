package com.agentbanking.ledger.infrastructure.web;

import com.agentbanking.ledger.domain.service.LedgerService;
import com.agentbanking.ledger.infrastructure.persistence.entity.TransactionEntity;
import com.agentbanking.ledger.infrastructure.persistence.repository.TransactionJpaRepository;
import com.agentbanking.ledger.infrastructure.web.dto.DepositRequest;
import com.agentbanking.ledger.infrastructure.web.dto.WithdrawalRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal")
public class LedgerController {

    private final LedgerService ledgerService;
    private final TransactionJpaRepository transactionRepository;

    public LedgerController(LedgerService ledgerService, TransactionJpaRepository transactionRepository) {
        this.ledgerService = ledgerService;
        this.transactionRepository = transactionRepository;
    }

    @PostMapping("/debit")
    public ResponseEntity<Map<String, Object>> debit(@RequestBody WithdrawalRequest request) {
        try {
            Map<String, Object> result = ledgerService.processWithdrawal(
                request.agentId(),
                request.amount(),
                request.customerFee(),
                request.agentCommission(),
                request.bankShare(),
                request.idempotencyKey(),
                request.customerCardMasked()
            );

            result.put("balance", ledgerService.getBalance(request.agentId()));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_SYS_AGENT_FLOAT_NOT_FOUND", "message", e.getMessage())
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_BIZ_INSUFFICIENT_FLOAT", "message", e.getMessage())
            ));
        }
    }

    @PostMapping("/credit")
    public ResponseEntity<Map<String, Object>> credit(@RequestBody DepositRequest request) {
        try {
            Map<String, Object> result = ledgerService.processDeposit(
                request.agentId(),
                request.amount(),
                request.customerFee(),
                request.agentCommission(),
                request.bankShare(),
                request.idempotencyKey(),
                request.destinationAccount()
            );

            result.put("balance", ledgerService.getBalance(request.agentId()));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_SYS_AGENT_FLOAT_NOT_FOUND", "message", e.getMessage())
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
                "error", Map.of("code", "ERR_SYS_AGENT_FLOAT_NOT_FOUND", "message", e.getMessage())
            ));
        }
    }

    @PostMapping("/reverse/{transactionId}")
    public ResponseEntity<Map<String, Object>> reverse(@PathVariable UUID transactionId) {
        return ResponseEntity.ok(Map.of(
            "status", "REVERSED",
            "transactionId", transactionId.toString()
        ));
    }

    @GetMapping("/backoffice/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        long totalTransactions = transactionRepository.countAllTransactions();
        BigDecimal totalVolume = transactionRepository.sumSuccessfulTransactionAmount();
        long activeAgents = transactionRepository.countDistinctAgents();
        
        return ResponseEntity.ok(Map.of(
            "totalAgents", activeAgents,
            "activeAgents", activeAgents,
            "totalTransactions", totalTransactions,
            "totalVolume", totalVolume,
            "pendingKyc", 0
        ));
    }

    @GetMapping("/backoffice/agents")
    public ResponseEntity<Map<String, Object>> getAgents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<TransactionEntity> transactions = transactionRepository.findRecentTransactions();
        List<Map<String, Object>> agentList = transactions.stream()
            .limit(20)
            .map(t -> {
                Map<String, Object> item = new HashMap<>();
                item.put("agentId", t.getAgentId().toString());
                item.put("transactionType", t.getTransactionType());
                item.put("amount", t.getAmount());
                item.put("status", t.getStatus());
                item.put("createdAt", t.getCreatedAt().toString());
                return item;
            })
            .toList();
        
        return ResponseEntity.ok(Map.of(
            "content", agentList,
            "totalElements", transactions.size(),
            "totalPages", 1,
            "page", page,
            "size", size
        ));
    }

    @GetMapping("/backoffice/transactions")
    public ResponseEntity<Map<String, Object>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<TransactionEntity> transactions = transactionRepository.findRecentTransactions();
        List<Map<String, Object>> content = transactions.stream()
            .skip((long) page * size)
            .limit(size)
            .map(t -> {
                Map<String, Object> item = new HashMap<>();
                item.put("transactionId", t.getTransactionId().toString());
                item.put("agentId", t.getAgentId().toString());
                item.put("transactionType", t.getTransactionType());
                item.put("amount", t.getAmount());
                item.put("status", t.getStatus());
                item.put("customerCardMasked", t.getCustomerCardMasked());
                item.put("createdAt", t.getCreatedAt().toString());
                return item;
            })
            .toList();
        
        return ResponseEntity.ok(Map.of(
            "content", content,
            "totalElements", transactions.size(),
            "totalPages", (int) Math.ceil((double) transactions.size() / size),
            "page", page,
            "size", size
        ));
    }

    @GetMapping("/backoffice/settlement")
    public ResponseEntity<Map<String, Object>> getSettlement(
            @RequestParam String date) {
        
        LocalDate settlementDate = LocalDate.parse(date);
        LocalDateTime startOfDay = settlementDate.atStartOfDay();
        LocalDateTime endOfDay = settlementDate.plusDays(1).atStartOfDay();
        
        List<TransactionEntity> transactions = transactionRepository.findRecentTransactions();
        
        BigDecimal totalDeposits = transactions.stream()
            .filter(t -> "DEPOSIT".equals(t.getTransactionType()))
            .filter(t -> t.getCreatedAt().isAfter(startOfDay) && t.getCreatedAt().isBefore(endOfDay))
            .map(TransactionEntity::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalWithdrawals = transactions.stream()
            .filter(t -> "WITHDRAWAL".equals(t.getTransactionType()))
            .filter(t -> t.getCreatedAt().isAfter(startOfDay) && t.getCreatedAt().isBefore(endOfDay))
            .map(TransactionEntity::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return ResponseEntity.ok(Map.of(
            "date", date,
            "totalDebits", totalWithdrawals,
            "totalCredits", totalDeposits,
            "netAmount", totalDeposits.subtract(totalWithdrawals),
            "transactions", transactions.stream()
                .filter(t -> t.getCreatedAt().isAfter(startOfDay) && t.getCreatedAt().isBefore(endOfDay))
                .map(t -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("transactionId", t.getTransactionId().toString());
                    item.put("agentId", t.getAgentId().toString());
                    item.put("transactionType", t.getTransactionType());
                    item.put("amount", t.getAmount());
                    item.put("status", t.getStatus());
                    return item;
                })
                .toList()
        ));
    }
}
