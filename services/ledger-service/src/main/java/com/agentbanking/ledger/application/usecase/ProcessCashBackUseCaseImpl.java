package com.agentbanking.ledger.application.usecase;

import com.agentbanking.ledger.domain.port.in.ProcessCashBackUseCase;
import com.agentbanking.ledger.domain.port.out.IdempotencyCache;
import com.agentbanking.ledger.domain.service.LedgerService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Implementation of ProcessCashBackUseCase for cash-back transactions
 */
@Component
public class ProcessCashBackUseCaseImpl implements ProcessCashBackUseCase {

    private final LedgerService ledgerService;
    private final IdempotencyCache idempotencyCache;

    public ProcessCashBackUseCaseImpl(LedgerService ledgerService,
                                       IdempotencyCache idempotencyCache) {
        this.ledgerService = ledgerService;
        this.idempotencyCache = idempotencyCache;
    }

    @Override
    public CashBackResponse processCashBack(CashBackCommand command) {
        String idempotencyKey = command.idempotencyKey();
        if (idempotencyKey != null && idempotencyCache.exists(idempotencyKey)) {
            try {
                return idempotencyCache.get(idempotencyKey, CashBackResponse.class);
            } catch (Exception e) {
                // Fall through if cache retrieval fails
            }
        }

        Map<String, Object> result = ledgerService.processCashBack(
                command.merchantId(),
                command.cashBackAmount(),
                command.cardData(),
                command.pinBlock(),
                idempotencyKey
        );

        CashBackResponse response = new CashBackResponse(
                (String) result.get("status"),
                (String) result.get("transactionId"),
                (BigDecimal) result.get("cashBackAmount"),
                (BigDecimal) result.get("commission")
        );

        if (idempotencyKey != null) {
            idempotencyCache.save(idempotencyKey, response, java.time.Duration.ofHours(24));
        }

        return response;
    }
}
