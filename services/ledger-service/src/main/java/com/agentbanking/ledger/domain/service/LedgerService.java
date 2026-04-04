package com.agentbanking.ledger.domain.service;

import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.common.exception.LedgerException;
import com.agentbanking.common.geofence.GeofenceChecker;
import com.agentbanking.common.transaction.TransactionStatus;
import com.agentbanking.common.efm.EfmEventPublisher;
import com.agentbanking.ledger.domain.model.*;
import com.agentbanking.ledger.domain.port.out.*;
import com.agentbanking.onboarding.domain.model.AgentRecord;
import com.agentbanking.onboarding.domain.model.AgentTier;

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
            efmEventPublisher.publishFraudAlert("GPS_UNAVAILABLE", null, agentId,
                "GPS coordinates not provided for withdrawal");
            throw new LedgerException(ErrorCodes.ERR_GPS_UNAVAILABLE, "DECLINE", 
                "GPS coordinates not provided");
        }
        
        if (agentFloat.merchantGpsLat() != null && agentFloat.merchantGpsLng() != null) {
            if (!GeofenceChecker.isWithinGeofence(
                    agentFloat.merchantGpsLat(), agentFloat.merchantGpsLng(),
                    geofenceLat, geofenceLng, 100.0)) {
                efmEventPublisher.publishFraudAlert("GEOFENCE_VIOLATION", null, agentId,
                    "Transaction attempted outside merchant location");
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
        
        // Publish EFM event for successful withdrawal
        Map<String, Object> efmDetails = new HashMap<>();
        efmDetails.put("transactionType", TransactionType.CASH_WITHDRAWAL);
        efmDetails.put("amount", amount);
        efmDetails.put("customerFee", customerFee);
        efmDetails.put("agentCommission", agentCommission);
        efmDetails.put("bankShare", bankShare);
        efmDetails.put("customerCardMasked", customerCardMasked);
        efmEventPublisher.publishEvent("CASH_WITHDRAWAL", transactionId, agentId, efmDetails);
        
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
         
         // Publish EFM event for successful deposit
         Map<String, Object> efmDetails = new HashMap<>();
         efmDetails.put("transactionType", TransactionType.CASH_DEPOSIT);
         efmDetails.put("amount", amount);
         efmDetails.put("customerFee", customerFee);
         efmDetails.put("agentCommission", agentCommission);
         efmDetails.put("bankShare", bankShare);
         efmEventPublisher.publishEvent("CASH_DEPOSIT", transactionId, agentId, efmDetails);
         
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
         AgentFloatRecord agentFloat = agentFloatRepository.findById(agentId);
         if (agentFloat == null) {
             throw new LedgerException(ErrorCodes.ERR_AGENT_FLOAT_NOT_FOUND, "RETRY");
         }
         
         if (!MYR_CURRENCY.equals(agentFloat.currency())) {
             throw new LedgerException(ErrorCodes.ERR_INVALID_CURRENCY, "DECLINE", 
                 "Only MYR currency is supported, got: " + agentFloat.currency());
         }
         
         return agentFloat.balance();
     }
     
     /**
      * Process retail sale transaction (card/QR or PIN purchase)
      * US-M01
      */
     @SuppressWarnings("unchecked")
     public Map<String, Object> processRetailSale(UUID merchantId, BigDecimal amount, 
                                                  String cardData, String pinBlock, String idempotencyKey) {
         if (idempotencyKey != null && idempotencyCache.exists(idempotencyKey)) {
             try {
                 return idempotencyCache.get(idempotencyKey, Map.class);
             } catch (Exception e) {
             }
         }
         
          amount = amount.setScale(2, RoundingMode.HALF_UP);
          
          // Authorize via switch
          Map<String, Object> authResult = switchService.authorize(cardData, pinBlock, amount, merchantId.toString());
          Boolean approved = (Boolean) authResult.get("approved");
          if (approved == null || !approved) {
              String declineCode = (String) authResult.getOrDefault("declineCode", "ERR_SWITCH_DECLINED");
              throw new LedgerException(declineCode, "DECLINE");
          }
         
          // Get agent tier
          Optional<AgentRecord> agentOpt = agentRepository.findById(merchantId);
          if (agentOpt.isEmpty()) {
              throw new LedgerException(ErrorCodes.ERR_AGENT_NOT_FOUND, "DECLINE");
          }
          AgentRecord agent = agentOpt.get();
          AgentTier tier = agent.tier(); // Already using MICRO, STANDARD, PREMIER from onboarding
         
         // Calculate MDR
         MdrCalculation mdr = merchantTransactionService.calculateMdr(amount, tier);
         BigDecimal netAmount = amount.subtract(mdr.mdrAmount());
         
         // Credit agent float with net amount
         AgentFloatRecord agentFloat = agentFloatRepository.findByIdWithLock(merchantId);
         if (agentFloat == null) {
             throw new LedgerException(ErrorCodes.ERR_AGENT_FLOAT_NOT_FOUND, "RETRY");
         }
         
         if (!MYR_CURRENCY.equals(agentFloat.currency())) {
             throw new LedgerException(ErrorCodes.ERR_INVALID_CURRENCY, "DECLINE", 
                 "Only MYR currency is supported");
         }
         
         BigDecimal newBalance = agentFloat.balance().add(netAmount).setScale(2, RoundingMode.HALF_UP);
         AgentFloatRecord updatedFloat = new AgentFloatRecord(
                 agentFloat.floatId(),
                 agentFloat.agentId(),
                 newBalance,
                 agentFloat.reservedBalance(),
                 agentFloat.currency(),
                 agentFloat.version() + 1,
                 agentFloat.merchantGpsLat(),
                 agentFloat.merchantGpsLng()
         );
         agentFloatRepository.save(updatedFloat);
         
         // Create transaction record
         UUID transactionId = UUID.randomUUID();
         LocalDateTime now = LocalDateTime.now();
         TransactionRecord txn = new TransactionRecord(
                 transactionId,
                 idempotencyKey,
                 merchantId,
                 TransactionType.CASHLESS_PAYMENT,
                 amount,
                 BigDecimal.ZERO, // customer fee
                 BigDecimal.ZERO, // agent commission
                 mdr.mdrAmount(), // bank share (MDR)
                 TransactionStatus.COMPLETED,
                 null,
                 null,
                 null,
                 (String) authResult.getOrDefault("referenceNumber", ""),
                 null,
                 null,
                 now,
                 now
         );
         transactionRepository.save(txn);
         
         // Create journal entries
         List<JournalEntryRecord> entries = new ArrayList<>();
         entries.add(new JournalEntryRecord(
                 UUID.randomUUID(),
                 transactionId,
                 EntryType.DEBIT,
                 "AGENT_FLOAT_" + merchantId,
                 netAmount,
                 "Retail sale net credit",
                 now
         ));
         entries.add(new JournalEntryRecord(
                 UUID.randomUUID(),
                 transactionId,
                 EntryType.CREDIT,
                 "SALES_REVENUE",
                 amount,
                 "Retail sale revenue",
                 now
         ));
         entries.add(new JournalEntryRecord(
                 UUID.randomUUID(),
                 transactionId,
                 EntryType.DEBIT,
                 "MDR_EXPENSE",
                 mdr.mdrAmount(),
                 "MDR expense for retail sale",
                 now
         ));
          journalEntryRepository.saveAll(entries);
          
          // Build response
          Map<String, Object> response = Map.of(
                  "status", "COMPLETED",
                  "transactionId", transactionId.toString(),
                  "amount", amount,
                  "mdrAmount", mdr.mdrAmount(),
                  "netToMerchant", netAmount
          );
          
          if (idempotencyKey != null) {
              idempotencyCache.save(idempotencyKey, response, IDEMPOTENCY_TTL);
          }
          
          // Publish EFM event for successful transaction
          Map<String, Object> efmDetails = new HashMap<>();
          efmDetails.put("transactionType", TransactionType.CASHLESS_PAYMENT);
          efmDetails.put("amount", amount);
          efmDetails.put("mdrAmount", mdr.mdrAmount());
          efmDetails.put("netToMerchant", netAmount);
          efmDetails.put("referenceNumber", authResult.getOrDefault("referenceNumber", ""));
          efmEventPublisher.publishEvent("RETAIL_SALE", transactionId, merchantId, efmDetails);
          
          return response;
     }
     
     /**
      * Process cash-back transaction
      * US-M02, US-M03
      */
     @SuppressWarnings("unchecked")
     public Map<String, Object> processCashBack(UUID merchantId, BigDecimal cashBackAmount,
                                                String cardData, String pinBlock, String idempotencyKey) {
         if (idempotencyKey != null && idempotencyCache.exists(idempotencyKey)) {
             try {
                 return idempotencyCache.get(idempotencyKey, Map.class);
             } catch (Exception e) {
             }
         }
         
         cashBackAmount = cashBackAmount.setScale(2, RoundingMode.HALF_UP);
         
         // Calculate commission
         BigDecimal commission = merchantTransactionService.calculateCashBackCommission(cashBackAmount);
         BigDecimal totalAmount = cashBackAmount.add(commission);
         
          // Authorize total amount via switch
          Map<String, Object> authResult = switchService.authorize(cardData, pinBlock, totalAmount, merchantId.toString());
          Boolean approved = (Boolean) authResult.get("approved");
          if (approved == null || !approved) {
              String declineCode = (String) authResult.getOrDefault("declineCode", "ERR_SWITCH_DECLINED");
              throw new LedgerException(declineCode, "DECLINE");
          }
         
          // Get agent for tier
           Optional<AgentRecord> agentOpt = agentRepository.findById(merchantId);
           if (agentOpt.isEmpty()) {
               throw new LedgerException(ErrorCodes.ERR_AGENT_NOT_FOUND, "DECLINE");
           }
           AgentRecord agent = agentOpt.get();
           AgentTier tier = agent.tier(); // Already using MICRO, STANDARD, PREMIER from onboarding
         
         // Update agent float: commission added, cashBackAmount subtracted
         AgentFloatRecord agentFloat = agentFloatRepository.findByIdWithLock(merchantId);
         if (agentFloat == null) {
             throw new LedgerException(ErrorCodes.ERR_AGENT_FLOAT_NOT_FOUND, "RETRY");
         }
         
         if (!MYR_CURRENCY.equals(agentFloat.currency())) {
             throw new LedgerException(ErrorCodes.ERR_INVALID_CURRENCY, "DECLINE", 
                 "Only MYR currency is supported");
         }
         
         BigDecimal newBalance = agentFloat.balance().add(commission).subtract(cashBackAmount)
                 .setScale(2, RoundingMode.HALF_UP);
         AgentFloatRecord updatedFloat = new AgentFloatRecord(
                 agentFloat.floatId(),
                 agentFloat.agentId(),
                 newBalance,
                 agentFloat.reservedBalance(),
                 agentFloat.currency(),
                 agentFloat.version() + 1,
                 agentFloat.merchantGpsLat(),
                 agentFloat.merchantGpsLng()
         );
         agentFloatRepository.save(updatedFloat);
         
         // Create transaction record
         UUID transactionId = UUID.randomUUID();
         LocalDateTime now = LocalDateTime.now();
         TransactionRecord txn = new TransactionRecord(
                 transactionId,
                 idempotencyKey,
                 merchantId,
                 TransactionType.CASH_BACK,
                 totalAmount,
                 BigDecimal.ZERO, // customer fee
                 commission,
                 BigDecimal.ZERO, // bank share
                 TransactionStatus.COMPLETED,
                 null,
                 null,
                 null,
                 (String) authResult.getOrDefault("referenceNumber", ""),
                 null,
                 null,
                 now,
                 now
         );
         transactionRepository.save(txn);
         
         // Create journal entries
         List<JournalEntryRecord> entries = new ArrayList<>();
         entries.add(new JournalEntryRecord(
                 UUID.randomUUID(),
                 transactionId,
                 EntryType.DEBIT,
                 "AGENT_FLOAT_" + merchantId,
                 commission,
                 "Cash-back commission credit",
                 now
         ));
         entries.add(new JournalEntryRecord(
                 UUID.randomUUID(),
                 transactionId,
                 EntryType.CREDIT,
                 "AGENT_FLOAT_" + merchantId,
                 cashBackAmount,
                 "Cash-back amount debit",
                 now
         ));
         if (cashBackAmount.compareTo(commission) >= 0) {
             entries.add(new JournalEntryRecord(
                     UUID.randomUUID(),
                     transactionId,
                     EntryType.DEBIT,
                     "CASH_BACK_COST",
                     cashBackAmount.subtract(commission),
                     "Net cash-back cost",
                     now
             ));
         } else {
             entries.add(new JournalEntryRecord(
                     UUID.randomUUID(),
                     transactionId,
                     EntryType.CREDIT,
                     "CASH_BACK_REVENUE",
                     commission.subtract(cashBackAmount),
                     "Net cash-back gain",
                     now
             ));
         }
         journalEntryRepository.saveAll(entries);
         
         // Build response
         Map<String, Object> response = Map.of(
                 "status", "COMPLETED",
                 "transactionId", transactionId.toString(),
                 "cashBackAmount", cashBackAmount,
                 "commission", commission
         );
         
          if (idempotencyKey != null) {
              idempotencyCache.save(idempotencyKey, response, IDEMPOTENCY_TTL);
          }
          
          // Publish EFM event for successful cash-back transaction
          Map<String, Object> efmDetails = new HashMap<>();
          efmDetails.put("transactionType", TransactionType.CASH_BACK);
          efmDetails.put("cashBackAmount", cashBackAmount);
          efmDetails.put("commission", commission);
          efmDetails.put("totalAmount", totalAmount);
          efmDetails.put("referenceNumber", authResult.getOrDefault("referenceNumber", ""));
          efmEventPublisher.publishEvent("CASH_BACK", transactionId, merchantId, efmDetails);
          
           return response;
      }

     /**
      * Process PIN voucher purchase
      * Agent earns commission on PIN voucher sales
      */
     public Map<String, Object> processPinPurchase(UUID agentId, String productCode,
                                                    BigDecimal amount, String idempotencyKey) {
         if (idempotencyKey != null && idempotencyCache.exists(idempotencyKey)) {
             try {
                 return idempotencyCache.get(idempotencyKey, Map.class);
             } catch (Exception e) {
             }
         }

         amount = amount.setScale(2, RoundingMode.HALF_UP);

         // Get agent for tier
         Optional<AgentRecord> agentOpt = agentRepository.findById(agentId);
          if (agentOpt.isEmpty()) {
              throw new LedgerException(ErrorCodes.ERR_AGENT_NOT_FOUND, "DECLINE");
          }
          AgentRecord agent = agentOpt.get();
          
           AgentTier tier = agent.tier(); // Already using MICRO, STANDARD, PREMIER from onboarding

          // Calculate commission (agent earns commission on PIN sales)
         BigDecimal commission = merchantTransactionService.calculatePinPurchaseCommission(amount, tier);

         // Credit agent float with commission
         AgentFloatRecord agentFloat = agentFloatRepository.findByIdWithLock(agentId);
         if (agentFloat == null) {
             throw new LedgerException(ErrorCodes.ERR_AGENT_FLOAT_NOT_FOUND, "RETRY");
         }

         if (!MYR_CURRENCY.equals(agentFloat.currency())) {
             throw new LedgerException(ErrorCodes.ERR_INVALID_CURRENCY, "DECLINE",
                 "Only MYR currency is supported");
         }

         BigDecimal newBalance = agentFloat.balance().add(commission).setScale(2, RoundingMode.HALF_UP);
         AgentFloatRecord updatedFloat = new AgentFloatRecord(
                 agentFloat.floatId(),
                 agentFloat.agentId(),
                 newBalance,
                 agentFloat.reservedBalance(),
                 agentFloat.currency(),
                 agentFloat.version() + 1,
                 agentFloat.merchantGpsLat(),
                 agentFloat.merchantGpsLng()
         );
         agentFloatRepository.save(updatedFloat);

         // Create transaction record
         UUID transactionId = UUID.randomUUID();
         LocalDateTime now = LocalDateTime.now();
         TransactionRecord txn = new TransactionRecord(
                 transactionId,
                 idempotencyKey,
                 agentId,
                 TransactionType.PIN_PURCHASE,
                 amount,
                 BigDecimal.ZERO, // customer fee
                 commission,
                 BigDecimal.ZERO, // bank share
                 TransactionStatus.COMPLETED,
                 null,
                 null,
                 null,
                 "",
                 null,
                 null,
                 now,
                 now
         );
         transactionRepository.save(txn);

         // Create journal entries (double-entry)
         List<JournalEntryRecord> entries = new ArrayList<>();
         entries.add(new JournalEntryRecord(
                 UUID.randomUUID(),
                 transactionId,
                 EntryType.CREDIT,
                 "PIN_PURCHASE_COMMISSION",
                 commission,
                 "Commission earned for PIN voucher sale",
                 now
         ));
         entries.add(new JournalEntryRecord(
                 UUID.randomUUID(),
                 transactionId,
                 EntryType.DEBIT,
                 "COMMISSION_EXPENSE",
                 commission,
                 "Commission expense for PIN voucher",
                 now
         ));
         journalEntryRepository.saveAll(entries);

         // Generate PIN code (mock - in real system this would come from PIN supplier)
         String pinCode = "PIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

         // Build response
         Map<String, Object> response = Map.of(
                 "status", "COMPLETED",
                 "transactionId", transactionId.toString(),
                 "pinCode", pinCode,
                 "commission", commission
         );

         if (idempotencyKey != null) {
             idempotencyCache.save(idempotencyKey, response, IDEMPOTENCY_TTL);
         }

         // Publish EFM event
         Map<String, Object> efmDetails = new HashMap<>();
         efmDetails.put("transactionType", TransactionType.PIN_PURCHASE);
         efmDetails.put("productCode", productCode);
         efmDetails.put("amount", amount);
         efmDetails.put("commission", commission);
         efmEventPublisher.publishEvent("PIN_PURCHASE", transactionId, agentId, efmDetails);

         return response;
     }
 }
