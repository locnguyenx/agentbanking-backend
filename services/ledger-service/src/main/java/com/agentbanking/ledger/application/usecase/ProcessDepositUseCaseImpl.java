package com.agentbanking.ledger.application.usecase;

import com.agentbanking.ledger.domain.port.in.ProcessDepositUseCase;
import com.agentbanking.ledger.infrastructure.messaging.TransactionEvent;
import com.agentbanking.ledger.infrastructure.messaging.TransactionEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@org.springframework.context.annotation.Primary
public class ProcessDepositUseCaseImpl implements ProcessDepositUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessDepositUseCaseImpl.class);

    private final com.agentbanking.ledger.domain.service.LedgerService ledgerService;
    private final TransactionEventPublisher transactionEventPublisher;

    public ProcessDepositUseCaseImpl(
            com.agentbanking.ledger.domain.service.LedgerService ledgerService,
            TransactionEventPublisher transactionEventPublisher) {
        this.ledgerService = ledgerService;
        this.transactionEventPublisher = transactionEventPublisher;
    }

    @Override
    @Transactional
    public Map<String, Object> processDeposit(UUID agentId, BigDecimal amount,
                                               BigDecimal customerFee, BigDecimal agentCommission,
                                               BigDecimal bankShare, String idempotencyKey,
                                               String customerMykad, String billerCode,
                                               String ref1, String ref2,
                                               BigDecimal geofenceLat, BigDecimal geofenceLng) {
        log.info("Processing deposit for agent: {}, amount: {}", agentId, amount);

        try {
            Map<String, Object> result = ledgerService.processDeposit(agentId, amount, customerFee, agentCommission,
                    bankShare, idempotencyKey, customerMykad, billerCode, ref1, ref2, geofenceLat, geofenceLng);

            if (result != null) {
                transactionEventPublisher.publish(new TransactionEvent(
                    UUID.randomUUID(),
                    String.valueOf(result.get("status")),
                    result.get("transactionId") instanceof UUID ? (UUID) result.get("transactionId") : UUID.fromString(String.valueOf(result.get("transactionId"))),
                    agentId,
                    "CASH_DEPOSIT",
                    amount,
                    "MYR",
                    null,
                    null
                ));
            }

            return result;
        } catch (Exception e) {
            transactionEventPublisher.publish(new TransactionEvent(
                UUID.randomUUID(),
                "FAILED",
                UUID.randomUUID(),
                agentId,
                "CASH_DEPOSIT",
                amount,
                "MYR",
                e instanceof com.agentbanking.common.exception.LedgerException le
                    ? le.getErrorCode() : e.getClass().getSimpleName(),
                null
            ));
            throw e;
        }
    }
}
