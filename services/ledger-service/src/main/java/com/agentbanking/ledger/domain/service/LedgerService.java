package com.agentbanking.ledger.domain.service;

import com.agentbanking.ledger.domain.model.AgentFloat;
import com.agentbanking.ledger.domain.model.Transaction;
import com.agentbanking.ledger.domain.model.TransactionStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class LedgerService {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public Transaction processWithdrawal(UUID agentId, BigDecimal amount, 
                                          BigDecimal customerFee, BigDecimal agentCommission,
                                          BigDecimal bankShare, String idempotencyKey,
                                          String customerCardMasked) {
        // Debit agent float with PESSIMISTIC_WRITE lock
        AgentFloat agentFloat = em.find(AgentFloat.class, agentId, LockModeType.PESSIMISTIC_WRITE);
        if (agentFloat == null) {
            throw new IllegalArgumentException("Agent float not found");
        }
        
        if (agentFloat.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient float balance");
        }

        // Debit float
        agentFloat.setBalance(agentFloat.getBalance().subtract(amount));
        agentFloat.setUpdatedAt(LocalDateTime.now());
        em.merge(agentFloat);

        // Create transaction
        Transaction txn = new Transaction();
        txn.setTransactionId(UUID.randomUUID());
        txn.setIdempotencyKey(idempotencyKey);
        txn.setAgentId(agentId);
        txn.setTransactionType(com.agentbanking.ledger.domain.model.TransactionType.CASH_WITHDRAWAL);
        txn.setAmount(amount);
        txn.setCustomerFee(customerFee);
        txn.setAgentCommission(agentCommission);
        txn.setBankShare(bankShare);
        txn.setStatus(TransactionStatus.COMPLETED);
        txn.setCustomerCardMasked(customerCardMasked);
        txn.setCreatedAt(LocalDateTime.now());
        txn.setCompletedAt(LocalDateTime.now());
        em.persist(txn);

        return txn;
    }

    @Transactional
    public Transaction processDeposit(UUID agentId, BigDecimal amount,
                                       BigDecimal customerFee, BigDecimal agentCommission,
                                       BigDecimal bankShare, String idempotencyKey,
                                       String destinationAccount) {
        // Credit agent float with PESSIMISTIC_WRITE lock
        AgentFloat agentFloat = em.find(AgentFloat.class, agentId, LockModeType.PESSIMISTIC_WRITE);
        if (agentFloat == null) {
            throw new IllegalArgumentException("Agent float not found");
        }

        // Credit float
        agentFloat.setBalance(agentFloat.getBalance().add(amount));
        agentFloat.setUpdatedAt(LocalDateTime.now());
        em.merge(agentFloat);

        // Create transaction
        Transaction txn = new Transaction();
        txn.setTransactionId(UUID.randomUUID());
        txn.setIdempotencyKey(idempotencyKey);
        txn.setAgentId(agentId);
        txn.setTransactionType(com.agentbanking.ledger.domain.model.TransactionType.CASH_DEPOSIT);
        txn.setAmount(amount);
        txn.setCustomerFee(customerFee);
        txn.setAgentCommission(agentCommission);
        txn.setBankShare(bankShare);
        txn.setStatus(TransactionStatus.COMPLETED);
        txn.setCreatedAt(LocalDateTime.now());
        txn.setCompletedAt(LocalDateTime.now());
        em.persist(txn);

        return txn;
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID agentId) {
        AgentFloat agentFloat = em.find(AgentFloat.class, agentId);
        if (agentFloat == null) {
            throw new IllegalArgumentException("Agent float not found");
        }
        return agentFloat.getBalance();
    }
}
