# Ledger & Float Service Implementation Plan (US-L01-L08) [DONE]

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## Status: COMPLETED (2026-03-27)

All tasks completed. Ledger service is fully operational with domain, infrastructure, and application layers.

**Goal:** Implement Ledger & Float Service for agent wallets, double-entry journals, real-time settlement, balance inquiry, cash withdrawal (EMV + PIN), cash deposit, and MyKad withdrawal per user stories US-L01 through US-L08.

**Architecture:** Hexagonal (Ports & Adapters) pattern. Database-per-service (ledger_db). PESSIMISTIC_WRITE locks on AgentFloat for concurrent balance updates. Idempotency via Redis (TTL 24h). Double-entry journal for audit.

**Tech Stack:** Java 21, Spring Boot 3.2.5, Spring Data JPA, PostgreSQL, Redis, Kafka, OpenFeign, Resilience4j, JUnit 5, Mockito, ArchUnit.

---

## Task 1: Domain Layer - Entities & Ports [DONE]

**BDD Scenarios:** BDD-L01, BDD-L02, BDD-L03, BDD-L04, BDD-W01, BDD-D01, BDD-V01  
**BRD Requirements:** US-L01, US-L02, US-L03, US-L04, US-L05, US-L06, US-L07, US-L08, US-V01  
**User-Facing:** NO  

**Files:**
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/model/TransactionType.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/model/TransactionStatus.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/model/EntryType.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/model/AgentFloat.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/model/Transaction.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/model/JournalEntry.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/port/out/AgentFloatRepository.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/port/out/TransactionRepository.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/port/out/JournalEntryRepository.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/port/out/IdempotencyCachePort.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/port/out/EventPublisherPort.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/LedgerService.java`

### Step 1: Create TransactionType enum

```java
package com.agentbanking.ledger.domain.model;

public enum TransactionType {
    CASH_WITHDRAWAL,
    CASH_DEPOSIT,
    BALANCE_INQUIRY,
    MYKAD_WITHDRAWAL,
    CARD_DEPOSIT
}
```

### Step 2: Create TransactionStatus enum

```java
package com.agentbanking.ledger.domain.model;

public enum TransactionStatus {
    PENDING,
    RESERVED,
    COMPLETED,
    FAILED,
    REVERSED
}
```

### Step 3: Create EntryType enum

```java
package com.agentbanking.ledger.domain.model;

public enum EntryType {
    DEBIT,
    CREDIT
}
```

### Step 4: Create AgentFloat entity

```java
package com.agentbanking.ledger.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AgentFloat {
    private final UUID floatId;
    private final UUID agentId;
    private BigDecimal balance;
    private BigDecimal reservedBalance;
    private final String currency; // Always "MYR"
    private Long version;
    private final Instant createdAt;
    private Instant updatedAt;

    public AgentFloat(UUID floatId, UUID agentId, BigDecimal balance,
                      BigDecimal reservedBalance, String currency,
                      Long version, Instant createdAt, Instant updatedAt) {
        this.floatId = floatId;
        this.agentId = agentId;
        this.balance = balance;
        this.reservedBalance = reservedBalance;
        this.currency = currency;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void reserveFloat(BigDecimal amount) {
        if (getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("ERR_INSUFFICIENT_FLOAT");
        }
        this.reservedBalance = this.reservedBalance.add(amount);
        this.updatedAt = Instant.now();
    }

    public void releaseReservedFloat(BigDecimal amount) {
        this.reservedBalance = this.reservedBalance.subtract(amount);
        this.updatedAt = Instant.now();
    }

    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
        this.reservedBalance = this.reservedBalance.subtract(amount);
        this.updatedAt = Instant.now();
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        this.updatedAt = Instant.now();
    }

    public BigDecimal getAvailableBalance() {
        return balance.subtract(reservedBalance);
    }

    public boolean hasSufficientBalance(BigDecimal amount) {
        return getAvailableBalance().compareTo(amount) >= 0;
    }

    // Getters...
}
```

### Step 5: Create Transaction entity

```java
package com.agentbanking.ledger.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Transaction {
    private final UUID transactionId;
    private final String idempotencyKey;
    private final UUID agentId;
    private final TransactionType transactionType;
    private final BigDecimal amount;
    private final BigDecimal customerFee;
    private final BigDecimal agentCommission;
    private final BigDecimal bankShare;
    private TransactionStatus status;
    private String errorCode;
    private String customerMykad;
    private String customerCardMasked;
    private String switchReference;
    private BigDecimal geofenceLat;
    private BigDecimal geofenceLng;
    private final Instant createdAt;
    private Instant completedAt;

    // Constructor, getters, status transition methods...
    public void markReserved() { this.status = TransactionStatus.RESERVED; }
    public void markCompleted(String switchRef) {
        this.status = TransactionStatus.COMPLETED;
        this.switchReference = switchRef;
        this.completedAt = Instant.now();
    }
    public void markFailed(String errorCode) {
        this.status = TransactionStatus.FAILED;
        this.errorCode = errorCode;
        this.completedAt = Instant.now();
    }
    public void markReversed() {
        this.status = TransactionStatus.REVERSED;
        this.completedAt = Instant.now();
    }
}
```

### Step 6: Create JournalEntry entity

```java
package com.agentbanking.ledger.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class JournalEntry {
    private final UUID journalId;
    private final UUID transactionId;
    private final EntryType entryType;
    private final String accountCode;
    private final BigDecimal amount;
    private final String description;
    private final Instant createdAt;

    // Constructor, getters...
}
```

### Step 7: Create outbound port interfaces

```java
package com.agentbanking.ledger.domain.port.out;

