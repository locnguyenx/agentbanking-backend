# Phase 2: Ledger & Float Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Phase 2 Ledger & Float Service — agent wallets, double-entry journals, real-time settlement, cash withdrawal, cash deposit, balance inquiry with PESSIMISTIC_WRITE locks, Redis idempotency, and Kafka events.

**Architecture:** Hexagonal (Ports & Adapters) with domain/application/infrastructure layers. Database-per-service (ledger_db). PESSIMISTIC_WRITE locks on AgentFloat for concurrent balance updates. Idempotency via Redis (TTL 24h). Double-entry journal for audit.

**Tech Stack:** Java 21, Spring Boot 3.x, Spring Cloud, PostgreSQL (Flyway), Redis (Spring Data Redis), Apache Kafka (Spring Cloud Stream), JUnit 5, Mockito, BigDecimal (HALF_UP, 2 decimal places).

---

## Task 1: Project Scaffolding

**BDD Scenarios:** N/A (infrastructure)
**BRD Requirements:** N/A
**User-Facing:** NO

**Files:**
- Create: `services/ledger-service/build.gradle`
- Create: `services/ledger-service/settings.gradle`
- Create: `services/ledger-service/src/main/resources/application.yaml`
- Create: `services/ledger-service/src/main/resources/db/migration/V1__init_ledger.sql`
- Create: `services/ledger-service/docs/openapi-internal.yaml`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/LedgerServiceApplication.java`

- [ ] **Step 1: Create Gradle build file**

```gradle
// services/ledger-service/build.gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.4' apply false
    id 'io.spring.dependency-management' version '1.1.4' apply false
}

allprojects {
    group = 'com.agentbanking'
    version = '1.0.0-SNAPSHOT'
    
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply plugin: 'java'
    
    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    dependencies {
        implementation 'org.springframework.boot:spring-boot-starter-web'
        implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
        implementation 'org.springframework.boot:spring-boot-starter-data-redis'
        implementation 'org.springframework.boot:spring-boot-starter-validation'
        implementation 'org.springframework.cloud:spring-cloud-starter-openfeign'
        implementation 'org.springframework.kafka:spring-kafka'
        implementation 'org.postgresql:postgresql'
        implementation 'org.flywaydb:flyway-core'
        implementation 'org.yaml:snakeyaml'
        
        compileOnly 'org.projectlombok:lombok'
        annotationProcessor 'org.projectlombok:lombok'
        
        testImplementation 'org.springframework.boot:spring-boot-starter-test'
        testImplementation 'org.springframework.kafka:spring-kafka-test'
        testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    }
    
    tasks.withType(Test) {
        useJUnitPlatform()
    }
}
```

- [ ] **Step 2: Create settings and application config**

```yaml
# services/ledger-service/src/main/resources/application.yaml
server:
  port: 8082

spring:
  application:
    name: ledger-service
  datasource:
    url: jdbc:postgresql://localhost:5432/ledger_db
    username: ${DB_USER:agentbanking}
    password: ${DB_PASSWORD:password}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 5000ms
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: ledger-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"

feign:
  client:
    config:
      default:
        connectTimeoutMillis: 5000
        readTimeoutMillis: 10000

resilience4j:
  circuitbreaker:
    registerHealthIndicator: true
    slidingWindowSize: 10
    permittedNumberOfCallsInHalfOpenState: 3
    slidingWindowSize: 100
    minimumNumberOfCalls: 20
    waitDurationInOpenState: 60s
    failureRateThreshold: 50

logging:
  level:
    com.agentbanking: DEBUG
    org.springframework: INFO
```

- [ ] **Step 3: Create Flyway migration for ledger tables**

```sql
-- services/ledger-service/src/main/resources/db/migration/V1__init_ledger.sql
-- Agent Float table
CREATE TABLE agent_float (
    float_id UUID PRIMARY KEY,
    agent_id UUID NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    reserved_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'MYR',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_float_agent FOREIGN KEY (agent_id) REFERENCES onboarding_db.agent(agent_id),
    CONSTRAINT chk_currency CHECK (currency = 'MYR'),
    CONSTRAINT chk_balance CHECK (balance >= 0),
    CONSTRAINT chk_reserved CHECK (reserved_balance >= 0)
);

CREATE INDEX idx_agent_float_agent_id ON agent_float(agent_id);

-- Transaction table
CREATE TABLE transaction (
    transaction_id UUID PRIMARY KEY,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    agent_id UUID NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    amount DECIMAL(15,2),
    customer_fee DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    agent_commission DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    bank_share DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL,
    error_code VARCHAR(20),
    customer_mykad VARCHAR(12),
    customer_card_masked VARCHAR(19),
    switch_reference VARCHAR(50),
    geofence_lat DECIMAL(9,6),
    geofence_lng DECIMAL(9,6),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT fk_transaction_agent FOREIGN KEY (agent_id) REFERENCES onboarding_db.agent(agent_id)
);

CREATE INDEX idx_transaction_idempotency_key ON transaction(idempotency_key);
CREATE INDEX idx_transaction_agent_id ON transaction(agent_id);
CREATE INDEX idx_transaction_status ON transaction(status);
CREATE INDEX idx_transaction_created_at ON transaction(created_at);

-- Journal Entry table (double-entry)
CREATE TABLE journal_entry (
    journal_id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    entry_type VARCHAR(10) NOT NULL,
    account_code VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    description VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_journal_entry_transaction FOREIGN KEY (transaction_id) REFERENCES transaction(transaction_id),
    CONSTRAINT chk_entry_type CHECK (entry_type IN ('DEBIT', 'CREDIT'))
);

