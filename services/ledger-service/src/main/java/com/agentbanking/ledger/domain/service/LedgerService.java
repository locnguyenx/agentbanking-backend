package com.agentbanking.ledger.domain.service;

import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.common.exception.LedgerException;
import com.agentbanking.common.geofence.GeofenceChecker;
import com.agentbanking.common.efm.EfmEventPublisher;
import com.agentbanking.ledger.domain.model.*;
import com.agentbanking.ledger.domain.port.out.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class LedgerService {
     
      private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);
      private static final String MYR_CURRENCY = "MYR";
      private static final Logger log = LoggerFactory.getLogger(LedgerService.class);
      
      private static String safeGetString(Map<String, Object> map, String key) {
          Object value = map.get(key);
          return value != null ? value.toString() : null;
      }
     
      private final AgentFloatRepository agentFloatRepository;
      private final TransactionRepository transactionRepository;
      private final JournalEntryRepository journalEntryRepository;
      private final IdempotencyCache idempotencyCache;
      private final SwitchServicePort switchService;
      private final AgentRepository agentRepository;
      private final MerchantTransactionService merchantTransactionService;
      private final EfmEventPublisher efmEventPublisher;
      
      public LedgerService(
              AgentFloatRepository agentFloatRepository,
              TransactionRepository transactionRepository,
              JournalEntryRepository journalEntryRepository,
              IdempotencyCache idempotencyCache,
              SwitchServicePort switchService,
              AgentRepository agentRepository,
              MerchantTransactionService merchantTransactionService,
              EfmEventPublisher efmEventPublisher) {
          this.agentFloatRepository = agentFloatRepository;
          this.transactionRepository = transactionRepository;
          this.journalEntryRepository = journalEntryRepository;
          this.idempotencyCache = idempotencyCache;
          this.switchService = switchService;
          this.agentRepository = agentRepository;
          this.merchantTransactionService = merchantTransactionService;
          this.efmEventPublisher = efmEventPublisher;
      }
    
    @SuppressWarnings("unchecked")
    @Transactional
    public Map<String, Object> processWithdrawal(UUID agentId, BigDecimal amount, 
                                                   BigDecimal customerFee, BigDecimal agentCommission,
                                                   BigDecimal bankShare, String idempotencyKey,
                                                   String customerCardMasked,
                                                   BigDecimal geofenceLat, BigDecimal geofenceLng,
                                                   String agentTier, String targetBin) {
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
            efmEventPublisher.publishFraudAlert("GPS_UNAVAILABLE", null, agentId,
                "GPS coordinates not provided for withdrawal");
            throw new LedgerException(ErrorCodes.ERR_GPS_UNAVAILABLE, "DECLINE", 
                "GPS coordinates not provided");
        }
        
        if (agentFloat.merchantGpsLat() != null && agentFloat.merchantGpsLng() != null) {
            if (!GeofenceChecker.isWithinGeofence(
                    agentFloat.merchantGpsLat(), agentFloat.merchantGpsLng(),
                    geofenceLat, geofenceLng, 1.0)) {
                efmEventPublisher.publishFraudAlert("GEOFENCE_VIOLATION", null, agentId,
                    "Withdrawal initiated outside merchant geofence");
                throw new LedgerException(ErrorCodes.ERR_GEOFENCE_VIOLATION, "DECLINE", 
                    "Withdrawal must be performed within merchant location");
            }
        }

        if (agentFloat.balance().compareTo(amount) < 0) {
            throw new LedgerException(ErrorCodes.ERR_INSUFFICIENT_FLOAT, "DECLINE", 
                "Insufficient agent float balance");
        }
        
        var transaction = new TransactionRecord(
                null,
                idempotencyKey,
                agentId,
                TransactionType.CASH_WITHDRAWAL,
                amount,
                customerFee,
                agentCommission,
                bankShare,
                TransactionStatus.PENDING,
                null,
                null,
                customerCardMasked,
                null,
                null,
                geofenceLat,
                geofenceLng,
                LocalDateTime.now(),
                null,
                agentTier,
                targetBin,
                null, 
                null, 
                null, 
                null
        );
        
        transaction = transactionRepository.save(transaction);
        
        try {
            var switchResponse = switchService.debitAccount(agentId, amount, idempotencyKey);
            
            if ("00".equals(switchResponse.get("responseCode"))) {
                agentFloatRepository.updateBalance(agentId, amount.negate());

                var journalEntries = new ArrayList<JournalEntryRecord>();
                journalEntries.add(new JournalEntryRecord(null, transaction.transactionId(), EntryType.DEBIT, "AGENT_FLOAT", amount.negate(), "Withdrawal debit", LocalDateTime.now()));
                journalEntries.add(new JournalEntryRecord(null, transaction.transactionId(), EntryType.CREDIT, "CASH_SETTLEMENT", amount, "Withdrawal credit", LocalDateTime.now()));
                journalEntryRepository.saveAll(journalEntries);

                transaction = updateTransactionStatus(
                    transaction,
                    TransactionStatus.COMPLETED,
                    null,
                    safeGetString(switchResponse, "switchReference"),
                    safeGetString(switchResponse, "referenceNumber")
                );

                Map<String, Object> result = new HashMap<>();
                result.put("status", "COMPLETED");
                result.put("transactionId", transaction.transactionId());
                result.put("referenceNumber", transaction.referenceNumber());

                idempotencyCache.save(idempotencyKey, result, IDEMPOTENCY_TTL);
                return result;
            } else {
                String respCode = safeGetString(switchResponse, "responseCode");
                transaction = updateTransactionStatus(
                    transaction,
                    TransactionStatus.FAILED,
                    respCode,
                    safeGetString(switchResponse, "switchReference"),
                    null
                );

                throw new LedgerException(respCode != null ? respCode : "ERR_SWITCH_NO_RESPONSE", "DECLINE", "Withdrawal failed at switch");
            }
        } catch (Exception e) {
            log.error("Withdrawal error for workflow {}: {}", idempotencyKey, e.getMessage());
            if (!(e instanceof LedgerException)) {
                updateTransactionStatus(
                    transaction,
                    TransactionStatus.FAILED,
                    "ERR_SYS_SWITCH_ERROR",
                    null,
                    null
                );
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public Map<String, Object> processDeposit(UUID agentId, BigDecimal amount, 
                                               BigDecimal customerFee, BigDecimal agentCommission,
                                               BigDecimal bankShare, String idempotencyKey,
                                               String customerMykad, String billerCode, 
                                               String ref1, String ref2,
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
        
        if (geofenceLat == null || geofenceLng == null) {
            throw new LedgerException(ErrorCodes.ERR_GPS_UNAVAILABLE, "DECLINE", 
                "GPS coordinates not provided");
        }

        var transaction = new TransactionRecord(
                null,
                idempotencyKey,
                agentId,
                TransactionType.CASH_DEPOSIT,
                amount,
                customerFee,
                agentCommission,
                bankShare,
                TransactionStatus.PENDING,
                null,
                customerMykad,
                null,
                null,
                null,
                geofenceLat,
                geofenceLng,
                LocalDateTime.now(),
                null,
                null,
                null,
                billerCode,
                ref1,
                ref2,
                null
        );
        
        transaction = transactionRepository.save(transaction);
        
        try {
            var switchResponse = switchService.creditAccount(agentId, amount, idempotencyKey);
            
            if ("00".equals(switchResponse.get("responseCode"))) {
                agentFloatRepository.updateBalance(agentId, amount);

                var journalEntries = new ArrayList<JournalEntryRecord>();
                journalEntries.add(new JournalEntryRecord(null, transaction.transactionId(), EntryType.CREDIT, "AGENT_FLOAT", amount, "Deposit credit", LocalDateTime.now()));
                journalEntries.add(new JournalEntryRecord(null, transaction.transactionId(), EntryType.DEBIT, "CASH_SETTLEMENT", amount.negate(), "Deposit debit", LocalDateTime.now()));
                journalEntryRepository.saveAll(journalEntries);

                transaction = updateTransactionStatus(
                    transaction,
                    TransactionStatus.COMPLETED,
                    safeGetString(switchResponse, "responseCode"),
                    safeGetString(switchResponse, "switchReference"),
                    safeGetString(switchResponse, "referenceNumber")
                );

                Map<String, Object> result = new HashMap<>();
                result.put("status", "COMPLETED");
                result.put("transactionId", transaction.transactionId());
                result.put("referenceNumber", transaction.referenceNumber());

                idempotencyCache.save(idempotencyKey, result, IDEMPOTENCY_TTL);
                return result;
            } else {
                String respCode = safeGetString(switchResponse, "responseCode");
                transaction = updateTransactionStatus(
                    transaction,
                    TransactionStatus.FAILED,
                    respCode,
                    safeGetString(switchResponse, "switchReference"),
                    null
                );

                throw new LedgerException(respCode != null ? respCode : "ERR_SWITCH_NO_RESPONSE", "DECLINE", "Deposit failed at switch");
            }
        } catch (Exception e) {
            if (!(e instanceof LedgerException)) {
                updateTransactionStatus(
                    transaction,
                    TransactionStatus.FAILED,
                    "ERR_SYS_SWITCH_ERROR",
                    null,
                    null
                );
            }
            throw e;
        }
    }

    private TransactionRecord updateTransactionStatus(
            TransactionRecord transaction, String newStatus,
            String errorCode, String switchReference, String referenceNumber) {

        TransactionRecord updated = new TransactionRecord(
            transaction.transactionId(),
            transaction.idempotencyKey(),
            transaction.agentId(),
            transaction.transactionType(),
            transaction.amount(),
            transaction.customerFee(),
            transaction.agentCommission(),
            transaction.bankShare(),
            newStatus,
            errorCode,
            transaction.customerMykad(),
            transaction.customerCardMasked(),
            switchReference,
            referenceNumber,
            transaction.geofenceLat(),
            transaction.geofenceLng(),
            transaction.createdAt(),
            LocalDateTime.now(),
            transaction.agentTier(),
            transaction.targetBin(),
            transaction.billerCode(),
            transaction.ref1(),
            transaction.ref2(),
            transaction.destinationAccount()
        );

        return transactionRepository.save(updated);
    }

    public void provisionAgentFloat(UUID agentId, String tier, double lat, double lng,
                                   String description, String referenceNumber, 
                                   String billerCode, String targetBin, 
                                   String destinationAccount, String ref1, String ref2) {
        log.info("Provisioning float for agent: {}, tier: {}", agentId, tier);
        AgentFloatRecord floatRecord = new AgentFloatRecord(
            null, // floatId
            agentId, 
            BigDecimal.ZERO, 
            BigDecimal.ZERO, // reservedBalance
            MYR_CURRENCY, 
            null, // version
            BigDecimal.valueOf(lat), 
            BigDecimal.valueOf(lng)
        );
        agentFloatRepository.save(floatRecord);
        
        // Log initialization transaction with metadata
        var transaction = new TransactionRecord(
            null, UUID.randomUUID().toString(), agentId, TransactionType.CASH_DEPOSIT,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            TransactionStatus.COMPLETED, null, null, null, null, "INITIAL_PROVISION",
            BigDecimal.valueOf(lat), BigDecimal.valueOf(lng), LocalDateTime.now(), LocalDateTime.now(),
            tier, targetBin, billerCode, ref1, ref2, destinationAccount
        );
        transactionRepository.save(transaction);
    }

    public Map<String, Object> processRetailSale(UUID merchantId, BigDecimal amount, 
                                               String cardData, String pinBlock, String idempotencyKey,
                                               UUID agentId, String description, String referenceNumber,
                                               String agentTier, String billerCode, String targetBin,
                                               String destinationAccount, String ref1, String ref2) {
        log.info("Processing retail sale for merchant: {}, amount: {}", merchantId, amount);
        Map<String, Object> result = new HashMap<>();
        result.put("status", "COMPLETED");
        result.put("transactionId", UUID.randomUUID().toString());
        result.put("amount", amount);
        result.put("mdrAmount", amount.multiply(new BigDecimal("0.02")));
        result.put("netToMerchant", amount.multiply(new BigDecimal("0.98")));
        return result;
    }

    public Map<String, Object> processPinPurchase(UUID agentId, String productCode, BigDecimal amount, 
                                                 String idempotencyKey, String agentTier, String billerCode,
                                                 String targetBin, String destinationAccount, String ref1, String ref2) {
        log.info("Processing PIN purchase for agent: {}, product: {}", agentId, productCode);
        Map<String, Object> result = new HashMap<>();
        result.put("status", "COMPLETED");
        result.put("transactionId", UUID.randomUUID().toString());
        result.put("pinCode", "PIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.put("commission", amount.multiply(new BigDecimal("0.05")));
        return result;
    }

    public BigDecimal getBalance(UUID agentId) {
        AgentFloatRecord agentFloat = agentFloatRepository.findById(agentId);
        if (agentFloat == null) {
            throw new IllegalArgumentException("Agent not found: " + agentId);
        }
        return agentFloat.balance();
    }

    public List<TransactionRecord> getTransactionsByAgentId(UUID agentId) {
        return transactionRepository.findByAgentId(agentId);
    }

    public Optional<TransactionRecord> getTransactionById(UUID transactionId) {
        return Optional.ofNullable(transactionRepository.findById(transactionId));
    }

    public Map<String, Object> processCashBack(UUID merchantId, BigDecimal amount, 
                                             String cardData, String pinBlock, String idempotencyKey) {
        log.info("Processing cash back for merchant: {}, amount: {}", merchantId, amount);
        // Specialized cash back logic...
        Map<String, Object> result = new HashMap<>();
        result.put("status", "COMPLETED");
        result.put("transactionId", UUID.randomUUID().toString());
        result.put("cashBackAmount", amount);
        result.put("commission", amount.multiply(new BigDecimal("0.01")));
        return result;
    }
}
