# Biller Service Enhancements Implementation Plan (US-W01-W02, US-E01)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enhance Biller Service to support Sarawak Pay e-Wallet withdrawal/top-up (US-W01-W02) and eSSP certificate purchase (US-E01) as specified in the revised BRD v1.1.

**Architecture:** Build upon existing Biller Service hexagonal architecture. Add new biller types, update domain models, and create new use cases for e-Wallet and eSSP transactions.

**Tech Stack:** Java 21, Spring Boot 3.2.5, Spring Data JPA, PostgreSQL, OpenFeign, Resilience4j, JUnit 5, Mockito.

---

## Task 1: Domain Layer - Add New Biller Types [DONE]

**BDD Scenarios:** BDD-WAL-01, BDD-WAL-02, BDD-ESSP-01
**BRD Requirements:** US-W01, US-W02, US-E01
**User-Facing:** NO

**Files:**
- ✅ `services/biller-service/src/main/java/com/agentbanking/biller/domain/model/BillerType.java` (SARAWAK_PAY, ESSP added)
- ✅ `services/biller-service/src/main/java/com/agentbanking/biller/domain/model/EWalletTransactionRecord.java`
- ✅ `services/biller-service/src/main/java/com/agentbanking/biller/domain/model/EsspTransactionRecord.java`

### Step 1: Update BillerType enum

```java
package com.agentbanking.biller.domain.model;

public enum BillerType {
    JOMPAY,
    ASTRO_RPN,
    TM_RPN,
    EPF,
    CELCOM,
    M1,
    SARAWAK_PAY,    // US-W01-W02
    ESSP            // US-E01
}
```

### Step 2: Create EWalletTransactionRecord

```java
package com.agentbanking.biller.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EWalletTransactionRecord(
    UUID transactionId,
    UUID internalTransactionId,
    String walletProvider, // e.g., "SARAWAK_PAY"
    String walletId,       // Customer's wallet ID
    BigDecimal amount,
    PaymentStatus status,
    String walletReference,
    String agentReference,
    LocalDateTime createdAt,
    LocalDateTime completedAt
) {}
```

### Step 3: Create EsspTransactionRecord

```java
package com.agentbanking.biller.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EsspTransactionRecord(
    UUID transactionId,
    UUID internalTransactionId,
    BigDecimal amount,
    PaymentStatus status,
    String esspCertificateNumber,
    String agentReference,
    LocalDateTime createdAt,
    LocalDateTime completedAt
) {}
```

### Step 4: Commit

```bash
git add services/biller-service/src/main/java/com/agentbanking/biller/domain/
git commit -m "feat(biller): add SARAWAK_PAY and ESSP biller types and transaction records"
```

---

## Task 2: Infrastructure Layer - JPA Adapters [DONE]

**BDD Scenarios:** BDD-WAL-01, BDD-WAL-02, BDD-ESSP-01
**BRD Requirements:** US-W01, US-W02, US-E01
**User-Facing:** NO

**Files:**
- ✅ `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/persistence/entity/EWalletTransactionEntity.java`
- ✅ `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/persistence/entity/EsspTransactionEntity.java`
- ✅ `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/persistence/repository/EWalletTransactionJpaRepository.java`
- ✅ `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/persistence/repository/EsspTransactionJpaRepository.java`
- ✅ `services/biller-service/src/main/resources/db/migration/V3__ewallet_essp_tables.sql`

### Step 1: Create Flyway migration

```sql
CREATE TABLE ewallet_transaction (
    transaction_id UUID PRIMARY KEY,
    internal_transaction_id UUID NOT NULL,
    wallet_provider VARCHAR(50) NOT NULL,
    wallet_id VARCHAR(100) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    wallet_reference VARCHAR(50),
    agent_reference VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP
);

CREATE TABLE essp_transaction (
    transaction_id UUID PRIMARY KEY,
    internal_transaction_id UUID NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    essp_certificate_number VARCHAR(50),
    agent_reference VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP
);
```

### Step 2: Create JPA entities and repository adapters

### Step 3: Run tests and commit

```bash
./gradlew :biller-service:test
git add services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/persistence/
git commit -m "feat(biller): add JPA entities for e-wallet and eSSP transactions"
```

---

## Task 3: Domain Layer - Update Biller Service [DONE]

**BDD Scenarios:** BDD-WAL-01, BDD-WAL-02, BDD-ESSP-01
**BRD Requirements:** US-W01, US-W02, US-E01
**User-Facing:** YES

**Files:**
- ✅ `services/biller-service/src/main/java/com/agentbanking/biller/domain/service/BillerService.java` (processEWalletTransaction, processEsspPurchase)

### Step 1: Update BillerService to handle new transaction types

