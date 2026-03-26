package com.agentbanking.ledger.application.usecase;

import com.agentbanking.ledger.domain.model.TransactionRecord;
import com.agentbanking.ledger.domain.port.in.TransactionQueryUseCase;
import com.agentbanking.ledger.domain.port.out.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionQueryUseCaseImpl implements TransactionQueryUseCase {

    private final TransactionRepository transactionRepository;

    public TransactionQueryUseCaseImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionRecord> findRecentTransactions() {
        return transactionRepository.findRecentTransactions();
    }

    @Transactional(readOnly = true)
    public BigDecimal sumSuccessfulTransactionAmount() {
        return transactionRepository.sumSuccessfulTransactionAmount();
    }

    @Transactional(readOnly = true)
    public long countAllTransactions() {
        return transactionRepository.countAllTransactions();
    }

    @Transactional(readOnly = true)
    public long countDistinctAgents() {
        return transactionRepository.countDistinctAgents();
    }
}