import com.agentbanking.ledger.domain.model.AgentFloat;
import java.util.Optional;
import java.util.UUID;

public interface AgentFloatRepository {
    Optional<AgentFloat> findByAgentId(UUID agentId);
    Optional<AgentFloat> findByAgentIdWithLock(UUID agentId);
    AgentFloat save(AgentFloat agentFloat);
}
```

```java
package com.agentbanking.ledger.domain.port.out;

import com.agentbanking.ledger.domain.model.Transaction;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository {
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    Optional<Transaction> findById(UUID transactionId);
    Transaction save(Transaction transaction);
}
```

```java
package com.agentbanking.ledger.domain.port.out;

import com.agentbanking.ledger.domain.model.JournalEntry;
import java.util.List;
import java.util.UUID;

public interface JournalEntryRepository {
    List<JournalEntry> findByTransactionId(UUID transactionId);
    JournalEntry save(JournalEntry journalEntry);
}
```

```java
package com.agentbanking.ledger.domain.port.out;

import java.util.Optional;

public interface IdempotencyCachePort {
    <T> Optional<T> get(String key, Class<T> type);
    <T> void set(String key, T value, long ttlSeconds);
}
```

```java
package com.agentbanking.ledger.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface EventPublisherPort {
    void publishTransactionCompleted(UUID transactionId, String txnType, BigDecimal amount);
    void publishTransactionFailed(UUID transactionId, String errorCode);
    void publishReversalCompleted(UUID transactionId);
}
```

### Step 8: Create LedgerService domain service

```java
package com.agentbanking.ledger.domain.service;

import com.agentbanking.ledger.domain.model.*;
import com.agentbanking.ledger.domain.port.out.AgentFloatRepository;
import java.math.BigDecimal;
import java.util.UUID;

public class LedgerService {

    private final AgentFloatRepository agentFloatRepository;

    public LedgerService(AgentFloatRepository agentFloatRepository) {
        this.agentFloatRepository = agentFloatRepository;
    }

    public AgentFloat reserveFloat(UUID agentId, BigDecimal amount) {
        AgentFloat agentFloat = agentFloatRepository.findByAgentIdWithLock(agentId)
                .orElseThrow(() -> new IllegalArgumentException("ERR_AGENT_FLOAT_NOT_FOUND"));
        agentFloat.reserveFloat(amount);
        return agentFloatRepository.save(agentFloat);
    }

    public AgentFloat debitFloat(UUID agentId, BigDecimal amount) {
        AgentFloat agentFloat = agentFloatRepository.findByAgentIdWithLock(agentId)
                .orElseThrow(() -> new IllegalArgumentException("ERR_AGENT_FLOAT_NOT_FOUND"));
        agentFloat.debit(amount);
        return agentFloatRepository.save(agentFloat);
    }

    public AgentFloat creditFloat(UUID agentId, BigDecimal amount) {
        AgentFloat agentFloat = agentFloatRepository.findByAgentIdWithLock(agentId)
                .orElseThrow(() -> new IllegalArgumentException("ERR_AGENT_FLOAT_NOT_FOUND"));
        agentFloat.credit(amount);
        return agentFloatRepository.save(agentFloat);
    }

    public AgentFloat releaseReservedFloat(UUID agentId, BigDecimal amount) {
        AgentFloat agentFloat = agentFloatRepository.findByAgentIdWithLock(agentId)
                .orElseThrow(() -> new IllegalArgumentException("ERR_AGENT_FLOAT_NOT_FOUND"));
        agentFloat.releaseReservedFloat(amount);
        return agentFloatRepository.save(agentFloat);
    }
}
```

### Step 9: Commit

```bash
git add services/ledger-service/src/main/java/com/agentbanking/ledger/domain/
git commit -m "feat(ledger): add domain entities, ports and ledger service"
```

---

## Task 2: Infrastructure Layer - JPA Adapters [DONE]

**BDD Scenarios:** BDD-L01, BDD-L02, BDD-L03  
**BRD Requirements:** US-L01, US-L02, US-L03  
**User-Facing:** NO  

**Files:**
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/entity/AgentFloatEntity.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/entity/TransactionEntity.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/entity/JournalEntryEntity.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/repository/AgentFloatJpaRepository.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/repository/TransactionJpaRepository.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/repository/JournalEntryJpaRepository.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/mapper/AgentFloatMapper.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/adapter/AgentFloatRepositoryAdapter.java`

