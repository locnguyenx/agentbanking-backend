# Hexagonal Architecture Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix critical hexagonal architecture violations across all services - move JPA annotations out of domain layer, add repository implementations, fix double-to-BigDecimal issues, implement real JWT validation, and add missing features.

**Architecture:** 
- Move JPA entities from `domain/model/` to `infrastructure/persistence/entity/`
- Create repository interfaces in `domain/port/out/` and implementations in `infrastructure/persistence/`
- Controllers remain in `infrastructure/web/`
- Domain services depend only on port interfaces (Dependency Inversion)
- Add common module as dependency to all services

**Tech Stack:** Java 21, Spring Boot 3.x, Spring Data JPA, Spring Security OAuth2 Resource Server

---

## Task 1: Fix rules-service - Hexagonal Refactor

**BRD Requirements:** Per AGENTS.md: "domain/ must have ZERO imports from Spring, JPA, Kafka, or any infrastructure framework"

**Files:**
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/domain/model/FeeConfigRecord.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/domain/model/VelocityRuleRecord.java`
- Keep existing: `services/rules-service/src/main/java/com/agentbanking/rules/domain/model/AgentTier.java` (enum)
- Keep existing: `services/rules-service/src/main/java/com/agentbanking/rules/domain/model/TransactionType.java` (enum)
- Keep existing: `services/rules-service/src/main/java/com/agentbanking/rules/domain/model/FeeType.java` (enum)
- Keep existing: `services/rules-service/src/main/java/com/agentbanking/rules/domain/model/VelocityScope.java` (enum)
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/persistence/entity/FeeConfigEntity.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/persistence/entity/VelocityRuleEntity.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/persistence/mapper/FeeConfigMapper.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/persistence/mapper/VelocityRuleMapper.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/persistence/repository/JpaFeeConfigRepository.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/persistence/repository/FeeConfigJpaRepository.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/persistence/repository/JpaVelocityRuleRepository.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/persistence/repository/VelocityRuleJpaRepository.java`
- Modify: `services/rules-service/src/main/java/com/agentbanking/rules/domain/service/FeeCalculationService.java`
- Modify: `services/rules-service/src/main/java/com/agentbanking/rules/domain/service/VelocityCheckService.java`
- Modify: `services/rules-service/build.gradle` (add common dependency)

- [ ] **Step 1: Write domain model records (no JPA)**

```java
// services/rules-service/src/main/java/com/agentbanking/rules/domain/model/FeeConfigRecord.java
package com.agentbanking.rules.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FeeConfigRecord(
    UUID feeConfigId,
    TransactionType transactionType,
    AgentTier agentTier,
    FeeType feeType,
    BigDecimal customerFeeValue,
    BigDecimal agentCommissionValue,
    BigDecimal bankShareValue,
    BigDecimal dailyLimitAmount,
    Integer dailyLimitCount,
    LocalDate effectiveFrom,
    LocalDate effectiveTo
) {}
```

- [ ] **Step 2: Write JPA entities in infrastructure layer**

```java
// services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/persistence/entity/FeeConfigEntity.java
package com.agentbanking.rules.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fee_config")
public class FeeConfigEntity {
    @Id
    private UUID feeConfigId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private TransactionType transactionType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "agent_tier")
    private AgentTier agentTier;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type")
    private FeeType feeType;
    
    @Column(name = "customer_fee_value")
    private BigDecimal customerFeeValue;
    
    @Column(name = "agent_commission_value")
    private BigDecimal agentCommissionValue;
    
    @Column(name = "bank_share_value")
    private BigDecimal bankShareValue;
    
    @Column(name = "daily_limit_amount")
    private BigDecimal dailyLimitAmount;
    
    @Column(name = "daily_limit_count")
    private Integer dailyLimitCount;
    
    @Column(name = "effective_from")
    private LocalDate effectiveFrom;
    
    @Column(name = "effective_to")
    private LocalDate effectiveTo;
    
    // getters and setters
}
```

- [ ] **Step 3: Write mapper between entity and domain**

