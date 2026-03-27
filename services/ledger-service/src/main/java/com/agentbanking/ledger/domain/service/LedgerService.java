package com.agentbanking.ledger.domain.service;

import com.agentbanking.common.transaction.TransactionStatus;
import com.agentbanking.ledger.domain.model.*;
import com.agentbanking.ledger.domain.port.out.*;
import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.common.exception.LedgerException;
import com.agentbanking.common.geofence.GeofenceChecker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LedgerService {
    
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);
    private static final String MYR_CURRENCY = "MYR";
    
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
    public Map<String, Object> processWithdrawal(UUID agentId, BigDecimal amount, 
                                                  BigDecimal customerFee, BigDecimal agentCommission,
                                                  BigDecimal bankShare, String idempotencyKey,
                                                  String customerCardMasked,
                                                  BigDecimal geofenceLat, BigDecimal geofenceLng) {
        if (idempotencyKey != null && idempotencyCache.exists(idempotencyKey)) {
            try {
                return idempotencyCache.get(idempotencyKey, Map.class);
            } catch (Exception e) {
            }
        }
        
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        customerFee = customerFee.setScale(2, RoundingMode.HALF_UP);
        agentCommission = agentCommission.setScale(2, RoundingMode.HALF_UP);
        bankShare = bankShare.setScale(2, RoundingMode.HALF_UP);
        
        AgentFloatRecord agentFloat = agentFloatRepository.findByIdWithLock(agentId);
        if (agentFloat == null) {
            throw new LedgerException(ErrorCodes.ERR_AGENT_FLOAT_NOT_FOUND, "RETRY");
        }
        
        if (!MYR_CURRENCY.equals(agentFloat.currency())) {
            throw new LedgerException(ErrorCodes.ERR_INVALID_CURRENCY, "DECLINE", 
                "Only MYR currency is supported, got: " + agentFloat.currency());
        }
        
        if (geofenceLat == null || geofenceLng == null) {
            throw new LedgerException(ErrorCodes.ERR_GPS_UNAVAILABLE, "DECLINE", 
                "GPS coordinates not provided");
        }
        
        if (agentFloat.merchantGpsLat() != null && agentFloat.merchantGpsLng() != null) {
            if (!GeofenceChecker.isWithinGeofence(
                    agentFloat.merchantGpsLat(), agentFloat.merchantGpsLng(),
                    geofenceLat, geofenceLng, 100.0)) {
                throw new LedgerException(ErrorCodes.ERR_GEOFENCE_VIOLATION, "DECLINE", 
                    "Transaction outside merchant location");
            }
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
            agentFloat.version(),
            agentFloat.merchantGpsLat(),
            agentFloat.merchantGpsLng()
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
    public Map<String, Object> processDeposit(UUID agentId, BigDecimal amount,
                                               BigDecimal customerFee, BigDecimal agentCommission,
                                               BigDecimal bankShare, String idempotencyKey,
                                               String destinationAccount) {
        if (idempotencyKey != null && idempotencyCache.exists(idempotencyKey)) {
            try {
                return idempotencyCache.get(idempotencyKey, Map.class);
            } catch (Exception e) {
            }
        }
        
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        customerFee = customerFee.setScale(2, RoundingMode.HALF_UP);
        agentCommission = agentCommission.setScale(2, RoundingMode.HALF_UP);
        bankShare = bankShare.setScale(2, RoundingMode.HALF_UP);
        
        AgentFloatRecord agentFloat = agentFloatRepository.findByIdWithLock(agentId);
        if (agentFloat == null) {
            throw new LedgerException(ErrorCodes.ERR_AGENT_FLOAT_NOT_FOUND, "RETRY");
        }
        
        if (!MYR_CURRENCY.equals(agentFloat.currency())) {
            throw new LedgerException(ErrorCodes.ERR_INVALID_CURRENCY, "DECLINE", 
                "Only MYR currency is supported, got: " + agentFloat.currency());
        }
        
        BigDecimal newBalance = agentFloat.balance().add(amount).setScale(2, RoundingMode.HALF_UP);
        AgentFloatRecord updatedFloat = new AgentFloatRecord(
            agentFloat.floatId(),
            agentFloat.agentId(),
            newBalance,
            agentFloat.reservedBalance(),
            agentFloat.currency(),
            agentFloat.version(),
            agentFloat.merchantGpsLat(),
            agentFloat.merchantGpsLng()
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
    
    public BigDecimal getBalance(UUID agentId) {
        AgentFloatRecord agentFloat = agentFloatRepository.findByIdWithLock(agentId);
        if (agentFloat == null) {
            throw new LedgerException(ErrorCodes.ERR_AGENT_FLOAT_NOT_FOUND, "RETRY");
        }
        
        if (!MYR_CURRENCY.equals(agentFloat.currency())) {
            throw new LedgerException(ErrorCodes.ERR_INVALID_CURRENCY, "DECLINE", 
                "Only MYR currency is supported, got: " + agentFloat.currency());
        }
        
        return agentFloat.balance();
    }
}
