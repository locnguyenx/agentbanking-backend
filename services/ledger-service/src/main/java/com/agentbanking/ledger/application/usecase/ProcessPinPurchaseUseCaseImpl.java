package com.agentbanking.ledger.application.usecase;

import com.agentbanking.ledger.domain.port.in.ProcessPinPurchaseUseCase;
import com.agentbanking.ledger.domain.port.out.IdempotencyCache;
import com.agentbanking.ledger.domain.service.LedgerService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of ProcessPinPurchaseUseCase for processing PIN voucher purchases
 */
@Component
public class ProcessPinPurchaseUseCaseImpl implements ProcessPinPurchaseUseCase {

    private final LedgerService ledgerService;
    private final IdempotencyCache idempotencyCache;

    public ProcessPinPurchaseUseCaseImpl(LedgerService ledgerService,
                                          IdempotencyCache idempotencyCache) {
        this.ledgerService = ledgerService;
        this.idempotencyCache = idempotencyCache;
    }

    @Override
    public PinPurchaseResponse processPinPurchase(PinPurchaseCommand command) {
        String idempotencyKey = command.idempotencyKey();
        if (idempotencyKey != null && idempotencyCache.exists(idempotencyKey)) {
            try {
                return idempotencyCache.get(idempotencyKey, PinPurchaseResponse.class);
            } catch (Exception e) {
                // Fall through if cache retrieval fails
            }
        }

        Map<String, Object> result = ledgerService.processPinPurchase(
                command.agentId(),
                command.productCode(),
                command.amount(),
                idempotencyKey
        );

        PinPurchaseResponse response = new PinPurchaseResponse(
                (String) result.get("status"),
                (String) result.get("transactionId"),
                (String) result.get("pinCode"),
                (BigDecimal) result.get("commission"),
                LocalDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        );

        if (idempotencyKey != null) {
            idempotencyCache.save(idempotencyKey, response, java.time.Duration.ofHours(24));
        }

        return response;
    }
}
