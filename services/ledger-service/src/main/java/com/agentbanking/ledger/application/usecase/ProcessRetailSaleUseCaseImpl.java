package com.agentbanking.ledger.application.usecase;

import com.agentbanking.ledger.domain.model.MdrCalculation;
import com.agentbanking.ledger.domain.port.in.ProcessRetailSaleUseCase;
import com.agentbanking.ledger.domain.port.out.IdempotencyCache;
import com.agentbanking.ledger.domain.service.LedgerService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of ProcessRetailSaleUseCase for processing retail card/QR purchases
 */
@Component
public class ProcessRetailSaleUseCaseImpl implements ProcessRetailSaleUseCase {

    private final LedgerService ledgerService;
    private final IdempotencyCache idempotencyCache;

    public ProcessRetailSaleUseCaseImpl(LedgerService ledgerService,
                                         IdempotencyCache idempotencyCache) {
        this.ledgerService = ledgerService;
        this.idempotencyCache = idempotencyCache;
    }

    @Override
    public RetailSaleResponse processRetailSale(RetailSaleCommand command) {
        String idempotencyKey = command.idempotencyKey();
        if (idempotencyKey != null && idempotencyCache.exists(idempotencyKey)) {
            try {
                return idempotencyCache.get(idempotencyKey, RetailSaleResponse.class);
            } catch (Exception e) {
                // Fall through if cache retrieval fails
            }
        }

        Map<String, Object> result = ledgerService.processRetailSale(
                command.merchantId(),
                command.amount(),
                command.cardData(),
                command.pinBlock(),
                idempotencyKey,
                command.agentId(),
                command.description(),
                command.referenceNumber(),
                command.agentTier(),
                command.billerCode(),
                command.targetBin(),
                command.destinationAccount(),
                command.ref1(),
                command.ref2()
        );

        RetailSaleResponse response = new RetailSaleResponse(
                (String) result.get("status"),
                (String) result.get("transactionId"),
                (BigDecimal) result.get("amount"),
                (BigDecimal) result.get("mdrAmount"),
                (BigDecimal) result.get("netToMerchant")
        );

        if (idempotencyKey != null) {
            idempotencyCache.save(idempotencyKey, response, java.time.Duration.ofHours(24));
        }

        return response;
    }
}
