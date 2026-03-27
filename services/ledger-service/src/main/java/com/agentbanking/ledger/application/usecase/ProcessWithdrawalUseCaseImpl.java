package com.agentbanking.ledger.application.usecase;

import com.agentbanking.common.exception.LedgerException;
import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.ledger.domain.port.in.ProcessWithdrawalUseCase;
import com.agentbanking.ledger.infrastructure.external.RulesServiceFeignClient;
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
public class ProcessWithdrawalUseCaseImpl implements ProcessWithdrawalUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessWithdrawalUseCaseImpl.class);

    private final com.agentbanking.ledger.domain.service.LedgerService ledgerService;
    private final TransactionEventPublisher transactionEventPublisher;
    private final RulesServiceFeignClient rulesServiceFeignClient;

    public ProcessWithdrawalUseCaseImpl(
            com.agentbanking.ledger.domain.service.LedgerService ledgerService,
            TransactionEventPublisher transactionEventPublisher,
            RulesServiceFeignClient rulesServiceFeignClient) {
        this.ledgerService = ledgerService;
        this.transactionEventPublisher = transactionEventPublisher;
        this.rulesServiceFeignClient = rulesServiceFeignClient;
    }

    @Override
    @Transactional
    public Map<String, Object> processWithdrawal(UUID agentId, BigDecimal amount,
                                                  BigDecimal customerFee, BigDecimal agentCommission,
                                                  BigDecimal bankShare, String idempotencyKey,
                                                  String customerCardMasked,
                                                  BigDecimal geofenceLat, BigDecimal geofenceLng) {
        log.info("Processing withdrawal for agent: {}, amount: {}", agentId, amount);

        checkVelocity(agentId, amount);

        try {
            Map<String, Object> result = ledgerService.processWithdrawal(agentId, amount, customerFee, agentCommission,
                    bankShare, idempotencyKey, customerCardMasked, geofenceLat, geofenceLng);

            if (result != null) {
                transactionEventPublisher.publish(new TransactionEvent(
                    UUID.randomUUID(),
                    (String) result.get("status"),
                    UUID.fromString((String) result.get("transactionId")),
                    agentId,
                    "CASH_WITHDRAWAL",
                    amount,
                    "MYR",
                    null,
                    customerCardMasked
                ));
            }

            return result;
        } catch (Exception e) {
            transactionEventPublisher.publish(new TransactionEvent(
                UUID.randomUUID(),
                "FAILED",
                UUID.randomUUID(),
                agentId,
                "CASH_WITHDRAWAL",
                amount,
                "MYR",
                e instanceof com.agentbanking.common.exception.LedgerException le
                    ? le.getErrorCode() : e.getClass().getSimpleName(),
                customerCardMasked
            ));
            throw e;
        }
    }

    private void checkVelocity(UUID agentId, BigDecimal amount) {
        Map<String, Object> request = Map.of(
            "agentId", agentId.toString(),
            "transactionType", "CASH_WITHDRAWAL",
            "amount", amount
        );

        Map<String, Object> response = rulesServiceFeignClient.checkVelocity(request);
        Boolean passed = (Boolean) response.get("passed");

        if (passed == null || !passed) {
            String errorCode = (String) response.getOrDefault("errorCode", ErrorCodes.ERR_VELOCITY_COUNT_EXCEEDED);
            throw new LedgerException(errorCode, "DECLINE");
        }
    }
}
