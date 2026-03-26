package com.agentbanking.ledger.application.usecase;

import com.agentbanking.ledger.domain.port.in.ReverseTransactionUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class ReverseTransactionUseCaseImpl implements ReverseTransactionUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReverseTransactionUseCaseImpl.class);

    @Override
    @Transactional
    public Map<String, Object> reverseTransaction(UUID transactionId) {
        log.info("Reversing transaction: {}", transactionId);
        return Map.of(
            "status", "REVERSED",
            "transactionId", transactionId.toString()
        );
    }
}