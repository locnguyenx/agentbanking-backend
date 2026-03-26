package com.agentbanking.ledger.application.usecase;

import com.agentbanking.ledger.domain.port.in.ProcessDepositUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class ProcessDepositUseCaseImpl implements ProcessDepositUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessDepositUseCaseImpl.class);

    private final com.agentbanking.ledger.domain.service.LedgerService ledgerService;

    public ProcessDepositUseCaseImpl(com.agentbanking.ledger.domain.service.LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @Override
    @Transactional
    public Map<String, Object> processDeposit(UUID agentId, BigDecimal amount,
                                               BigDecimal customerFee, BigDecimal agentCommission,
                                               BigDecimal bankShare, String idempotencyKey,
                                               String destinationAccount) {
        log.info("Processing deposit for agent: {}, amount: {}", agentId, amount);
        return ledgerService.processDeposit(agentId, amount, customerFee, agentCommission,
                bankShare, idempotencyKey, destinationAccount);
    }
}