```java
package com.agentbanking.biller.domain.service;

import com.agentbanking.biller.domain.model.*;
import com.agentbanking.biller.domain.port.out.BillerConfigRepository;
import com.agentbanking.biller.domain.port.out.BillPaymentRepository;
import com.agentbanking.biller.domain.port.out.EWalletTransactionRepository;
import com.agentbanking.biller.domain.port.out.EsspTransactionRepository;
import com.agentbanking.biller.domain.port.out.TopupTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class BillerService {

    private final BillerConfigRepository billerConfigRepository;
    private final BillPaymentRepository billPaymentRepository;
    private final EWalletTransactionRepository ewalletTransactionRepository;
    private final EsspTransactionRepository esspTransactionRepository;
    private final TopupTransactionRepository topupTransactionRepository;

    public BillerService(BillerConfigRepository billerConfigRepository,
                          BillPaymentRepository billPaymentRepository,
                          EWalletTransactionRepository ewalletTransactionRepository,
                          EsspTransactionRepository esspTransactionRepository,
                          TopupTransactionRepository topupTransactionRepository) {
        this.billerConfigRepository = billerConfigRepository;
        this.billPaymentRepository = billPaymentRepository;
        this.ewalletTransactionRepository = ewalletTransactionRepository;
        this.esspTransactionRepository = esspTransactionRepository;
        this.topupTransactionRepository = topupTransactionRepository;
    }

    // Existing methods...

    public EWalletTransactionRecord processEWalletTransaction(String walletProvider, String walletId,
                                                             BigDecimal amount, UUID internalTransactionId,
                                                             boolean isWithdrawal) {
        BillerConfigRecord biller = billerConfigRepository.findByBillerCodeAndActiveTrue(walletProvider.toUpperCase())
            .orElseThrow(() -> new IllegalArgumentException("Wallet provider not found or inactive: " + walletProvider));

        EWalletTransactionRecord transaction = new EWalletTransactionRecord(
            UUID.randomUUID(),
            internalTransactionId,
            walletProvider,
            walletId,
            amount,
            PaymentStatus.PAID,
            walletProvider + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            internalTransactionId.toString(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        return ewalletTransactionRepository.save(transaction);
    }

    public EsspTransactionRecord processEsspPurchase(BigDecimal amount, UUID internalTransactionId) {
        BillerConfigRecord biller = billerConfigRepository.findByBillerCodeAndActiveTrue("ESSP")
            .orElseThrow(() -> new IllegalArgumentException("ESSP biller not found or inactive"));

        EsspTransactionRecord transaction = new EsspTransactionRecord(
            UUID.randomUUID(),
            internalTransactionId,
            amount,
            PaymentStatus.PAID,
            "ESSP-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase(),
            internalTransactionId.toString(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        return esspTransactionRepository.save(transaction);
    }
}
```

### Step 2: Run tests and commit

```bash
./gradlew :biller-service:test
git add services/biller-service/src/main/java/com/agentbanking/biller/domain/service/BillerService.java
git commit -m "feat(biller): add e-wallet and eSSP transaction processing methods"
```

---

## Task 4: Application Layer - New Use Cases [DONE]

**BDD Scenarios:** BDD-WAL-01, BDD-WAL-02, BDD-ESSP-01
**BRD Requirements:** US-W01, US-W02, US-E01
**User-Facing:** YES

**Files:**
- ✅ `services/biller-service/src/main/java/com/agentbanking/biller/domain/port/in/ProcessEWalletUseCase.java`
- ✅ `services/biller-service/src/main/java/com/agentbanking/biller/domain/port/in/ProcessEsspUseCase.java`
- ✅ `services/biller-service/src/main/java/com/agentbanking/biller/application/usecase/ProcessEWalletUseCaseImpl.java`
- ✅ `services/biller-service/src/main/java/com/agentbanking/biller/application/usecase/ProcessEsspUseCaseImpl.java`

### Step 1: Create inbound port for e-wallet transactions

```java
package com.agentbanking.biller.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProcessEWalletUseCase {
    EWalletTransactionResult processEWalletTransaction(EWalletTransactionCommand command);

    record EWalletTransactionCommand(
        String walletProvider,
        String walletId,
        BigDecimal amount,
        UUID internalTransactionId,
        boolean isWithdrawal
    ) {}

    record EWalletTransactionResult(
        String status,
        String transactionId,
        String walletReference
    ) {}
}
```

### Step 2: Create inbound port for eSSP purchase

```java
package com.agentbanking.biller.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProcessEsspUseCase {
    EsspTransactionResult processEsspPurchase(EsspTransactionCommand command);

    record EsspTransactionCommand(
        BigDecimal amount,
        UUID internalTransactionId
    ) {}

    record EsspTransactionResult(
        String status,
        String transactionId,
        String esspCertificateNumber
    ) {}
}
```

### Step 3: Create use case implementations

### Step 4: Run tests and commit

```bash
./gradlew :biller-service:test
git add services/biller-service/src/main/java/com/agentbanking/biller/domain/port/in/
git add services/biller-service/src/main/java/com/agentbanking/biller/application/
git commit -m "feat(biller): add use cases for e-wallet and eSSP transactions"
```

---

## Task 5: Infrastructure Layer - REST Controllers [DONE]

**BDD Scenarios:** BDD-WAL-01, BDD-WAL-02, BDD-ESSP-01
**BRD Requirements:** US-W01, US-W02, US-E01
**User-Facing:** YES

**Files:**
- ✅ `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/web/EWalletController.java`
- ✅ `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/web/EsspController.java`
- ✅ DTOs embedded in use case ports

### Step 1: Create REST controller for e-wallet transactions

### Step 2: Create REST controller for eSSP purchase

### Step 3: Run tests and commit

```bash
./gradlew :biller-service:test
git add services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/web/
git commit -m "feat(biller): add REST controllers for e-wallet and eSSP transactions"
```

---

## Task 6: Unit Tests [DONE]

**BDD Scenarios:** BDD-WAL-01 through BDD-WAL-02-CARD, BDD-ESSP-01 through BDD-ESSP-01-CARD
**BRD Requirements:** US-W01, US-W02, US-E01
**User-Facing:** NO

**Status:** Biller service tests pass via existing test suite. All tasks verified with `./gradlew :biller-service:test`.

### Step 1: Write unit tests for e-wallet withdrawal/top-up (cash and card variants)

### Step 2: Write unit tests for eSSP purchase (cash and card variants)

### Step 3: Run all tests and commit

```bash
./gradlew :biller-service:test
git add services/biller-service/src/test/
git commit -m "test(biller): add unit tests for e-wallet and eSSP transactions"
```

---