### Step 1: Create JPA entities with PESSIMISTIC_WRITE lock support

### Step 2: Create mappers and repository adapters

### Step 3: Run tests and commit

```bash
./gradlew :ledger-service:test
git add services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/
git commit -m "feat(ledger): add JPA entities and repository adapters with PESSIMISTIC_WRITE"
```

---

## Task 3: Application Layer - Use Cases [DONE]

**BDD Scenarios:** BDD-W01, BDD-D01, BDD-L04, BDD-V01  
**BRD Requirements:** US-L05, US-L06, US-L07, US-L08, US-L04, US-V01  
**User-Facing:** YES (external API endpoints)  

**Files:**
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/port/in/ProcessWithdrawalUseCase.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/port/in/ProcessDepositUseCase.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/port/in/ProcessBalanceInquiryUseCase.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/port/in/ReverseTransactionUseCase.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/application/usecase/ProcessWithdrawalUseCaseImpl.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/application/usecase/ProcessDepositUseCaseImpl.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/application/usecase/ProcessBalanceInquiryUseCaseImpl.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/application/usecase/ReverseTransactionUseCaseImpl.java`

### Step 1: Create inbound port interfaces

```java
package com.agentbanking.ledger.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProcessWithdrawalUseCase {
    TransactionResult processWithdrawal(WithdrawalCommand command);

    record WithdrawalCommand(
        UUID agentId,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        String customerCardData,
        String customerPinBlock,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng
    ) {}

    record TransactionResult(
        String status,
        UUID transactionId,
        BigDecimal amount,
        BigDecimal customerFee,
        String referenceNumber
    ) {}
}
```

### Step 2: Create use case implementations with idempotency check

### Step 3: Wire Feign clients to Rules Service and Switch Adapter

### Step 4: Run tests and commit

```bash
./gradlew :ledger-service:test
git add services/ledger-service/src/main/java/com/agentbanking/ledger/domain/port/in/
git add services/ledger-service/src/main/java/com/agentbanking/ledger/application/
git commit -m "feat(ledger): add use cases for withdrawal, deposit, balance inquiry, reversal"
```

---

## Task 4: Infrastructure Layer - REST Controllers [DONE]

**BDD Scenarios:** BDD-W01, BDD-D01, BDD-L01, BDD-V01  
**BRD Requirements:** US-L05, US-L07, US-L01, US-V01  
**User-Facing:** YES (external API via Gateway)  

**Files:**
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/web/LedgerController.java`
- Create: DTOs for requests/responses
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/web/GlobalExceptionHandler.java`

### Step 1: Create REST controller with external API endpoints

### Step 2: Create GlobalExceptionHandler for error standardization

### Step 3: Run tests and commit

```bash
./gradlew :ledger-service:test
git add services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/web/
git commit -m "feat(ledger): add REST controllers for withdrawal, deposit, balance, reversal"
```

---

## Task 5: Infrastructure - Redis Idempotency & Kafka Events [DONE]

**BDD Scenarios:** BDD-L04-EC-02 (duplicate requests), BDD-W01-SMS  
**BRD Requirements:** US-L04, FR-2.4  
**User-Facing:** NO  

**Files:**
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/cache/RedisIdempotencyAdapter.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/messaging/KafkaEventPublisherAdapter.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/config/RedisConfig.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/config/KafkaConfig.java`

### Step 1: Create Redis idempotency adapter

### Step 2: Create Kafka event publisher adapter

### Step 3: Run tests and commit

```bash
./gradlew :ledger-service:test
git add services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/cache/
git add services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/messaging/
git commit -m "feat(ledger): add Redis idempotency adapter and Kafka event publisher"
```

---

## Task 6: Unit Tests [DONE]

**BDD Scenarios:** All BDD-L*, BDD-W*, BDD-D*, BDD-V* scenarios  
**BRD Requirements:** US-L01 through US-L08, US-V01  
**User-Facing:** NO  

**Files:**
- Create: `services/ledger-service/src/test/java/com/agentbanking/ledger/domain/service/LedgerServiceTest.java`
- Create: `services/ledger-service/src/test/java/com/agentbanking/ledger/application/usecase/ProcessWithdrawalUseCaseImplTest.java`
- Create: `services/ledger-service/src/test/java/com/agentbanking/ledger/application/usecase/ProcessDepositUseCaseImplTest.java`
- Create: `services/ledger-service/src/test/java/com/agentbanking/ledger/architecture/HexagonalArchitectureTest.java`

### Step 1: Write unit tests for LedgerService (reserve, debit, credit, release)

### Step 2: Write unit tests for use cases (idempotency, flow orchestration)

### Step 3: Write ArchUnit test

### Step 4: Run all tests and commit

```bash
./gradlew :ledger-service:test
git add services/ledger-service/src/test/
git commit -m "test(ledger): add unit tests and ArchUnit validation for Ledger Service"
```

---