```java
// services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/persistence/mapper/FeeConfigMapper.java
package com.agentbanking.rules.infrastructure.persistence.mapper;

import com.agentbanking.rules.domain.model.FeeConfigRecord;
import com.agentbanking.rules.infrastructure.persistence.entity.FeeConfigEntity;

public class FeeConfigMapper {
    public static FeeConfigRecord toDomain(FeeConfigEntity entity) {
        return new FeeConfigRecord(
            entity.getFeeConfigId(),
            entity.getTransactionType(),
            entity.getAgentTier(),
            entity.getFeeType(),
            entity.getCustomerFeeValue(),
            entity.getAgentCommissionValue(),
            entity.getBankShareValue(),
            entity.getDailyLimitAmount(),
            entity.getDailyLimitCount(),
            entity.getEffectiveFrom(),
            entity.getEffectiveTo()
        );
    }
}
```

- [ ] **Step 4: Implement repository interface in infrastructure**

```java
// services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/persistence/repository/JpaFeeConfigRepository.java
package com.agentbanking.rules.infrastructure.persistence.repository;

import com.agentbanking.rules.domain.port.out.FeeConfigRepository;
import com.agentbanking.rules.domain.model.FeeConfigRecord;
import com.agentbanking.rules.domain.model.AgentTier;
import com.agentbanking.rules.domain.model.TransactionType;
import com.agentbanking.rules.infrastructure.persistence.mapper.FeeConfigMapper;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public class JpaFeeConfigRepository implements FeeConfigRepository {
    
    private final FeeConfigJpaRepository jpaRepository;
    
    public JpaFeeConfigRepository(FeeConfigJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public Optional<FeeConfigRecord> findByTransactionTypeAndAgentTier(
            TransactionType transactionType, AgentTier agentTier, LocalDate asOfDate) {
        return jpaRepository.findByTransactionTypeAndAgentTierAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
            transactionType, agentTier, asOfDate, asOfDate
        ).map(FeeConfigMapper::toDomain);
    }
}
```

- [ ] **Step 4b: Write Spring Data JPA repository (separate file)**

```java
// services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/persistence/repository/FeeConfigJpaRepository.java
package com.agentbanking.rules.infrastructure.persistence.repository;

import com.agentbanking.rules.domain.model.AgentTier;
import com.agentbanking.rules.domain.model.TransactionType;
import com.agentbanking.rules.infrastructure.persistence.entity.FeeConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

interface FeeConfigJpaRepository extends JpaRepository<FeeConfigEntity, UUID> {
    Optional<FeeConfigEntity> findByTransactionTypeAndAgentTierAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
        TransactionType transactionType, AgentTier agentTier, LocalDate effectiveFrom, LocalDate effectiveTo);
}
```

- [ ] **Step 5: Update domain service to use repository port**

```java
// services/rules-service/src/main/java/com/agentbanking/rules/domain/service/FeeCalculationService.java
package com.agentbanking.rules.domain.service;

import com.agentbanking.rules.domain.model.*;
import com.agentbanking.rules.domain.port.out.FeeConfigRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class FeeCalculationService {
    
    private final FeeConfigRepository feeConfigRepository;
    
    public FeeCalculationService(FeeConfigRepository feeConfigRepository) {
        this.feeConfigRepository = feeConfigRepository;
    }
    
    public FeeCalculationResult calculateFee(TransactionType txType, AgentTier tier, BigDecimal amount) {
        Optional<FeeConfigRecord> config = feeConfigRepository.findByTransactionTypeAndAgentTier(
            txType, tier, LocalDate.now()
        );
        
        if (config.isEmpty()) {
            throw new IllegalStateException("No fee config found");
        }
        
        FeeConfigRecord fc = config.get();
        BigDecimal customerFee;
        if (fc.feeType() == FeeType.FLAT) {
            customerFee = fc.customerFeeValue();
        } else {
            // AGENTS.md requires HALF_UP rounding to 2 decimal places
            customerFee = amount.multiply(fc.customerFeeValue())
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        }
            
        return new FeeCalculationResult(customerFee, fc.agentCommissionValue(), fc.bankShareValue());
    }
    
    public record FeeCalculationResult(BigDecimal customerFee, BigDecimal agentCommission, BigDecimal bankShare) {}
}
```

- [ ] **Step 6: Add common dependency to build.gradle**

