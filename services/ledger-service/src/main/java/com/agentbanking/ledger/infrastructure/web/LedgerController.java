package com.agentbanking.ledger.infrastructure.web;

import com.agentbanking.ledger.domain.model.TransactionRecord;
import com.agentbanking.ledger.domain.model.TransactionStatus;
import com.agentbanking.ledger.domain.model.TransactionType;
import com.agentbanking.ledger.domain.port.in.*;
import com.agentbanking.ledger.infrastructure.web.dto.BalanceInquiryRequest;
import com.agentbanking.ledger.infrastructure.web.dto.DepositRequest;
import com.agentbanking.ledger.infrastructure.web.dto.WithdrawalRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal")
public class LedgerController {

    private final ProcessWithdrawalUseCase processWithdrawalUseCase;
    private final ProcessDepositUseCase processDepositUseCase;
    private final GetBalanceUseCase getBalanceUseCase;
    private final ReverseTransactionUseCase reverseTransactionUseCase;
    private final TransactionQueryUseCase transactionQueryUseCase;
    private final CustomerBalanceInquiryUseCase customerBalanceInquiryUseCase;

    @org.springframework.beans.factory.annotation.Autowired
    public LedgerController(@org.springframework.beans.factory.annotation.Qualifier("processWithdrawalUseCaseImpl") ProcessWithdrawalUseCase processWithdrawalUseCase,
                            @org.springframework.beans.factory.annotation.Qualifier("processDepositUseCaseImpl") ProcessDepositUseCase processDepositUseCase,
                            GetBalanceUseCase getBalanceUseCase,
                            ReverseTransactionUseCase reverseTransactionUseCase,
                            TransactionQueryUseCase transactionQueryUseCase,
                            CustomerBalanceInquiryUseCase customerBalanceInquiryUseCase) {
        this.processWithdrawalUseCase = processWithdrawalUseCase;
        this.processDepositUseCase = processDepositUseCase;
        this.getBalanceUseCase = getBalanceUseCase;
        this.reverseTransactionUseCase = reverseTransactionUseCase;
        this.transactionQueryUseCase = transactionQueryUseCase;
        this.customerBalanceInquiryUseCase = customerBalanceInquiryUseCase;
    }

