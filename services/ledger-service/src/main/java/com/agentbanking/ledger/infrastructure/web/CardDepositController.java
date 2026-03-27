package com.agentbanking.ledger.infrastructure.web;

import com.agentbanking.ledger.domain.port.in.ProcessMyKadWithdrawalUseCase;
import com.agentbanking.ledger.domain.port.in.ProcessCardDepositUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/ledger/card")
public class CardDepositController {

    private final ProcessCardDepositUseCase processCardDepositUseCase;

    public CardDepositController(ProcessCardDepositUseCase processCardDepositUseCase) {
        this.processCardDepositUseCase = processCardDepositUseCase;
    }

    @PostMapping("/deposit")
    public ResponseEntity<Map<String, Object>> deposit(@RequestBody Map<String, Object> request) {
        try {
            UUID agentId = UUID.fromString((String) request.get("agentId"));
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String currency = (String) request.get("currency");
            String idempotencyKey = (String) request.get("idempotencyKey");
            String customerCardData = (String) request.get("customerCardData");
            String customerPinBlock = (String) request.get("customerPinBlock");

            ProcessCardDepositUseCase.TransactionResult result = processCardDepositUseCase.processCardDeposit(
                    new ProcessCardDepositUseCase.CardDepositCommand(
                            agentId,
                            amount,
                            currency,
                            idempotencyKey,
                            customerCardData,
                            customerPinBlock
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
                    "error", Map.of("code", "ERR_CARD_DEPOSIT_FAILED", "message", e.getMessage())
            ));
        }
    }
}