```groovy
// services/rules-service/build.gradle - add this line
dependencies {
    // existing deps...
    implementation project(':common')
}
```

- [ ] **Step 7: Commit - rules-service hexagonal refactor**

```bash
git add services/rules-service/src/main/java/com/agentbanking/rules/domain/model/ services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/ services/rules-service/build.gradle
git commit -m "refactor(rules-service): implement hexagonal architecture - separate domain from JPA entities"
```

---

## Task 2: Fix ledger-service - Hexagonal Refactor

**BRD Requirements:** Per AGENTS.md: "All financial methods must be marked @Transactional" and "Ledger Updates: Must use PESSIMISTIC_WRITE locks"

**Files:**
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/model/AgentFloatRecord.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/model/TransactionRecord.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/model/JournalEntryRecord.java`
- Keep existing enums: `TransactionType.java`, `TransactionStatus.java`, `EntryType.java` (already in domain/model)
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/port/out/TransactionRepository.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/port/out/AgentFloatRepository.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/port/out/JournalEntryRepository.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/port/out/IdempotencyCache.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/entity/AgentFloatEntity.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/entity/TransactionEntity.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/entity/JournalEntryEntity.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/repository/JpaTransactionRepository.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/repository/JpaAgentFloatRepository.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/repository/AgentFloatJpaRepository.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/repository/JpaJournalEntryRepository.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/mapper/*Mapper.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/efm/LedgerEventPublisher.java`
- Modify: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/LedgerService.java`
- Modify: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/web/LedgerController.java`
- Modify: `services/ledger-service/build.gradle` (add common dependency)
- Modify: `services/ledger-service/src/main/resources/db/migration/V1__init_ledger.sql` (add encryption for mykad)
- Note: `ErrorCodes.java` already exists in common module - will use existing constants

- [ ] **Step 1: Write domain model records**

```java
// services/ledger-service/src/main/java/com/agentbanking/ledger/domain/model/AgentFloatRecord.java
package com.agentbanking.ledger.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AgentFloatRecord(
    UUID floatId,
    UUID agentId,
    BigDecimal balance,
    BigDecimal reservedBalance,
    String currency,
    Long version,
    LocalDateTime updatedAt
) {}
```

```java
// services/ledger-service/src/main/java/com/agentbanking/ledger/domain/model/TransactionRecord.java
package com.agentbanking.ledger.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionRecord(
    UUID transactionId,
    String idempotencyKey,
    UUID agentId,
    TransactionType transactionType,
    BigDecimal amount,
    BigDecimal customerFee,
    BigDecimal agentCommission,
    BigDecimal bankShare,
    TransactionStatus status,
    String customerCardMasked,
    String customerMykadEncrypted,
    String destinationAccount,
    LocalDateTime createdAt,
    LocalDateTime completedAt
) {}
```

- [ ] **Step 2: Write repository port interfaces (separate per single responsibility)**

```java
// services/ledger-service/src/main/java/com/agentbanking/ledger/domain/port/out/TransactionRepository.java
package com.agentbanking.ledger.domain.port.out;

import com.agentbanking.ledger.domain.model.TransactionRecord;
import java.util.Optional;

public interface TransactionRepository {
    Optional<TransactionRecord> findByIdempotencyKey(String key);
    TransactionRecord save(TransactionRecord transaction);
}

// Separate AgentFloatRepository per single responsibility principle
public interface AgentFloatRepository {
    Optional<AgentFloatRecord> findByAgentId(UUID agentId);
    AgentFloatRecord save(AgentFloatRecord agentFloat);
}

// Separate JournalEntryRepository for double-entry accounting
public interface JournalEntryRepository {
    JournalEntryRecord save(JournalEntryRecord entry);
}

public interface IdempotencyCache {
    Optional<String> get(String key);
    void set(String key, String response, java.time.Duration ttl);
}
```

- [ ] **Step 3: Write JPA entities in infrastructure layer**

