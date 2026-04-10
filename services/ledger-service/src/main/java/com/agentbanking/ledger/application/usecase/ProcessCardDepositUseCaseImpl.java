package com.agentbanking.ledger.application.usecase;

import com.agentbanking.ledger.domain.port.in.ProcessCardDepositUseCase;
import com.agentbanking.ledger.domain.port.out.IdempotencyCache;
import com.agentbanking.ledger.domain.service.LedgerService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of ProcessCardDepositUseCase for card-based cash deposits
 */
@Component
public class ProcessCardDepositUseCaseImpl implements ProcessCardDepositUseCase {

    private final LedgerService ledgerService;
    private final IdempotencyCache idempotencyCache;

    public ProcessCardDepositUseCaseImpl(LedgerService ledgerService,
                                           IdempotencyCache idempotencyCache) {
        this.ledgerService = ledgerService;
        this.idempotencyCache = idempotencyCache;
    }

    @Override
    public TransactionResult processCardDeposit(CardDepositCommand command) {
        String idempotencyKey = command.idempotencyKey();
        if (idempotencyKey != null && idempotencyCache.exists(idempotencyKey)) {
            try {
                return idempotencyCache.get(idempotencyKey, TransactionResult.class);
            } catch (Exception e) {
                // Fall through if cache retrieval fails
            }
        }

        // Use the standard deposit process from LedgerService
        Map<String, Object> result = ledgerService.processDeposit(
                command.agentId(),
                command.amount(),
                BigDecimal.ZERO, // customerFee
                BigDecimal.ZERO, // agentCommission
                BigDecimal.ZERO, // bankShare
                idempotencyKey,
                null, // customerMykad
                null, // billerCode
                null, // ref1
                null, // ref2
                null, // geofenceLat
                null  // geofenceLng
        );

        TransactionResult response = new TransactionResult(
                (String) result.get("status"),
                UUID.fromString((String) result.get("transactionId")),
                (BigDecimal) result.get("amount"),
                (BigDecimal) result.get("customerFee"),
                null // referenceNumber - not applicable for deposit
        );

        if (idempotencyKey != null) {
            idempotencyCache.save(idempotencyKey, response, java.time.Duration.ofHours(24));
        }

        return response;
    }
}
