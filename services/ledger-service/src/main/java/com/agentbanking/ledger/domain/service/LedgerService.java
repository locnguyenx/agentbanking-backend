package com.agentbanking.ledger.domain.service;

import com.agentbanking.ledger.domain.model.*;
import com.agentbanking.ledger.domain.port.out.*;
import com.agentbanking.common.security.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LedgerService {
    
    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);
    
    private final AgentFloatRepository agentFloatRepository;
    private final TransactionRepository transactionRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final IdempotencyCache idempotencyCache;
    
    public LedgerService(
            AgentFloatRepository agentFloatRepository,
            TransactionRepository transactionRepository,
            JournalEntryRepository journalEntryRepository,
            IdempotencyCache idempotencyCache) {
        this.agentFloatRepository = agentFloatRepository;
        this.transactionRepository = transactionRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.idempotencyCache = idempotencyCache;
    }
    
    @SuppressWarnings("unchecked")
    @Transactional
    public Map<String, Object> processWithdrawal(UUID agentId, BigDecimal amount, 
                                                  BigDecimal customerFee, BigDecimal agentCommission,
                                                  BigDecimal bankShare, String idempotencyKey,
                                                  String customerCardMasked) {
        if (idempotencyKey != null && idempotencyCache.exists(idempotencyKey)) {
            try {
                return idempotencyCache.get(idempotencyKey, Map.class);
            } catch (Exception e) {
                log.error("Failed to retrieve cached response for idempotency key: {}", idempotencyKey, e);
            }
        }
        
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        customerFee = customerFee.setScale(2, RoundingMode.HALF_UP);
        agentCommission = agentCommission.setScale(2, RoundingMode.HALF_UP);
        bankShare = bankShare.setScale(2, RoundingMode.HALF_UP);
        
        AgentFloatRecord agentFloat = agentFloatRepository.findByIdWithLock(agentId);
        if (agentFloat == null) {
            throw new IllegalArgumentException(ErrorCodes.ERR_AGENT_FLOAT_NOT_FOUND);
        }
        
        if (agentFloat.balance().compareTo(amount) < 0) {
            throw new IllegalStateException(ErrorCodes.ERR_INSUFFICIENT_FLOAT);
        }
        
        BigDecimal newBalance = agentFloat.balance().subtract(amount).setScale(2, RoundingMode.HALF_UP);
        AgentFloatRecord updatedFloat = new AgentFloatRecord(
            agentFloat.floatId(),
            agentFloat.agentId(),
            newBalance,
            agentFloat.reservedBalance(),
            agentFloat.currency(),
            agentFloat.version()
        );
        agentFloatRepository.save(updatedFloat);
        
        UUID transactionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        TransactionRecord txn = new TransactionRecord(
            transactionId,
            idempotencyKey,
            agentId,
            TransactionType.CASH_WITHDRAWAL,
            amount,
            customerFee,
            agentCommission,
            bankShare,
            TransactionStatus.COMPLETED,
            null,
            null,
            customerCardMasked,
            null,
            null,
            null,
            now,
            now
        );
        transactionRepository.save(txn);
        
        createJournalEntries(transactionId, agentId, amount, customerFee, agentCommission, bankShare, true);
        
        Map<String, Object> response = Map.of(
            "status", "COMPLETED",
            "transactionId", transactionId.toString(),
            "amount", amount,
            "balance", newBalance
        );
        
        if (idempotencyKey != null) {
            idempotencyCache.save(idempotencyKey, response, IDEMPOTENCY_TTL);
        }
        
        return response;
    }
    
    @SuppressWarnings("unchecked")
    @Transactional
    public Map<String, Object> processDeposit(UUID agentId, BigDecimal amount,
                                               BigDecimal customerFee, BigDecimal agentCommission,
                                               BigDecimal bankShare, String idempotencyKey,
                                               String destinationAccount) {
        if (idempotencyKey != null && idempotencyCache.exists(idempotencyKey)) {
            try {
                return idempotencyCache.get(idempotencyKey, Map.class);
            } catch (Exception e) {
                log.error("Failed to retrieve cached response for idempotency key: {}", idempotencyKey, e);
            }
        }
        
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        customerFee = customerFee.setScale(2, RoundingMode.HALF_UP);
        agentCommission = agentCommission.setScale(2, RoundingMode.HALF_UP);
        bankShare = bankShare.setScale(2, RoundingMode.HALF_UP);
        
        AgentFloatRecord agentFloat = agentFloatRepository.findByIdWithLock(agentId);
        if (agentFloat == null) {
            throw new IllegalArgumentException(ErrorCodes.ERR_AGENT_FLOAT_NOT_FOUND);
        }
        
        BigDecimal newBalance = agentFloat.balance().add(amount).setScale(2, RoundingMode.HALF_UP);
        AgentFloatRecord updatedFloat = new AgentFloatRecord(
            agentFloat.floatId(),
            agentFloat.agentId(),
            newBalance,
            agentFloat.reservedBalance(),
            agentFloat.currency(),
            agentFloat.version()
        );
        agentFloatRepository.save(updatedFloat);
        
        UUID transactionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        TransactionRecord txn = new TransactionRecord(
            transactionId,
            idempotencyKey,
            agentId,
            TransactionType.CASH_DEPOSIT,
            amount,
            customerFee,
            agentCommission,
            bankShare,
            TransactionStatus.COMPLETED,
            null,
            null,
            null,
            null,
            null,
            null,
            now,
            now
        );
        transactionRepository.save(txn);
        
        createJournalEntries(transactionId, agentId, amount, customerFee, agentCommission, bankShare, false);
        
        Map<String, Object> response = Map.of(
            "status", "COMPLETED",
            "transactionId", transactionId.toString(),
            "amount", amount,
            "balance", newBalance
        );
        
        if (idempotencyKey != null) {
            idempotencyCache.save(idempotencyKey, response, IDEMPOTENCY_TTL);
        }
        
        return response;
    }
    
    private void createJournalEntries(UUID transactionId, UUID agentId, BigDecimal amount,
                                       BigDecimal customerFee, BigDecimal agentCommission,
                                       BigDecimal bankShare, boolean isWithdrawal) {
        List<JournalEntryRecord> entries = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        if (isWithdrawal) {
            entries.add(new JournalEntryRecord(
                UUID.randomUUID(),
                transactionId,
                EntryType.DEBIT,
                "AGENT_FLOAT_" + agentId,
                amount,
                "Agent float debit for withdrawal",
                now
            ));
        } else {
            entries.add(new JournalEntryRecord(
                UUID.randomUUID(),
                transactionId,
                EntryType.CREDIT,
                "AGENT_FLOAT_" + agentId,
                amount,
                "Agent float credit for deposit",
                now
            ));
        }
        
        if (customerFee.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(new JournalEntryRecord(
                UUID.randomUUID(),
                transactionId,
                EntryType.CREDIT,
                "FEE_INCOME",
                customerFee,
                "Customer fee income",
                now
            ));
        }
        
        if (agentCommission.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(new JournalEntryRecord(
                UUID.randomUUID(),
                transactionId,
                EntryType.CREDIT,
                "AGENT_COMMISSION",
                agentCommission,
                "Agent commission payable",
                now
            ));
        }
        
        if (bankShare.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(new JournalEntryRecord(
                UUID.randomUUID(),
                transactionId,
                EntryType.CREDIT,
                "BANK_SHARE",
                bankShare,
                "Bank revenue share",
                now
            ));
        }
        
        journalEntryRepository.saveAll(entries);
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID agentId) {
        AgentFloatRecord agentFloat = agentFloatRepository.findByIdWithLock(agentId);
        if (agentFloat == null) {
            throw new IllegalArgumentException(ErrorCodes.ERR_AGENT_FLOAT_NOT_FOUND);
        }
        return agentFloat.balance();
    }
}