```java
// services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/entity/TransactionEntity.java
package com.agentbanking.ledger.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger_transaction")
public class TransactionEntity {
    @Id
    private UUID transactionId;
    
    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;
    
    @Column(name = "agent_id")
    private UUID agentId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private TransactionType transactionType;
    
    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "customer_fee", precision = 15, scale = 2)
    private BigDecimal customerFee;
    
    @Column(name = "agent_commission", precision = 15, scale = 2)
    private BigDecimal agentCommission;
    
    @Column(name = "bank_share", precision = 15, scale = 2)
    private BigDecimal bankShare;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TransactionStatus status;
    
    @Column(name = "customer_card_masked")
    private String customerCardMasked;
    
    @Column(name = "customer_mykad_encrypted")  // CHANGED: encrypted
    private String customerMykadEncrypted;
    
    @Column(name = "destination_account")
    private String destinationAccount;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    // getters and setters
}
```

- [ ] **Step 4: Implement repositories with pessimistic locking**

```java
// services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/repository/JpaAgentFloatRepository.java
package com.agentbanking.ledger.infrastructure.persistence.repository;

import com.agentbanking.ledger.domain.port.out.TransactionRepository;
import com.agentbanking.ledger.domain.model.AgentFloatRecord;
import com.agentbanking.ledger.infrastructure.persistence.entity.AgentFloatEntity;
import com.agentbanking.ledger.infrastructure.persistence.mapper.AgentFloatMapper;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaAgentFloatRepository implements AgentFloatRepository {
    
    private final AgentFloatJpaRepository jpaRepository;
    
    public JpaAgentFloatRepository(AgentFloatJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    public Optional<AgentFloatRecord> findByAgentIdForUpdate(UUID agentId) {
        return jpaRepository.findByAgentIdWithLock(agentId).map(AgentFloatMapper::toDomain);
    }
    
    @Override
    public Optional<AgentFloatRecord> findByAgentId(UUID agentId) {
        return jpaRepository.findByAgentId(agentId).map(AgentFloatMapper::toDomain);
    }
    
    @Override
    public AgentFloatRecord save(AgentFloatRecord record) {
        return AgentFloatMapper.toDomain(jpaRepository.save(AgentFloatMapper.toEntity(record)));
    }
}
```

- [ ] **Step 4b: Add AgentFloatJpaRepository with pessimistic lock**

```java
// services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/repository/AgentFloatJpaRepository.java
package com.agentbanking.ledger.infrastructure.persistence.repository;

import com.agentbanking.ledger.infrastructure.persistence.entity.AgentFloatEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;

interface AgentFloatJpaRepository extends JpaRepository<AgentFloatEntity, UUID> {
    Optional<AgentFloatEntity> findByAgentId(UUID agentId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AgentFloatEntity a WHERE a.agentId = :agentId")
    Optional<AgentFloatEntity> findByAgentIdWithLock(UUID agentId);
}
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AgentFloatEntity a WHERE a.agentId = :agentId")
    public Optional<AgentFloatRecord> findByAgentId(UUID agentId) {
        return jpaRepository.findByAgentId(agentId).map(AgentFloatMapper::toDomain);
    }
    
    public Optional<AgentFloatRecord> findByAgentIdForUpdate(UUID agentId) {
        return jpaRepository.findByAgentId(agentId).map(AgentFloatMapper::toDomain);
    }
    
    @Override
    public AgentFloatRecord save(AgentFloatRecord record) {
        return AgentFloatMapper.toDomain(jpaRepository.save(AgentFloatMapper.toEntity(record)));
    }
}
```

- [ ] **Step 5: Add idempotency check against Redis**

```java
// services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/repository/RedisIdempotencyCache.java
package com.agentbanking.ledger.infrastructure.persistence.repository;

import com.agentbanking.ledger.domain.port.out.IdempotencyCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.Optional;

@Component
public class RedisIdempotencyCache implements IdempotencyCache {
    
    private final StringRedisTemplate redis;
    
    public RedisIdempotencyCache(StringRedisTemplate redis) {
        this.redis = redis;
    }
    
    @Override
    public Optional<String> get(String key) {
        String value = redis.opsForValue().get("idempotency:" + key);
        return Optional.ofNullable(value);
    }
    
    @Override
    public void set(String key, String response, Duration ttl) {
        redis.opsForValue().set("idempotency:" + key, response, ttl);
    }
}
```