CREATE INDEX idx_journal_entry_transaction_id ON journal_entry(transaction_id);
CREATE INDEX idx_journal_entry_account_code ON journal_entry(account_code);

-- Idempotency cache (Redis key pattern handled in application)
```

- [ ] **Step 4: Create main application class**

```java
package com.agentbanking.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableFeignClients
@EnableKafka
public class LedgerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LedgerServiceApplication.class, args);
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add services/ledger-service/
git commit -m "feat: scaffold ledger-service project structure"
```

---

## Task 2: Domain Layer — Entities & Ports

**BDD Scenarios:** BDD-L01, BDD-L02, BDD-L03 (real-time settlement), BDD-W01, BDD-D01
**BRD Requirements:** FR-2.1, FR-2.2, FR-2.3, US-L01, US-L02, US-L03, US-L05, US-L07
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

- [ ] **Step 1: Create enums**

```java
package com.agentbanking.ledger.domain.model;

public enum TransactionType {
    CASH_WITHDRAWAL,
    CASH_DEPOSIT,
    BALANCE_INQUIRY,
    DUTNOW_TRANSFER,
    BILL_PAYMENT,
    PREPAID_TOPUP
}
```

```java
package com.agentbanking.ledger.domain.model;

public enum TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REVERSED
}
```

```java
package com.agentbanking.ledger.domain.model;

public enum EntryType {
    DEBIT,
    CREDIT
}
```

- [ ] **Step 2: Create AgentFloat entity**

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
    private final Long version;
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

    public UUID getFloatId() { return floatId; }
    public UUID getAgentId() { return agentId; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getReservedBalance() { return reservedBalance; }
    public String getCurrency() { return currency; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    
    public BigDecimal getAvailableBalance() {
        return balance.subtract(reservedBalance);
    }

    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
        this.updatedAt = Instant.now();
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        this.updatedAt = Instant.now();
    }

    public boolean hasSufficientBalance(BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }
}
```

