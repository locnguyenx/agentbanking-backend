package com.agentbanking.ledger.application.usecase;

import com.agentbanking.ledger.domain.model.TransactionRecord;
import com.agentbanking.ledger.domain.model.TransactionStatus;
import com.agentbanking.ledger.domain.model.TransactionType;
import com.agentbanking.ledger.domain.port.in.TransactionQueryUseCase;
import com.agentbanking.ledger.domain.port.out.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionQueryUseCaseImpl implements TransactionQueryUseCase {

    private final TransactionRepository transactionRepository;

    public TransactionQueryUseCaseImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionRecord findById(UUID transactionId) {
        return transactionRepository.findById(transactionId);
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

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumSuccessfulTransactionAmountByType(TransactionType type) {
        return transactionRepository.sumSuccessfulTransactionAmountByType(type);
    }

    @Transactional(readOnly = true)
    public long countAllTransactions() {
        return transactionRepository.countAllTransactions();
    }

    @Transactional(readOnly = true)
    public long countDistinctAgents() {
        return transactionRepository.countDistinctAgents();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByAgentIdAndStatus(UUID agentId, TransactionStatus status) {
        return transactionRepository.countByAgentIdAndStatus(agentId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByAgentIdAndStatusIn(UUID agentId, List<TransactionStatus> statuses) {
        return transactionRepository.existsByAgentIdAndStatusIn(agentId, statuses);
    }
}