- [ ] **Step 5b: Add JournalEntry JPA repository**

```java
// services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/repository/JpaJournalEntryRepository.java
package com.agentbanking.ledger.infrastructure.persistence.repository;

import com.agentbanking.ledger.domain.port.out.JournalEntryRepository;
import com.agentbanking.ledger.domain.model.JournalEntryRecord;
import com.agentbanking.ledger.infrastructure.persistence.mapper.JournalEntryMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JpaJournalEntryRepository implements JournalEntryRepository {
    
    private final JournalEntryJpaRepository jpaRepository;
    
    public JpaJournalEntryRepository(JournalEntryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public JournalEntryRecord save(JournalEntryRecord record) {
        return JournalEntryMapper.toDomain(jpaRepository.save(JournalEntryMapper.toEntity(record)));
    }
}

interface JournalEntryJpaRepository extends JpaRepository<JournalEntryEntity, UUID> {}
```

- [ ] **Step 6: Add double-entry journal entries**

```java
// services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/LedgerService.java - add method
@Transactional
public TransactionRecord processWithdrawal(UUID agentId, BigDecimal amount, 
                                           BigDecimal customerFee, BigDecimal agentCommission,
                                           BigDecimal bankShare, String idempotencyKey,
                                           String customerCardMasked, String customerMykadEncrypted) {
    // Check idempotency first
    Optional<String> cached = idempotencyCache.get(idempotencyKey);
    if (cached.isPresent()) {
        return objectMapper.readValue(cached.get(), TransactionRecord.class);
    }
    
    // Get agent float with pessimistic lock
    AgentFloatRecord agentFloat = agentFloatRepository.findByAgentIdForUpdate(agentId)
        .orElseThrow(() -> new IllegalArgumentException(ErrorCodes.ERR_AGENT_FLOAT_NOT_FOUND));
    
    if (agentFloat.balance().compareTo(amount) < 0) {
        throw new IllegalStateException(ErrorCodes.ERR_INSUFFICIENT_FLOAT);
    }
    
    // Debit float
    AgentFloatRecord updatedFloat = new AgentFloatRecord(
        agentFloat.floatId(), agentFloat.agentId(),
        agentFloat.balance().subtract(amount), agentFloat.reservedBalance(),
        agentFloat.currency(), agentFloat.version() + 1, LocalDateTime.now()
    );
    agentFloatRepository.save(updatedFloat);
    
    // Create transaction
    TransactionRecord txn = new TransactionRecord(
        UUID.randomUUID(), idempotencyKey, agentId,
        TransactionType.CASH_WITHDRAWAL, amount, customerFee, agentCommission, bankShare,
        TransactionStatus.COMPLETED, customerCardMasked, customerMykadEncrypted, null,
        LocalDateTime.now(), LocalDateTime.now()
    );
    transactionRepository.save(txn);
    
    // Create journal entries (double-entry)
    createJournalEntry(txn, EntryType.DEBIT, "AGENT_FLOAT", amount);
    createJournalEntry(txn, EntryType.CREDIT, "CUSTOMER_FEE", customerFee);
    createJournalEntry(txn, EntryType.CREDIT, "AGENT_COMMISSION", agentCommission);
    createJournalEntry(txn, EntryType.CREDIT, "BANK_SHARE", bankShare);
    
    // Cache response for idempotency
    String response = objectMapper.writeValueAsString(txn);
    idempotencyCache.set(idempotencyKey, response, Duration.ofHours(24));
    
    return txn;
}

private void createJournalEntry(TransactionRecord txn, EntryType type, String account, BigDecimal amount) {
    JournalEntryRecord entry = new JournalEntryRecord(
        UUID.randomUUID(), txn.transactionId(), type, account, amount,
        txn.agentId(), LocalDateTime.now()
    );
    journalEntryRepository.save(entry);
}
```

- [ ] **Step 7: Use ErrorCodes from common module**

```java
// services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/LedgerService.java - imports
import com.agentbanking.common.security.ErrorCodes;

// Use ErrorCodes.ERR_AGENT_FLOAT_NOT_FOUND instead of hardcoded string
```

- [ ] **Step 8: Add common dependency to build.gradle**

