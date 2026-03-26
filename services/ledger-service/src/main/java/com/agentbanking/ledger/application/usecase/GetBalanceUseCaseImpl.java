package com.agentbanking.ledger.application.usecase;

import com.agentbanking.ledger.domain.port.in.GetBalanceUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class GetBalanceUseCaseImpl implements GetBalanceUseCase {

    private final com.agentbanking.ledger.domain.service.LedgerService ledgerService;

    public GetBalanceUseCaseImpl(com.agentbanking.ledger.domain.service.LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID agentId) {
        return ledgerService.getBalance(agentId);
    }
}