- [ ] **Step 3: Create Transaction entity**

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
    private final String errorCode;
    private final String customerMykad;
    private final String customerCardMasked;
    private final String switchReference;
    private final BigDecimal geofenceLat;
    private final BigDecimal geofenceLng;
    private final Instant createdAt;
    private Instant completedAt;

    public Transaction(UUID transactionId, String idempotencyKey, UUID agentId,
                       TransactionType transactionType, BigDecimal amount,
                       BigDecimal customerFee, BigDecimal agentCommission,
                       BigDecimal bankShare, TransactionStatus status,
                       String errorCode, String customerMykad, String customerCardMasked,
                       String switchReference, BigDecimal geofenceLat, BigDecimal geofenceLng,
                       Instant createdAt, Instant completedAt) {
        this.transactionId = transactionId;
        this.idempotencyKey = idempotencyKey;
        this.agentId = agentId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.customerFee = customerFee;
        this.agentCommission = agentCommission;
        this.bankShare = bankShare;
        this.status = status;
        this.errorCode = errorCode;
        this.customerMykad = customerMykad;
        this.customerCardMasked = customerCardMasked;
        this.switchReference = switchReference;
        this.geofenceLat = geofenceLat;
        this.geofenceLng = geofenceLng;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public UUID getTransactionId() { return transactionId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public UUID getAgentId() { return agentId; }
    public TransactionType getTransactionType() { return transactionType; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getCustomerFee() { return customerFee; }
    public BigDecimal getAgentCommission() { return agentCommission; }
    public BigDecimal getBankShare() { return bankShare; }
    public TransactionStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
    public String getCustomerMykad() { return customerMykad; }
    public String getCustomerCardMasked() { return customerCardMasked; }
    public String getSwitchReference() { return switchReference; }
    public BigDecimal getGeofenceLat() { return geofenceLat; }
    public BigDecimal getGeofenceLng() { return geofenceLng; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }

    public void markCompleted() {
        this.status = TransactionStatus.COMPLETED;
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

- [ ] **Step 4: Create JournalEntry entity**

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

    public JournalEntry(UUID journalId, UUID transactionId, EntryType entryType,
                        String accountCode, BigDecimal amount, String description,
                        Instant createdAt) {
        this.journalId = journalId;
        this.transactionId = transactionId;
        this.entryType = entryType;
        this.accountCode = accountCode;
        this.amount = amount;
        this.description = description;
        this.createdAt = createdAt;
    }

    public UUID getJournalId() { return journalId; }
    public UUID getTransactionId() { return transactionId; }
    public EntryType getEntryType() { return entryType; }
    public String getAccountCode() { return accountCode; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 5: Create outbound port interfaces**

```java
package com.agentbanking.ledger.domain.port.out;

import com.agentbanking.ledger.domain.model.AgentFloat;
import java.util.Optional;
import java.util.UUID;

public interface AgentFloatRepository {
    Optional<AgentFloat> findByAgentId(UUID agentId);
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
    void delete(String key);
}
```

```java
package com.agentbanking.ledger.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface EventPublisherPort {
    void publishTransactionCompleted(UUID transactionId, BigDecimal amount);
    void publishTransactionFailed(UUID transactionId, String errorCode);
    void publishReversalCompleted(UUID transactionId);
}
```

- [ ] **Step 6: Commit**

```bash
git add services/ledger-service/src/main/java/com/agentbanking/ledger/domain/
git commit -m "feat: add domain entities and outbound ports"
```

---

## Task 3: Infrastructure — JPA Adapters

**BDD Scenarios:** BDD-L01, BDD-L02, BDD-L03
**BRD Requirements:** FR-2.1, FR-2.2
**User-Facing:** NO

**Files:**
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/entity/AgentFloatEntity.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/entity/TransactionEntity.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/entity/JournalEntryEntity.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/repository/AgentFloatJpaRepository.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/repository/TransactionJpaRepository.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/repository/JournalEntryJpaRepository.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/adapter/AgentFloatRepositoryAdapter.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/adapter/TransactionRepositoryAdapter.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/adapter/JournalEntryRepositoryAdapter.java`
- Modify: `services/ledger-service/src/main/java/com/agentbanking/ledger/LedgerServiceApplication.java`

- [ ] **Step 1: Create JPA entity for AgentFloat**

```java
package com.agentbanking.ledger.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_float")
public class AgentFloatEntity {
    @Id
    @Column(name = "float_id")
    private UUID floatId;
    
    @Column(name = "agent_id", nullable = false)
    private UUID agentId;
    
    @Column(name = "balance", nullable = false)
    private BigDecimal balance;
    
    @Column(name = "reserved_balance", nullable = false)
    private BigDecimal reservedBalance;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getFloatId() { return floatId; }
    public void setFloatId(UUID floatId) { this.floatId = floatId; }
    public UUID getAgentId() { return agentId; }
    public void setAgentId(UUID agentId) { this.agentId = agentId; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public BigDecimal getReservedBalance() { return reservedBalance; }
    public void setReservedBalance(BigDecimal reservedBalance) { this.reservedBalance = reservedBalance; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 2: Create JPA entity for Transaction**

```java
package com.agentbanking.ledger.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transaction")
public class TransactionEntity {
    @Id
    @Column(name = "transaction_id")
    private UUID transactionId;
    
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;
    
    @Column(name = "agent_id", nullable = false)
    private UUID agentId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;
    
    @Column(name = "amount")
    private BigDecimal amount;
    
    @Column(name = "customer_fee", nullable = false)
    private BigDecimal customerFee;
    
    @Column(name = "agent_commission", nullable = false)
    private BigDecimal agentCommission;
    
    @Column(name = "bank_share", nullable = false)
    private BigDecimal bankShare;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;
    
    @Column(name = "error_code")
    private String errorCode;
    
    @Column(name = "customer_mykad")
    private String customerMykad;
    
    @Column(name = "customer_card_masked")
    private String customerCardMasked;
    
    @Column(name = "switch_reference")
    private String switchReference;
    
    @Column(name = "geofence_lat", precision = 9, scale = 6)
    private BigDecimal geofenceLat;
    
    @Column(name = "geofence_lng", precision = 9, scale = 6)
    private BigDecimal geofenceLng;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    // Getters and setters...
}
```

- [ ] **Step 3: Create JPA entity for JournalEntry**

```java
package com.agentbanking.ledger.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "journal_entry")
public class JournalEntryEntity {
    @Id
    @Column(name = "journal_id")
    private UUID journalId;
    
    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private EntryType entryType;
    
    @Column(name = "account_code", nullable = false)
    private String accountCode;
    
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
    
    @Column(name = "description", nullable = false)
    private String description;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    // Getters and setters...
}
```

- [ ] **Step 4: Create Spring Data JPA repositories**

```java
package com.agentbanking.ledger.infrastructure.persistence.repository;

import com.agentbanking.ledger.infrastructure.persistence.entity.AgentFloatEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface AgentFloatJpaRepository extends JpaRepository<AgentFloatEntity, UUID> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT af FROM AgentFloatEntity af WHERE af.agentId = :agentId")
    Optional<AgentFloatEntity> findByAgentIdWithLock(@Param("agentId") UUID agentId);
    
    Optional<AgentFloatEntity> findByAgentId(UUID agentId);
}
```

```java
package com.agentbanking.ledger.infrastructure.persistence.repository;

import com.agentbanking.ledger.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, UUID> {
    Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey);
}
```

```java
package com.agentbanking.ledger.infrastructure.persistence.repository;

import com.agentbanking.ledger.infrastructure.persistence.entity.JournalEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface JournalEntryJpaRepository extends JpaRepository<JournalEntryEntity, UUID> {
    List<JournalEntryEntity> findByTransactionId(UUID transactionId);
}
```

- [ ] **Step 5: Create mappers and adapters**

```java
// services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/mapper/AgentFloatMapper.java
package com.agentbanking.ledger.infrastructure.persistence.mapper;

import com.agentbanking.ledger.domain.model.AgentFloat;
import com.agentbanking.ledger.infrastructure.persistence.entity.AgentFloatEntity;
import java.util.UUID;

public class AgentFloatMapper {
    public static AgentFloat toDomain(AgentFloatEntity entity) {
        if (entity == null) return null;
        return new AgentFloat(
            entity.getFloatId(),
            entity.getAgentId(),
            entity.getBalance(),
            entity.getReservedBalance(),
            entity.getCurrency(),
            entity.getVersion(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
    
    public static AgentFloatEntity toEntity(AgentFloat domain) {
        if (domain == null) return null;
        AgentFloatEntity entity = new AgentFloatEntity();
        entity.setFloatId(domain.getFloatId());
        entity.setAgentId(domain.getAgentId());
        entity.setBalance(domain.getBalance());
        entity.setReservedBalance(domain.getReservedBalance());
        entity.setCurrency(domain.getCurrency());
        entity.setVersion(domain.getVersion());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/
git commit -m "feat: add JPA entities and repositories"
```

---

## Task 4: Infrastructure — Redis Idempotency Adapter & Kafka Events

**BDD Scenarios:** BDD-W01-EC-07, BDD-L04-EC-02 (duplicate requests)
**BRD Requirements:** FR-2.4
**User-Facing:** NO

**Files:**
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/cache/RedisIdempotencyAdapter.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/messaging/KafkaEventPublisherAdapter.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/config/RedisConfig.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/config/KafkaConfig.java`

- [ ] **Step 1: Create Redis idempotency adapter**

```java
package com.agentbanking.ledger.infrastructure.cache;

import com.agentbanking.ledger.domain.port.out.IdempotencyCachePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class RedisIdempotencyAdapter implements IdempotencyCachePort {
    
    private static final long TTL_24_HOURS = 24 * 60 * 60;
    
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    public RedisIdempotencyAdapter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(value, type));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }
    
    @Override
    public <T> void set(String key, T value, long ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttlSeconds, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to cache response", e);
        }
    }
    
    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
```

- [ ] **Step 2: Create Kafka event publisher adapter**

```java
package com.agentbanking.ledger.infrastructure.messaging;

import com.agentbanking.ledger.domain.port.out.EventPublisherPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;

@Component
public class KafkaEventPublisherAdapter implements EventPublisherPort {
    
    private static final String TRANSACTION_TOPIC = "agentbanking.transactions";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public KafkaEventPublisherAdapter(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    @Override
    public void publishTransactionCompleted(UUID transactionId, BigDecimal amount) {
        kafkaTemplate.send(TRANSACTION_TOPIC, transactionId.toString(), new TransactionEvent(
            transactionId, "COMPLETED", amount, null
        ));
    }
    
    @Override
    public void publishTransactionFailed(UUID transactionId, String errorCode) {
        kafkaTemplate.send(TRANSACTION_TOPIC, transactionId.toString(), new TransactionEvent(
            transactionId, "FAILED", null, errorCode
        ));
    }
    
    @Override
    public void publishReversalCompleted(UUID transactionId) {
        kafkaTemplate.send(TRANSACTION_TOPIC, transactionId.toString(), new TransactionEvent(
            transactionId, "REVERSED", null, null
        ));
    }
    
    public record TransactionEvent(UUID transactionId, String status, 
                                   BigDecimal amount, String errorCode) {}
}
```

- [ ] **Step 3: Commit**

```bash
git add services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/cache/
git add services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/messaging/
git commit -m "feat: add Redis idempotency and Kafka event publisher"
```

---

## Task 5: Domain Service — Balance Management

**BDD Scenarios:** BDD-L01, BDD-L01-EC-01, BDD-L01-EC-02, BDD-L03, BDD-L03-EC-01
**BRD Requirements:** FR-2.1, FR-2.3, US-L01, US-L03
**User-Facing:** YES (agent balance check via GET /api/v1/agent/balance)

**Files:**
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/BalanceManagementService.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/IdempotencyService.java`

- [ ] **Step 1: Write failing test for balance retrieval**

```java
package com.agentbanking.ledger.domain.service;

import com.agentbanking.ledger.domain.model.AgentFloat;
import com.agentbanking.ledger.domain.port.out.AgentFloatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceManagementServiceTest {
    
    @Mock
    private AgentFloatRepository agentFloatRepository;
    
    @InjectMocks
    private BalanceManagementService balanceManagementService;
    
    @Test
    void getAgentBalance_shouldReturnBalance_whenFloatExists() {
        UUID agentId = UUID.randomUUID();
        AgentFloat floatData = new AgentFloat(
            UUID.randomUUID(), agentId, new BigDecimal("10000.00"),
            new BigDecimal("0.00"), "MYR", 0L, null, null
        );
        when(agentFloatRepository.findByAgentId(agentId)).thenReturn(Optional.of(floatData));
        
        var result = balanceManagementService.getAgentBalance(agentId);
        
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("10000.00"), result.get().getBalance());
    }
    
    @Test
    void getAgentBalance_shouldReturnEmpty_whenFloatNotFound() {
        UUID agentId = UUID.randomUUID();
        when(agentFloatRepository.findByAgentId(agentId)).thenReturn(Optional.empty());
        
        var result = balanceManagementService.getAgentBalance(agentId);
        
        assertTrue(result.isEmpty());
    }
    
    @Test
    void getAvailableBalance_shouldReturnBalanceMinusReserved() {
        UUID agentId = UUID.randomUUID();
        AgentFloat floatData = new AgentFloat(
            UUID.randomUUID(), agentId, new BigDecimal("10000.00"),
            new BigDecimal("500.00"), "MYR", 0L, null, null
        );
        
        BigDecimal available = balanceManagementService.getAvailableBalance(floatData);
        
        assertEquals(new BigDecimal("9500.00"), available);
    }
}
```

- [ ] **Step 2: Implement BalanceManagementService**

```java
package com.agentbanking.ledger.domain.service;

import com.agentbanking.ledger.domain.model.AgentFloat;
import com.agentbanking.ledger.domain.port.out.AgentFloatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class BalanceManagementService {
    
    private static final Logger log = LoggerFactory.getLogger(BalanceManagementService.class);
    
    private final AgentFloatRepository agentFloatRepository;
    
    public BalanceManagementService(AgentFloatRepository agentFloatRepository) {
        this.agentFloatRepository = agentFloatRepository;
    }
    
    public Optional<AgentFloat> getAgentBalance(UUID agentId) {
        log.info("Retrieving balance for agent: {}", agentId);
        return agentFloatRepository.findByAgentId(agentId);
    }
    
    public BigDecimal getAvailableBalance(AgentFloat floatData) {
        return floatData.getBalance().subtract(floatData.getReservedBalance());
    }
}
```

- [ ] **Step 3: Run test and verify it passes**

```bash
./gradlew :ledger-service:test --tests "*BalanceManagementServiceTest" -v
```

- [ ] **Step 4: Write failing test for debit operation**

```java
@Test
void debitAgentFloat_shouldDeductBalance_whenSufficientFunds() {
    UUID agentId = UUID.randomUUID();
    AgentFloat floatData = new AgentFloat(
        UUID.randomUUID(), agentId, new BigDecimal("10000.00"),
        new BigDecimal("0.00"), "MYR", 0L, null, null
    );
    when(agentFloatRepository.findByAgentIdWithLock(agentId)).thenReturn(Optional.of(floatData));
    when(agentFloatRepository.save(any())).thenReturn(floatData);
    
    balanceManagementService.debitAgentFloat(agentId, new BigDecimal("500.00"));
    
    // Verify balance was debited
}
```

- [ ] **Step 5: Run test and verify failures, then implement**

- [ ] **Step 6: Write failing test for credit operation**

- [ ] **Step 7: Implement with PESSIMISTIC_WRITE lock**

```java
public void debitAgentFloat(UUID agentId, BigDecimal amount) {
    log.info("Debiting agent float: agentId={}, amount={}", agentId, amount);
    
    AgentFloat floatData = agentFloatRepository.findByAgentIdWithLock(agentId)
        .orElseThrow(() -> new IllegalArgumentException("ERR_AGENT_FLOAT_NOT_FOUND"));
    
    if (!floatData.hasSufficientBalance(amount)) {
        throw new IllegalArgumentException("ERR_INSUFFICIENT_FLOAT");
    }
    
    floatData.debit(amount);
    agentFloatRepository.save(floatData);
}
```

- [ ] **Step 8: Commit**

```bash
git add services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/
git commit -m "feat: add balance management service with PESSIMISTIC_WRITE"
```

---

## Task 6: Application Layer — Use Cases (Withdrawal, Deposit, Balance)

**BDD Scenarios:** BDD-W01, BDD-W01-EC-07, BDD-D01, BDD-D01-EC-01, BDD-L04
**BRD Requirements:** FR-2.4, FR-3.1, FR-3.5, FR-4.1, FR-4.3, FR-5.1
**User-Facing:** YES

**Files:**
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/application/usecase/ProcessWithdrawalUseCase.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/application/usecase/ProcessDepositUseCase.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/application/usecase/ProcessBalanceInquiryUseCase.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/application/dto/WithdrawalRequest.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/application/dto/DepositRequest.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/application/dto/BalanceInquiryRequest.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/application/dto/TransactionResponse.java`

- [ ] **Step 1: Create DTOs**

```java
package com.agentbanking.ledger.application.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record WithdrawalRequest(
    @NotNull @Positive
    BigDecimal amount,
    @NotBlank
    String cardData,
    @NotBlank
    String pinBlock,
    @NotBlank @Size(min = 3, max = 3)
    String currency
) {}
```

```java
package com.agentbanking.ledger.application.dto;

import jakarta.validation.constraints.*;

public record DepositRequest(
    @NotNull @Positive
    BigDecimal amount,
    @NotBlank
    String destinationAccount,
    @NotBlank @Size(min = 3, max = 3)
    String currency
) {}
```

```java
package com.agentbanking.ledger.application.dto;

import jakarta.validation.constraints.*;

public record BalanceInquiryRequest(
    String cardData,
    String pinBlock
) {}
```

```java
package com.agentbanking.ledger.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
    String status,
    UUID transactionId,
    BigDecimal amount,
    BigDecimal customerFee,
    String referenceNumber,
    Instant timestamp
) {}
```

- [ ] **Step 2: Write failing test for withdrawal use case**

```java
@ExtendWith(MockitoExtension.class)
class ProcessWithdrawalUseCaseTest {
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private IdempotencyCachePort idempotencyCache;
    
    @Mock
    private RulesServiceClient rulesServiceClient;
    
    @Mock
    private SwitchAdapterClient switchAdapterClient;
    
    @Mock
    private EventPublisherPort eventPublisher;
    
    @InjectMocks
    private ProcessWithdrawalUseCase processWithdrawalUseCase;
    
    @Test
    void processWithdrawal_shouldReturnCachedResponse_whenIdempotencyKeyExists() {
        // Given idempotency key exists in Redis
        // When process withdrawal
        // Then return cached response, don't process again
    }
    
    @Test
    void processWithdrawal_shouldCreateTransaction_whenIdempotencyKeyNew() {
        // Given new idempotency key
        // When process withdrawal
        // Then create Transaction, debit float, create journal entries
    }
}
```

- [ ] **Step 3: Implement ProcessWithdrawalUseCase**

```java
package com.agentbanking.ledger.application.usecase;

import com.agentbanking.ledger.application.dto.*;
import com.agentbanking.ledger.domain.model.*;
import com.agentbanking.ledger.domain.port.out.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ProcessWithdrawalUseCase {
    
    private static final Logger log = LoggerFactory.getLogger(ProcessWithdrawalUseCase.class);
    private static final String ACCOUNT_CODE_AGENT_FLOAT = "AGT_FLOAT";
    private static final String ACCOUNT_CODE_BANK_SETTLEMENT = "BANK_SETTLE";
    
    private final TransactionRepository transactionRepository;
    private final AgentFloatRepository agentFloatRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final IdempotencyCachePort idempotencyCache;
    private final EventPublisherPort eventPublisher;
    
    public ProcessWithdrawalUseCase(
            TransactionRepository transactionRepository,
            AgentFloatRepository agentFloatRepository,
            JournalEntryRepository journalEntryRepository,
            IdempotencyCachePort idempotencyCache,
            EventPublisherPort eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.agentFloatRepository = agentFloatRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.idempotencyCache = idempotencyCache;
        this.eventPublisher = eventPublisher;
    }
    
    public TransactionResponse execute(UUID agentId, String idempotencyKey, WithdrawalRequest request) {
        log.info("Processing withdrawal: agentId={}, amount={}", agentId, request.amount());
        
        // Step 1: Check idempotency
        Optional<TransactionResponse> cached = idempotencyCache.get(
            "withdrawal:" + idempotencyKey, TransactionResponse.class);
        if (cached.isPresent()) {
            log.info("Returning cached response for idempotency key: {}", idempotencyKey);
            return cached.get();
        }
        
        // Step 2: Check agent float with PESSIMISTIC_WRITE lock
        AgentFloat floatData = agentFloatRepository.findByAgentIdWithLock(agentId)
            .orElseThrow(() -> new IllegalArgumentException("ERR_AGENT_FLOAT_NOT_FOUND"));
        
        if (!floatData.hasSufficientBalance(request.amount())) {
            throw new IllegalArgumentException("ERR_INSUFFICIENT_FLOAT");
        }
        
        // Step 3: Create transaction
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
            transactionId, idempotencyKey, agentId,
            TransactionType.CASH_WITHDRAWAL, request.amount(),
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            TransactionStatus.COMPLETED, null, null, null,
            null, null, null, Instant.now(), Instant.now()
        );
        transactionRepository.save(transaction);
        
        // Step 4: Debit agent float
        floatData.debit(request.amount());
        agentFloatRepository.save(floatData);
        
        // Step 5: Create double-entry journal entries
        JournalEntry debitEntry = new JournalEntry(
            UUID.randomUUID(), transactionId, EntryType.DEBIT,
            ACCOUNT_CODE_AGENT_FLOAT, request.amount(),
            "Debit agent float for withdrawal", Instant.now()
        );
        JournalEntry creditEntry = new JournalEntry(
            UUID.randomUUID(), transactionId, EntryType.CREDIT,
            ACCOUNT_CODE_BANK_SETTLEMENT, request.amount(),
            "Credit bank settlement account", Instant.now()
        );
        journalEntryRepository.save(debitEntry);
        journalEntryRepository.save(creditEntry);
        
        // Step 6: Cache response for idempotency
        TransactionResponse response = new TransactionResponse(
            "COMPLETED", transactionId, request.amount(),
            BigDecimal.ZERO, "TXN-" + transactionId, Instant.now()
        );
        idempotencyCache.set("withdrawal:" + idempotencyKey, response, 86400);
        
        // Step 7: Publish Kafka event
        eventPublisher.publishTransactionCompleted(transactionId, request.amount());
        
        return response;
    }
}
```

- [ ] **Step 4: Implement ProcessDepositUseCase**

```java
@Service
@Transactional
public class ProcessDepositUseCase {
    
    private static final Logger log = LoggerFactory.getLogger(ProcessDepositUseCase.class);
    
    private final TransactionRepository transactionRepository;
    private final AgentFloatRepository agentFloatRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final IdempotencyCachePort idempotencyCache;
    private final EventPublisherPort eventPublisher;
    
    public TransactionResponse execute(UUID agentId, String idempotencyKey, DepositRequest request) {
        log.info("Processing deposit: agentId={}, amount={}", agentId, request.amount());
        
        Optional<TransactionResponse> cached = idempotencyCache.get(
            "deposit:" + idempotencyKey, TransactionResponse.class);
        if (cached.isPresent()) {
            return cached.get();
        }
        
        AgentFloat floatData = agentFloatRepository.findByAgentIdWithLock(agentId)
            .orElseThrow(() -> new IllegalArgumentException("ERR_AGENT_FLOAT_NOT_FOUND"));
        
        floatData.credit(request.amount());
        agentFloatRepository.save(floatData);
        
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
            transactionId, idempotencyKey, agentId,
            TransactionType.CASH_DEPOSIT, request.amount(),
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            TransactionStatus.COMPLETED, null, null, null,
            null, null, null, Instant.now(), Instant.now()
        );
        transactionRepository.save(transaction);
        
        TransactionResponse response = new TransactionResponse(
            "COMPLETED", transactionId, request.amount(),
            BigDecimal.ZERO, "DEP-" + transactionId, Instant.now()
        );
        idempotencyCache.set("deposit:" + idempotencyKey, response, 86400);
        
        eventPublisher.publishTransactionCompleted(transactionId, request.amount());
        
        return response;
    }
}
```

- [ ] **Step 5: Implement ProcessBalanceInquiryUseCase**

```java
@Service
@Transactional(readOnly = true)
public class ProcessBalanceInquiryUseCase {
    
    private final BalanceManagementService balanceManagementService;
    
    public ProcessBalanceInquiryUseCase(BalanceManagementService balanceManagementService) {
        this.balanceManagementService = balanceManagementService;
    }
    
    public AgentBalanceResponse execute(UUID agentId) {
        log.info("Processing balance inquiry for agent: {}", agentId);
        
        AgentFloat floatData = balanceManagementService.getAgentBalance(agentId)
            .orElseThrow(() -> new IllegalArgumentException("ERR_AGENT_FLOAT_NOT_FOUND"));
        
        return new AgentBalanceResponse(
            "COMPLETED",
            floatData.getBalance(),
            floatData.getReservedBalance(),
            balanceManagementService.getAvailableBalance(floatData),
            "MYR",
            Instant.now()
        );
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add services/ledger-service/src/main/java/com/agentbanking/ledger/application/
git commit -m "feat: add application use cases (withdrawal, deposit, balance)"
```

---

## Task 7: Controller Layer — REST Endpoints

**BDD Scenarios:** BDD-W01, BDD-D01, BDD-L01
**BRD Requirements:** FR-2.1, FR-3.1, FR-4.1, FR-5.1, FR-5.2
**User-Facing:** YES

**Files:**
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/web/controller/WithdrawalController.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/web/controller/DepositController.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/web/controller/BalanceController.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/web/dto/AgentBalanceResponse.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/web/GlobalExceptionHandler.java`

- [ ] **Step 1: Create controllers**

```java
package com.agentbanking.ledger.infrastructure.web.controller;

import com.agentbanking.ledger.application.dto.*;
import com.agentbanking.ledger.application.usecase.ProcessWithdrawalUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class WithdrawalController {
    
    private final ProcessWithdrawalUseCase processWithdrawalUseCase;
    
    public WithdrawalController(ProcessWithdrawalUseCase processWithdrawalUseCase) {
        this.processWithdrawalUseCase = processWithdrawalUseCase;
    }
    
    @PostMapping("/withdrawal")
    public ResponseEntity<TransactionResponse> withdraw(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-Agent-Id") UUID agentId,
            @Valid @RequestBody WithdrawalRequest request) {
        return ResponseEntity.ok(processWithdrawalUseCase.execute(agentId, idempotencyKey, request));
    }
}
```

```java
package com.agentbanking.ledger.infrastructure.web.controller;

import com.agentbanking.ledger.application.dto.*;
import com.agentbanking.ledger.application.usecase.ProcessDepositUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class DepositController {
    
    private final ProcessDepositUseCase processDepositUseCase;
    
    public DepositController(ProcessDepositUseCase processDepositUseCase) {
        this.processDepositUseCase = processDepositUseCase;
    }
    
    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-Agent-Id") UUID agentId,
            @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(processDepositUseCase.execute(agentId, idempotencyKey, request));
    }
}
```

```java
package com.agentbanking.ledger.infrastructure.web.controller;

import com.agentbanking.ledger.infrastructure.web.dto.AgentBalanceResponse;
import com.agentbanking.ledger.application.usecase.ProcessBalanceInquiryUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class BalanceController {
    
    private final ProcessBalanceInquiryUseCase processBalanceInquiryUseCase;
    
    public BalanceController(ProcessBalanceInquiryUseCase processBalanceInquiryUseCase) {
        this.processBalanceInquiryUseCase = processBalanceInquiryUseCase;
    }
    
    @GetMapping("/agent/balance")
    public ResponseEntity<AgentBalanceResponse> getAgentBalance(
            @RequestHeader("X-Agent-Id") UUID agentId) {
        return ResponseEntity.ok(processBalanceInquiryUseCase.execute(agentId));
    }
    
    @PostMapping("/balance-inquiry")
    public ResponseEntity<?> customerBalanceInquiry() {
        // Handled by switch adapter for card-based inquiry
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 2: Create global exception handler**

```java
package com.agentbanking.ledger.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Validation error: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of(
            "status", "FAILED",
            "error", Map.of(
                "code", ex.getMessage(),
                "message", ex.getMessage(),
                "action_code", "DECLINE",
                "trace_id", "",
                "timestamp", Instant.now().toString()
            )
        ));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "status", "FAILED",
            "error", Map.of(
                "code", "ERR_INTERNAL",
                "message", "Internal server error",
                "action_code", "RETRY",
                "trace_id", "",
                "timestamp", Instant.now().toString()
            )
        ));
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/web/
git commit -m "feat: add REST controllers and exception handler"
```

---

## Task 8: Internal API Client — Rules Service Integration

**BDD Scenarios:** BDD-W01 (calls Rules for fee calculation)
**BRD Requirements:** US-R01, US-R02, US-R03, US-R04
**User-Facing:** NO

**Files:**
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/external/RulesServiceClient.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/external/dto/FeeCheckRequest.java`
- Create: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/external/dto/FeeCheckResponse.java`

- [ ] **Step 1: Create Feign client for Rules Service**

```java
package com.agentbanking.ledger.infrastructure.external;

import com.agentbanking.ledger.infrastructure.external.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "rules-service", url = "${feign.client.rules-service.url:http://localhost:8081}")
public interface RulesServiceClient {
    
    @PostMapping("/internal/v1/check-fee")
    FeeCheckResponse checkFee(@RequestBody FeeCheckRequest request);
    
    @PostMapping("/internal/v1/check-limits")
    LimitCheckResponse checkLimits(@RequestBody LimitCheckRequest request);
}
```

- [ ] **Step 2: Update use cases to call Rules Service**

Modify `ProcessWithdrawalUseCase.execute()` to call Rules service for fee calculation.

- [ ] **Step 3: Commit**

```bash
git add services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/external/
git commit -m "feat: add Rules Service integration for fee/limit checks"
```

---

## Task 9: Integration Tests

**BDD Scenarios:** BDD-L01 through BDD-L05, BDD-W01, BDD-W01-EC-07, BDD-D01, BDD-D01-EC-01 through BDD-D01-EC-04
**BRD Requirements:** FR-2.1, FR-2.2, FR-2.3, FR-2.4, FR-3.1, FR-3.5, FR-4.1, FR-4.3
**User-Facing:** YES (API tests)

**Files:**
- Create: `services/ledger-service/src/test/resources/application-test.yaml`
- Create: `services/ledger-service/src/test/java/com/agentbanking/ledger/integration/WithdrawalIntegrationTest.java`
- Create: `services/ledger-service/src/test/java/com/agentbanking/ledger/integration/DepositIntegrationTest.java`
- Create: `services/ledger-service/src/test/java/com/agentbanking/ledger/integration/BalanceIntegrationTest.java`

- [ ] **Step 1: Write integration tests for withdrawal flow**

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class WithdrawalIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("ledger_db")
        .withUsername("test")
        .withPassword("test");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7")
        .withExposedPorts(6379);
    
    @Test
    void withdrawal_shouldSucceed_whenValidRequest() throws Exception {
        // Test BDD-W01: Successful withdrawal
    }
    
    @Test
    void withdrawal_shouldReturnCachedResponse_whenDuplicateIdempotencyKey() throws Exception {
        // Test BDD-W01-EC-07: Duplicate idempotency
    }
    
    @Test
    void withdrawal_shouldFail_whenInsufficientFloat() throws Exception {
        // Test BDD-L03-EC-01: Insufficient float
    }
}
```

- [ ] **Step 2: Write integration tests for deposit flow**

```java
@SpringBootTest
@AutoConfigureMockMvc
class DepositIntegrationTest {
    
    @Test
    void deposit_shouldSucceed_whenValidAccount() throws Exception {
        // Test BDD-D01: Successful deposit
    }
    
    @Test
    void deposit_shouldFail_whenInvalidAmount() throws Exception {
        // Test BDD-D01-EC-02, BDD-D01-EC-03: Invalid amount
    }
}
```

- [ ] **Step 3: Run all tests**

```bash
./gradlew :ledger-service:test -v
```

- [ ] **Step 4: Commit**

```bash
git add services/ledger-service/src/test/
git commit -m "test: add integration tests for withdrawal, deposit, balance"
```

---

## Task 10: Internal OpenAPI Spec

**BDD Scenarios:** N/A (documentation)
**BRD Requirements:** FR-12.3
**User-Facing:** NO

**Files:**
- Modify: `services/ledger-service/docs/openapi-internal.yaml`

```yaml
openapi: 3.0.3
info:
  title: Ledger & Float Service - Internal API
  version: 1.0.0
  description: Internal API for Ledger & Float Service

paths:
  /internal/v1/withdrawal:
    post:
      summary: Process cash withdrawal
      parameters:
        - name: X-Idempotency-Key
          required: true
          in: header
          schema:
            type: string
        - name: X-Agent-Id
          required: true
          in: header
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/WithdrawalRequest'
      responses:
        '200':
          description: Successful withdrawal
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TransactionResponse'

  /internal/v1/deposit:
    post:
      summary: Process cash deposit
      # ... similar to withdrawal

  /internal/v1/agent/balance:
    get:
      summary: Get agent wallet balance
      parameters:
        - name: X-Agent-Id
          required: true
          in: header
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Balance response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AgentBalanceResponse'

components:
  schemas:
    WithdrawalRequest:
      type: object
      required:
        - amount
        - cardData
        - pinBlock
        - currency
      properties:
        amount:
          type: number
          example: 500.00
        cardData:
          type: string
        pinBlock:
          type: string
        currency:
          type: string
          example: MYR

    TransactionResponse:
      type: object
      properties:
        status:
          type: string
        transactionId:
          type: string
          format: uuid
        amount:
          type: number
        customerFee:
          type: number
        referenceNumber:
          type: string
        timestamp:
          type: string
          format: date-time
```

---

## Task 11: Run Lint & Verify

- [ ] **Step 1: Run quality checks**

```bash
./gradlew :ledger-service:check
./gradlew :ledger-service:spotlessApply
./gradlew :ledger-service:intTest
```

---

## Summary

| Task | Description | BDD References |
|------|-------------|----------------|
| 1 | Project scaffolding | N/A |
| 2 | Domain layer (entities, ports) | BDD-L01, L02, L03 |
| 3 | Infrastructure (JPA adapters) | BDD-L01, L02, L03 |
| 4 | Redis idempotency & Kafka events | BDD-W01-EC-07 |
| 5 | BalanceManagementService | BDD-L01, L03, L03-EC-01 |
| 6 | Use cases (withdrawal, deposit, balance) | BDD-W01, D01, L04 |
| 7 | REST controllers | BDD-W01, D01, L01 |
| 8 | Rules Service integration | BDD-R01, R02 |
| 9 | Integration tests | All BDD-L*, W01, D01 |
| 10 | Internal OpenAPI spec | N/A |
| 11 | Quality verification | N/A |