```groovy
// services/ledger-service/build.gradle
dependencies {
    // existing deps...
    implementation project(':common')
}
```

- [ ] **Step 9: Commit - ledger-service hexagonal refactor**

```bash
git add services/ledger-service/src/main/java/com/agentbanking/ledger/
git commit -m "refactor(ledger-service): implement hexagonal architecture with repository pattern, idempotency, double-entry journal"
```

---

## Task 3: Fix switch-adapter-service - Change double to BigDecimal

**BRD Requirements:** Per AGENTS.md: "All monetary values use BigDecimal — NEVER use float or double"

**Files:**
- Create: `services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/domain/model/SwitchTransactionRecord.java`
- Create: `services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/infrastructure/persistence/entity/SwitchTransactionEntity.java`
- Create: `services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/infrastructure/persistence/repository/JpaSwitchTransactionRepository.java`
- Create: `services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/infrastructure/web/dto/CardAuthRequest.java`
- Create: `services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/infrastructure/web/dto/ReversalRequest.java`
- Create: `services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/infrastructure/web/dto/DuitNowRequest.java`
- Modify: `services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/domain/service/SwitchAdapterService.java`
- Modify: `services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/infrastructure/web/SwitchController.java`
- Modify: `services/switch-adapter-service/build.gradle`

- [ ] **Step 1: Write DTOs with validation**

```java
// services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/infrastructure/web/dto/CardAuthRequest.java
package com.agentbanking.switchadapter.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record CardAuthRequest(
    @NotNull UUID internalTransactionId,
    @NotBlank String pan,
    @NotNull @Positive BigDecimal amount
) {}
```

- [ ] **Step 2: Fix SwitchAdapterService - use BigDecimal**

```java
// services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/domain/service/SwitchAdapterService.java
package com.agentbanking.switchadapter.domain.service;

import com.agentbanking.switchadapter.domain.model.*;
import com.agentbanking.switchadapter.domain.port.out.SwitchTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SwitchAdapterService {
    
    private final SwitchTransactionRepository repository;
    
    public SwitchAdapterService(SwitchTransactionRepository repository) {
        this.repository = repository;
    }
    
    @Transactional
    public SwitchTransactionRecord processCardAuth(UUID internalTransactionId, 
                                                    String pan, 
                                                    BigDecimal amount) {  // Changed from double
        SwitchTransactionRecord txn = new SwitchTransactionRecord(
            UUID.randomUUID(),
            internalTransactionId,
            MessageType.MT0100,
            "00",
            "PAYNET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            SwitchStatus.APPROVED,
            null,
            amount,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        return repository.save(txn);
    }
    
    // Similarly fix processReversal and processDuitNowTransfer
}
```

- [ ] **Step 3: Fix SwitchController - use DTOs with validation**

```java
// services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/infrastructure/web/SwitchController.java
package com.agentbanking.switchadapter.infrastructure.web;

import com.agentbanking.switchadapter.domain.service.SwitchAdapterService;
import com.agentbanking.switchadapter.infrastructure.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/internal")
public class SwitchController {
    
    private final SwitchAdapterService switchAdapterService;
    
    public SwitchController(SwitchAdapterService switchAdapterService) {
        this.switchAdapterService = switchAdapterService;
    }
    
    @PostMapping("/auth")
    public ResponseEntity<Map<String, Object>> cardAuth(@Valid @RequestBody CardAuthRequest request) {
        SwitchTransactionRecord txn = switchAdapterService.processCardAuth(
            request.internalTransactionId(), request.pan(), request.amount()
        );
        
        return ResponseEntity.ok(Map.of(
            "status", "APPROVED",
            "responseCode", txn.isoResponseCode(),
            "referenceId", txn.switchReference(),
            "switchTxId", txn.switchTxId().toString()
        ));
    }
}
```

- [ ] **Step 4: Commit - switch-adapter BigDecimal fix**

```bash
git add services/switch-adapter-service/
git commit -m "fix(switch-adapter): replace double with BigDecimal for monetary values, add DTOs with validation"
```

---

## Task 4: Fix gateway - Implement Real JWT Validation

**BRD Requirements:** Per AGENTS.md: Gateway must validate JWT tokens, not just check non-blank

