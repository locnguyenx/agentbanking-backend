package com.agentbanking.biller.infrastructure.web;

import com.agentbanking.biller.domain.port.in.ProcessEWalletUseCase;
import com.agentbanking.biller.domain.port.in.ProcessEsspUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/ewallet")
public class EWalletController {

    private final ProcessEWalletUseCase processEWalletUseCase;

    public EWalletController(ProcessEWalletUseCase processEWalletUseCase) {
        this.processEWalletUseCase = processEWalletUseCase;
    }

    @PostMapping("/withdrawal")
    public ResponseEntity<Map<String, Object>> withdrawal(@RequestBody Map<String, Object> request) {
        try {
            String walletProvider = (String) request.get("walletProvider");
            String walletId = (String) request.get("walletId");
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            UUID internalTxId = UUID.fromString((String) request.get("internalTransactionId"));
            boolean isWithdrawal = true;

            ProcessEWalletUseCase.EWalletTransactionResult result = processEWalletUseCase.processEWalletTransaction(
                    new ProcessEWalletUseCase.EWalletTransactionCommand(
                            walletProvider,
                            walletId,
                            amount,
                            internalTxId,
                            isWithdrawal
                    )
            );

            return ResponseEntity.ok(Map.of(
                    "status", result.status(),
                    "transactionId", result.transactionId(),
                    "walletReference", result.walletReference()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "error", Map.of("code", "ERR_EWALLET_TRANSACTION_FAILED", "message", e.getMessage())
            ));
        }
    }

    @PostMapping("/topup")
    public ResponseEntity<Map<String, Object>> topup(@RequestBody Map<String, Object> request) {
        try {
            String walletProvider = (String) request.get("walletProvider");
            String walletId = (String) request.get("walletId");
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            UUID internalTxId = UUID.fromString((String) request.get("internalTransactionId"));
            boolean isWithdrawal = false;

            ProcessEWalletUseCase.EWalletTransactionResult result = processEWalletUseCase.processEWalletTransaction(
                    new ProcessEWalletUseCase.EWalletTransactionCommand(
                            walletProvider,
                            walletId,
                            amount,
                            internalTxId,
                            isWithdrawal
                    )
            );

            return ResponseEntity.ok(Map.of(
                    "status", result.status(),
                    "transactionId", result.transactionId(),
                    "walletReference", result.walletReference()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "error", Map.of("code", "ERR_EWALLET_TRANSACTION_FAILED", "message", e.getMessage())
            ));
        }
    }
}