    @PostMapping("/debit")
    public ResponseEntity<Map<String, Object>> debit(@Valid @RequestBody WithdrawalRequest request) {
        try {
            Map<String, Object> result = new HashMap<>(processWithdrawalUseCase.processWithdrawal(
                request.agentId(),
                request.amount(),
                request.customerFee(),
                request.agentCommission(),
                request.bankShare(),
                request.idempotencyKey(),
                request.customerCardMasked(),
                request.geofenceLat(),
                request.geofenceLng(),
                request.agentTier(),
                request.targetBin()
            ));

            result.put("balance", getBalanceUseCase.getBalance(request.agentId()));
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
        } catch (com.agentbanking.common.exception.LedgerException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of(
                    "code", e.getErrorCode() != null ? e.getErrorCode() : "ERR_BIZ_LEDGER_ERROR",
                    "message", e.getMessage() != null ? e.getMessage() : "Business error occurred",
                    "action_code", e.getActionCode() != null ? e.getActionCode() : "DECLINE"
                )
            ));
        }
    }

    @PostMapping("/credit")
    public ResponseEntity<Map<String, Object>> credit(@Valid @RequestBody DepositRequest request) {
        try {
            Map<String, Object> result = new HashMap<>(processDepositUseCase.processDeposit(
                request.agentId(),
                request.amount(),
                request.customerFee(),
                request.agentCommission(),
                request.bankShare(),
                request.idempotencyKey(),
                request.destinationAccount(),
                request.agentTier(),
                request.targetBin(),
                request.referenceNumber(),
                request.geofenceLat(),
                request.geofenceLng()
            ));

            result.put("balance", getBalanceUseCase.getBalance(request.agentId()));
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
            BigDecimal balance = getBalanceUseCase.getBalance(agentId);
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

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalanceByQuery(@RequestParam UUID agentId) {
        try {
            BigDecimal balance = getBalanceUseCase.getBalance(agentId);
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
        return ResponseEntity.ok(reverseTransactionUseCase.reverseTransaction(transactionId));
    }

    @GetMapping("/backoffice/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        long totalTransactions = transactionQueryUseCase.countAllTransactions();
        BigDecimal totalVolume = transactionQueryUseCase.sumSuccessfulTransactionAmount();
        long activeAgents = transactionQueryUseCase.countDistinctAgents();
        
        BigDecimal totalDebits = transactionQueryUseCase.sumSuccessfulTransactionAmountByType(TransactionType.CASH_WITHDRAWAL);
        BigDecimal totalCredits = transactionQueryUseCase.sumSuccessfulTransactionAmountByType(TransactionType.CASH_DEPOSIT);
        
        return ResponseEntity.ok(Map.of(
            "totalAgents", activeAgents,
            "activeAgents", activeAgents,
            "totalTransactions", totalTransactions,
            "totalVolume", totalVolume,
            "totalDebits", totalDebits,
            "totalCredits", totalCredits,
            "pendingKyc", 0
        ));
    }

    @GetMapping("/backoffice/agents")
    public ResponseEntity<Map<String, Object>> getAgents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<TransactionRecord> transactions = transactionQueryUseCase.findRecentTransactions();
        List<Map<String, Object>> agentList = transactions.stream()
            .limit(20)
            .map(t -> {
                Map<String, Object> item = new HashMap<>();
                item.put("agentId", t.agentId().toString());
                item.put("transactionType", t.transactionType());
                item.put("amount", t.amount());
                item.put("status", t.status());
                item.put("createdAt", t.createdAt().toString());
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
        
        List<TransactionRecord> transactions = transactionQueryUseCase.findRecentTransactions();
        List<Map<String, Object>> content = transactions.stream()
            .skip((long) page * size)
            .limit(size)
            .map(this::mapToTransactionResponse)
            .toList();
        
        return ResponseEntity.ok(Map.of(
            "content", content,
            "totalElements", transactions.size(),
            "totalPages", (int) Math.ceil((double) transactions.size() / size),
            "page", page,
            "size", size
        ));
    }

    @GetMapping("/internal/transactions/{transactionId}")
    public ResponseEntity<Map<String, Object>> getTransaction(@PathVariable UUID transactionId) {
        TransactionRecord t = transactionQueryUseCase.findById(transactionId);
        if (t == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapToTransactionResponse(t));
    }

    private Map<String, Object> mapToTransactionResponse(TransactionRecord t) {
        Map<String, Object> item = new HashMap<>();
        item.put("transactionId", t.transactionId() != null ? t.transactionId().toString() : null);
        item.put("agentId", t.agentId() != null ? t.agentId().toString() : null);
        item.put("transactionType", t.transactionType() != null ? t.transactionType().name() : null);
        item.put("amount", t.amount());
        item.put("customerFee", t.customerFee());
        item.put("status", t.status() != null ? t.status().name() : null);
        item.put("customerCardMasked", t.customerCardMasked());
        item.put("referenceNumber", t.referenceNumber());
        item.put("geofenceLat", t.geofenceLat());
        item.put("geofenceLng", t.geofenceLng());
        item.put("agentTier", t.agentTier());
        item.put("targetBin", t.targetBin());
        item.put("billerCode", t.billerCode());
        item.put("ref1", t.ref1());
        item.put("ref2", t.ref2());
        item.put("destinationAccount", t.destinationAccount());
        item.put("createdAt", t.createdAt() != null ? t.createdAt().toString() : null);
        if (t.completedAt() != null) {
            item.put("completedAt", t.completedAt().toString());
        }
        return item;
    }

    @GetMapping("/backoffice/settlement")
    public ResponseEntity<Map<String, Object>> getSettlement(
            @RequestParam String date) {
        
        LocalDate settlementDate = LocalDate.parse(date);
        LocalDateTime startOfDay = settlementDate.atStartOfDay();
        LocalDateTime endOfDay = settlementDate.plusDays(1).atStartOfDay();
        
        List<TransactionRecord> transactions = transactionQueryUseCase.findRecentTransactions();
        
        BigDecimal totalDeposits = transactions.stream()
            .filter(t -> TransactionType.CASH_DEPOSIT.equals(t.transactionType()))
            .filter(t -> t.createdAt() != null && t.createdAt().isAfter(startOfDay) && t.createdAt().isBefore(endOfDay))
            .map(TransactionRecord::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalWithdrawals = transactions.stream()
            .filter(t -> TransactionType.CASH_WITHDRAWAL.equals(t.transactionType()))
            .filter(t -> t.createdAt() != null && t.createdAt().isAfter(startOfDay) && t.createdAt().isBefore(endOfDay))
            .map(TransactionRecord::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return ResponseEntity.ok(Map.of(
            "date", date,
            "totalCredits", totalDeposits,
            "totalDebits", totalWithdrawals,
            "totalCommissions", BigDecimal.ZERO,
            "netAmount", totalDeposits.subtract(totalWithdrawals),
            "transactions", transactions.stream()
                .filter(t -> t.createdAt().isAfter(startOfDay) && t.createdAt().isBefore(endOfDay))
                .map(t -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("transactionId", t.transactionId().toString());
                    item.put("agentId", t.agentId().toString());
                    item.put("transactionType", t.transactionType());
                    item.put("amount", t.amount());
                    item.put("status", t.status());
                    return item;
                })
                .toList()
        ));
    }

    @GetMapping("/transactions/has-pending")
    public ResponseEntity<Map<String, Object>> hasPendingTransactions(@RequestParam UUID agentId) {
        List<TransactionStatus> pendingStatuses = List.of(TransactionStatus.PENDING);
        boolean hasPending = transactionQueryUseCase.existsByAgentIdAndStatusIn(agentId, pendingStatuses);
        return ResponseEntity.ok(Map.of("hasPending", hasPending));
    }

    @GetMapping("/transactions/count-by-status")
    public ResponseEntity<Map<String, Object>> getTransactionsCount(@RequestParam UUID agentId, @RequestParam TransactionStatus status) {
        long count = transactionQueryUseCase.countByAgentIdAndStatus(agentId, status);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/transactions/exists-by-status")
    public ResponseEntity<Map<String, Object>> existsByAgentIdAndStatusIn(
            @RequestParam UUID agentId,
            @RequestParam List<TransactionStatus> statuses) {
        boolean exists = transactionQueryUseCase.existsByAgentIdAndStatusIn(agentId, statuses);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/backoffice/settlement/export")
    public ResponseEntity<byte[]> exportSettlement(@RequestParam String date) {
        LocalDate settlementDate = LocalDate.parse(date);
        LocalDateTime startOfDay = settlementDate.atStartOfDay();
        LocalDateTime endOfDay = settlementDate.plusDays(1).atStartOfDay();
        
        List<TransactionRecord> transactions = transactionQueryUseCase.findRecentTransactions();
        var dailyTransactions = transactions.stream()
            .filter(t -> t.createdAt().isAfter(startOfDay) && t.createdAt().isBefore(endOfDay))
            .collect(Collectors.toList());
        
        BigDecimal totalDeposits = dailyTransactions.stream()
            .filter(t -> "DEPOSIT".equals(t.transactionType()))
            .map(TransactionRecord::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalWithdrawals = dailyTransactions.stream()
            .filter(t -> "WITHDRAWAL".equals(t.transactionType()))
            .map(TransactionRecord::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        StringBuilder csv = new StringBuilder();
        csv.append("Date,AgentId,TransactionType,Amount,Status\n");
        for (TransactionRecord t : dailyTransactions) {
            csv.append(String.format("%s,%s,%s,%s,%s\n",
                date,
                t.agentId(),
                t.transactionType(),
                t.amount(),
                t.status()));
        }
        
        return ResponseEntity.ok()
            .header("Content-Type", "text/csv")
            .header("Content-Disposition", "attachment; filename=settlement-" + date + ".csv")
            .body(csv.toString().getBytes());
    }

    @PostMapping("/balance-inquiry")
    public ResponseEntity<Map<String, Object>> balanceInquiry(@Valid @RequestBody BalanceInquiryRequest request) {
        try {
            var result = customerBalanceInquiryUseCase.inquire(
                new CustomerBalanceInquiryUseCase.CustomerInquiryCommand(
                    request.encryptedCardData(),
                    request.pinBlock()
                )
            );
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "balance", result.balance(),
                "currency", result.currency(),
                "accountMasked", result.accountMasked()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_INVALID_CARD", "message", e.getMessage())
            ));
        }
    }
}