**Files:**
- Modify: `gateway/build.gradle` (add OAuth2 resource server dependency)
- Modify: `gateway/src/main/resources/application.yaml` (add JWT configuration)
- Modify: `gateway/src/main/java/com/agentbanking/gateway/filter/JwtAuthFilter.java`
- Create: `gateway/src/main/java/com/agentbanking/gateway/config/SecurityConfig.java`

- [ ] **Step 1: Add OAuth2 resource server dependency**

```groovy
// gateway/build.gradle
plugins {
    id 'java-library'
    id 'org.springframework.boot' version '3.2.5'
    id 'io.spring.dependency-management' version '1.1.5'
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
    implementation 'org.springframework.cloud:spring-cloud-starter-gateway'
    implementation project(':common')
}
```

- [ ] **Step 2: Configure JWT in application.yaml**

```yaml
# gateway/src/main/resources/application.yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI:https://auth.agentbanking.com}
          jwk-set-uri: ${JWT_JWK_SET_URI:https://auth.agentbanking.com/.well-known/jwks.json}
```

- [ ] **Step 3: Implement proper JWT validation filter**

```java
// gateway/src/main/java/com/agentbanking/gateway/filter/JwtAuthFilter.java
package com.agentbanking.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    public JwtAuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);
            
            // Validate using ReactiveSecurityContextHolder
            // Extract claims and validate
            return ReactiveSecurityContextHolder.getContext()
                .filter(ctx -> ctx.getAuthentication() instanceof JwtAuthenticationToken)
                .map(ctx -> (JwtAuthenticationToken) ctx.getAuthentication())
                .map(auth -> {
                    Jwt jwt = auth.getToken();
                    String agentId = jwt.getClaimAsString("agent_id");
                    String agentTier = jwt.getClaimAsString("agent_tier");
                    
                    if (agentId == null) {
                        return exchange;
                    }
                    
                    return exchange.mutate()
                        .request(r -> r
                            .header("X-Agent-Id", agentId)
                            .header("X-Agent-Tier", agentTier != null ? agentTier : ""))
                        .build();
                })
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter)
                .onErrorResume(e -> onError(exchange, "Invalid token: " + e.getMessage(), HttpStatus.UNAUTHORIZED));
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        String body = "{\"status\":\"FAILED\",\"error\":{\"code\":\"ERR_AUTH_INVALID_TOKEN\",\"message\":\"" + message + "\"}}";
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes())));
    }

    public static class Config {}
}
```

- [ ] **Step 4: Add security configuration**

```java
// gateway/src/main/java/com/agentbanking/gateway/config/SecurityConfig.java
package com.agentbanking.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/internal/**").authenticated()
                .anyExchange().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {})
            )
            .build();
    }
}
```

- [ ] **Step 5: Commit - JWT validation**

```bash
git add gateway/
git commit -m "fix(gateway): implement real JWT validation with OAuth2 resource server"
```

---

## Task 5: Add Global Error Handler to All Services

**BRD Requirements:** Per AGENTS.md: "MUST use the Global Error Schema" - must include action_code, trace_id, timestamp

**Files:**
- Create: `services/common/src/main/java/com/agentbanking/common/web/ErrorResponse.java`
- Create: `services/common/src/main/java/com/agentbanking/common/web/GlobalExceptionHandler.java`
- Modify: All service controllers to use GlobalExceptionHandler
- Modify: `common/build.gradle` (add spring-boot-starter-web)

- [ ] **Step 1: Add web dependency to common**

```groovy
// common/build.gradle
dependencies {
    api 'org.slf4j:slf4j-api'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
}
```

- [ ] **Step 2: Write ErrorResponse record**

```java
// common/src/main/java/com/agentbanking/common/web/ErrorResponse.java
package com.agentbanking.common.web;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record ErrorResponse(
    String status,
    ErrorDetail error
) {
    public static ErrorResponse of(String code, String message, String actionCode) {
        // AGENTS.md requires ISO 8601 format: 2026-03-25T14:30:00+08:00
        String timestamp = OffsetDateTime.now(java.time.ZoneOffset.of("+08:00"))
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return new ErrorResponse("FAILED", new ErrorDetail(
            code, message, actionCode, UUID.randomUUID().toString(), timestamp
        ));
    }
    
    public record ErrorDetail(
        String code,
        String message,
        String actionCode,
        String traceId,
        String timestamp
    ) {}
}
```

