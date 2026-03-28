package com.agentbanking.ledger.application.usecase;

import com.agentbanking.ledger.domain.port.in.ProcessMyKadWithdrawalUseCase;
import com.agentbanking.ledger.domain.port.out.IdempotencyCache;
import com.agentbanking.ledger.domain.service.LedgerService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
public class ProcessMyKadWithdrawalUseCaseImpl implements ProcessMyKadWithdrawalUseCase {

    private final LedgerService ledgerService;
    private final IdempotencyCache idempotencyCache;

    public ProcessMyKadWithdrawalUseCaseImpl(LedgerService ledgerService,
                                               IdempotencyCache idempotencyCache) {
        this.ledgerService = ledgerService;
        this.idempotencyCache = idempotencyCache;
    }

    @Override
    public TransactionResult processMyKadWithdrawal(MyKadWithdrawalCommand command) {
        String idempotencyKey = command.idempotencyKey();
        if (idempotencyKey != null && idempotencyCache.exists(idempotencyKey)) {
            try {
                return idempotencyCache.get(idempotencyKey, TransactionResult.class);
            } catch (Exception e) {
                // Fall through if cache retrieval fails
            }
        }

        Map<String, Object> result = ledgerService.processWithdrawal(
                command.agentId(),
                command.amount(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                idempotencyKey,
                null,
                command.geofenceLat(),
                command.geofenceLng()
        );

        TransactionResult response = new TransactionResult(
                (String) result.get("status"),
                UUID.fromString((String) result.get("transactionId")),
                (BigDecimal) result.get("amount"),
                BigDecimal.ZERO,
                null
        );

        if (idempotencyKey != null) {
            idempotencyCache.save(idempotencyKey, response, java.time.Duration.ofHours(24));
        }

        return response;
    }
}
