package com.agentbanking.ledger.application.usecase;

import com.agentbanking.ledger.domain.port.in.ReverseTransactionUseCase;
import com.agentbanking.ledger.infrastructure.messaging.ReversalEvent;
import com.agentbanking.ledger.infrastructure.messaging.ReversalEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class ReverseTransactionUseCaseImpl implements ReverseTransactionUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReverseTransactionUseCaseImpl.class);

    private final ReversalEventPublisher reversalEventPublisher;

    public ReverseTransactionUseCaseImpl(ReversalEventPublisher reversalEventPublisher) {
        this.reversalEventPublisher = reversalEventPublisher;
    }

    @Override
    @Transactional
    public Map<String, Object> reverseTransaction(UUID transactionId) {
        log.info("Reversing transaction: {}", transactionId);

        UUID reversalTransactionId = UUID.randomUUID();

        reversalEventPublisher.publish(new ReversalEvent(
            UUID.randomUUID(),
            "TRANSACTION_REVERSAL",
            transactionId,
            reversalTransactionId,
            null,
            null,
            "MYR",
            null,
            "SYSTEM_REVERSAL",
            LocalDateTime.now()
        ));

        return Map.of(
            "status", "REVERSED",
            "transactionId", transactionId.toString()
        );
    }
}