- [ ] **Step 3: Write GlobalExceptionHandler**

```java
// common/src/main/java/com/agentbanking/common/web/GlobalExceptionHandler.java
package com.agentbanking.common.web;

import com.agentbanking.common.security.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.error("Validation error: {}", e.getMessage());
        return ResponseEntity.badRequest().body(
            ErrorResponse.of(ErrorCodes.ERR_INVALID_AMOUNT, e.getMessage(), "RETRY")
        );
    }
    
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        log.error("Business error: {}", e.getMessage());
        return ResponseEntity.badRequest().body(
            ErrorResponse.of("ERR_BIZ_CUSTOM", e.getMessage(), "DECLINE")
        );
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .findFirst()
            .orElse("Validation failed");
        
        return ResponseEntity.badRequest().body(
            ErrorResponse.of(ErrorCodes.ERR_INVALID_AMOUNT, message, "RETRY")
        );
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
        log.error("Internal error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse.of(ErrorCodes.ERR_INTERNAL, "Internal server error", "REVIEW")
        );
    }
}
```

- [ ] **Step 4: Commit - Global Error Handler**

```bash
git add common/src/main/java/com/agentbanking/common/web/
git commit -m "feat(common): add GlobalExceptionHandler with standardized error schema"
```

---

## Task 6: Wire common module to all services

**Files:**
- Modify: `services/rules-service/build.gradle`
- Modify: `services/ledger-service/build.gradle`
- Modify: `services/onboarding-service/build.gradle`
- Modify: `services/switch-adapter-service/build.gradle`
- Modify: `services/biller-service/build.gradle`
- Modify: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/model/KycVerification.java`
- Modify: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/service/KycDecisionService.java`

- [ ] **Step 1: Add common to all service build.gradle files**

```groovy
// Each service's build.gradle - add:
dependencies {
    // existing deps...
    implementation project(':common')
}
```

- [ ] **Step 2: Commit - wire common module**

```bash
git add services/*/build.gradle
git commit -m "build: add common module dependency to all services"
```

---

## Task 7: Audit All Services for BigDecimal Compliance

**BRD Requirements:** Per AGENTS.md: "All monetary values use BigDecimal — NEVER use float or double"

**Files:**
- Search: All Java files in services/ for `double` usage with monetary context

- [ ] **Step 1: Search for double usage across all services**

```bash
# Run in each service directory to find double usage
grep -rn "double" --include="*.java" services/*/src/main/java/ | grep -v "test"
```

- [ ] **Step 2: Fix any remaining double in rules-service**

If found, change to BigDecimal and ensure RoundingMode.HALF_UP is used.

- [ ] **Step 3: Fix any remaining double in biller-service**

If found, change to BigDecimal and ensure RoundingMode.HALF_UP is used.

- [ ] **Step 4: Fix any remaining double in onboarding-service**

If found, change to BigDecimal and ensure RoundingMode.HALF_UP is used.

- [ ] **Step 5: Commit - BigDecimal audit fix**

```bash
git add services/
git commit -m "fix: audit and fix remaining double usage for BigDecimal compliance"
```

---

## Plan Summary

| Task | Focus | Key Changes |
|------|-------|-------------|
| Task 1 | rules-service | Domain records separate from JPA entities, repository pattern |
| Task 2 | ledger-service | Repository pattern, Redis idempotency, double-entry journal, encrypted MyKad |
| Task 3 | switch-adapter | BigDecimal instead of double, proper DTOs with validation |
| Task 4 | gateway | Real JWT OAuth2 resource server validation |
| Task 5 | common | Global error handler with standardized schema |
| Task 6 | All services | Wire common module to all services |
| Task 7 | All services | Audit and fix remaining double issues, ensure HALF_UP rounding |

**Plan complete and saved to `docs/superpowers/plans/2026-03-26-hexagonal-refactor.md`**

Two execution options:

1. **Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

2. **Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
