# Biller Service Implementation Plan (US-B01-B05, US-T01-T03, US-W01-W02, US-E01, US-D01-D02) [DONE]

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## Status: COMPLETED (2026-03-27)

All tasks completed. Biller service is fully operational with domain, infrastructure, and application layers.

**Goal:** Implement Biller Service for bill payments (JomPAY, ASTRO RPN, TM RPN, EPF), prepaid top-up (CELCOM, M1), e-Wallet (Sarawak Pay), eSSP purchase, and DuitNow/JomPAY transfers per user stories US-B01-B05, US-T01-T03, US-W01-W02, US-E01, US-D01-D02.

**Architecture:** Hexagonal (Ports & Adapters) pattern. Database-per-service (biller_db). Biller Gateway adapter for multi-biller integration. Idempotency for bill payments. Webhook validation for biller callbacks.

**Tech Stack:** Java 21, Spring Boot 3.2.5, Spring Data JPA, PostgreSQL, Kafka, OpenFeign, Resilience4j, JUnit 5, Mockito, ArchUnit.

---

## Task 1: Domain Layer - Entities & Ports [DONE]

**BDD Scenarios:** BDD-B01, BDD-B02, BDD-B03, BDD-B04, BDD-B05, BDD-T01, BDD-T02, BDD-T03, BDD-W01, BDD-W02, BDD-E01, BDD-D01, BDD-D02  
**BRD Requirements:** US-B01-B05, US-T01-T03, US-W01-W02, US-E01, US-D01-D02  
**User-Facing:** YES  

**Files:**
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/domain/model/BillerType.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/domain/model/BillerConfig.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/domain/model/BillPayment.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/domain/model/BillPaymentStatus.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/domain/port/out/BillerConfigRepository.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/domain/port/out/BillPaymentRepository.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/domain/port/out/BillerGatewayPort.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/domain/service/BillerService.java`

### Step 1: Create BillerType enum

```java
package com.agentbanking.biller.domain.model;

public enum BillerType {
    JOMPAY,
    ASTRO,
    TM,
    EPF,
    CELCOM,
    M1,
    SARAWAK_PAY,
    ESSP,
    DUITNOW
}
```

### Step 2: Create BillPaymentStatus enum

```java
package com.agentbanking.biller.domain.model;

public enum BillPaymentStatus {
    PENDING,
    VALIDATED,
    COMPLETED,
    FAILED
}
```

### Step 3: Create BillerConfig entity

```java
package com.agentbanking.biller.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public class BillerConfig {
    private final String billerCode;
    private final BillerType billerType;
    private final String billerName;
    private final boolean active;
    private final BigDecimal convenienceFee;
    private final String apiEndpoint;
    private final String apiKey; // encrypted
    private final Instant createdAt;
    private final Instant updatedAt;

    // Constructor, getters...
}
```

### Step 4: Create BillPayment entity

```java
package com.agentbanking.biller.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class BillPayment {
    private final UUID paymentId;
    private final String idempotencyKey;
    private final String billerCode;
    private final String ref1;
    private final String ref2;
    private final BigDecimal amount;
    private BillPaymentStatus status;
    private String billerReference;
    private String errorCode;
    private final UUID agentId;
    private final Instant createdAt;
    private Instant completedAt;

    public void markValidated() {
        this.status = BillPaymentStatus.VALIDATED;
    }

    public void markCompleted(String billerReference) {
        this.status = BillPaymentStatus.COMPLETED;
        this.billerReference = billerReference;
        this.completedAt = Instant.now();
    }

    public void markFailed(String errorCode) {
        this.status = BillPaymentStatus.FAILED;
        this.errorCode = errorCode;
        this.completedAt = Instant.now();
    }

    // Getters...
}
```

### Step 5: Create outbound ports

```java
package com.agentbanking.biller.domain.port.out;

import com.agentbanking.biller.domain.model.BillerConfig;
import java.util.Optional;

public interface BillerConfigRepository {
    Optional<BillerConfig> findByBillerCode(String billerCode);
    BillerConfig save(BillerConfig config);
}
```

```java
package com.agentbanking.biller.domain.port.out;

import com.agentbanking.biller.domain.model.BillPayment;
import java.util.Optional;
import java.util.UUID;

public interface BillPaymentRepository {
    Optional<BillPayment> findByIdempotencyKey(String idempotencyKey);
    BillPayment save(BillPayment payment);
}
```

```java
package com.agentbanking.biller.domain.port.out;

import java.math.BigDecimal;

public interface BillerGatewayPort {
    ValidationResult validate(String billerCode, String ref1, String ref2);
    PaymentResult pay(String billerCode, String ref1, String ref2, BigDecimal amount, String paymentId);

    record ValidationResult(boolean valid, String accountName, BigDecimal amountDue) {}
    record PaymentResult(boolean success, String billerReference, String errorCode) {}
}
```

### Step 6: Create BillerService domain service

```java
package com.agentbanking.biller.domain.service;

import com.agentbanking.biller.domain.model.*;
import com.agentbanking.biller.domain.port.out.*;
import java.math.BigDecimal;
import java.util.UUID;

public class BillerService {

    private final BillerConfigRepository configRepository;
    private final BillPaymentRepository paymentRepository;
    private final BillerGatewayPort billerGateway;

    public BillerService(BillerConfigRepository configRepository,
                          BillPaymentRepository paymentRepository,
                          BillerGatewayPort billerGateway) {
        this.configRepository = configRepository;
        this.paymentRepository = paymentRepository;
        this.billerGateway = billerGateway;
    }

    public BillPayment validateAndPay(String billerCode, String ref1, String ref2,
                                      BigDecimal amount, UUID agentId, String idempotencyKey) {
        // Check idempotency
        var existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Validate bill
        BillerGatewayPort.ValidationResult validation = billerGateway.validate(billerCode, ref1, ref2);

        if (!validation.valid()) {
            throw new IllegalArgumentException("ERR_BILL_VALIDATION_FAILED");
        }

        // Create payment
        BillPayment payment = new BillPayment(
            UUID.randomUUID(), idempotencyKey, billerCode, ref1, ref2,
            amount, BillPaymentStatus.PENDING, null, null, agentId, null, null
        );
        payment.markValidated();

        // Process payment
        BillerGatewayPort.PaymentResult result = billerGateway.pay(billerCode, ref1, ref2, amount, payment.getPaymentId().toString());

        if (result.success()) {
            payment.markCompleted(result.billerReference());
        } else {
            payment.markFailed(result.errorCode());
        }

        return paymentRepository.save(payment);
    }
}
```

### Step 7: Commit

```bash
git add services/biller-service/src/main/java/com/agentbanking/biller/domain/
git commit -m "feat(biller): add domain entities, ports and biller service"
```

---

## Task 2: Infrastructure Layer - JPA Adapters [DONE]

**BDD Scenarios:** BDD-B01, BDD-B02, BDD-B03, BDD-T01, BDD-T02, BDD-W01, BDD-E01, BDD-D01  
**BRD Requirements:** US-B01-B05, US-T01-T03, US-W01-W02, US-E01, US-D01-D02  
**User-Facing:** NO  

**Files:**
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/persistence/entity/BillerConfigEntity.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/persistence/entity/BillPaymentEntity.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/persistence/repository/BillerConfigJpaRepository.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/persistence/repository/BillPaymentJpaRepository.java`
- Create: Flyway migration: `services/biller-service/src/main/resources/db/migration/V2__biller_tables.sql`

### Step 1: Create Flyway migration

```sql
CREATE TABLE biller_config (
    biller_code VARCHAR(20) PRIMARY KEY,
    biller_type VARCHAR(20) NOT NULL,
    biller_name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    convenience_fee DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    api_endpoint VARCHAR(200),
    api_key VARCHAR(255), -- encrypted
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE bill_payment (
    payment_id UUID PRIMARY KEY,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    biller_code VARCHAR(20) NOT NULL,
    ref1 VARCHAR(50) NOT NULL,
    ref2 VARCHAR(50),
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    biller_reference VARCHAR(50),
    error_code VARCHAR(20),
    agent_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    CONSTRAINT fk_biller_config FOREIGN KEY (biller_code) REFERENCES biller_config(biller_code)
);
```

### Step 2: Create JPA entities and repository adapters

### Step 3: Run tests and commit

```bash
./gradlew :biller-service:test
git add services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/persistence/
git commit -m "feat(biller): add JPA entities and repository adapters"
```

---

## Task 3: Application Layer - Use Cases [DONE]

**BDD Scenarios:** BDD-B01, BDD-B02, BDD-B03, BDD-B04, BDD-B05, BDD-T01, BDD-T02, BDD-T03, BDD-W01, BDD-W02, BDD-E01, BDD-D01, BDD-D02  
**BRD Requirements:** US-B01-B05, US-T01-T03, US-W01-W02, US-E01, US-D01-D02  
**User-Facing:** YES  

**Files:**
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/domain/port/in/ValidateBillUseCase.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/domain/port/in/PayBillUseCase.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/domain/port/in/TopUpUseCase.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/application/usecase/ValidateBillUseCaseImpl.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/application/usecase/PayBillUseCaseImpl.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/application/usecase/TopUpUseCaseImpl.java`

### Step 1: Create inbound port interfaces

```java
package com.agentbanking.biller.domain.port.in;

import java.math.BigDecimal;

public interface ValidateBillUseCase {
    ValidationResponse validate(ValidationCommand command);

    record ValidationCommand(String billerCode, String ref1, String ref2) {}

    record ValidationResponse(boolean valid, String accountName, BigDecimal amountDue) {}
}
```

```java
package com.agentbanking.biller.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface PayBillUseCase {
    PaymentResponse pay(PaymentCommand command);

    record PaymentCommand(
        String billerCode,
        String ref1,
        String ref2,
        BigDecimal amount,
        UUID agentId,
        String idempotencyKey
    ) {}

    record PaymentResponse(String status, String paymentId, String billerReference) {}
}
```

```java
package com.agentbanking.biller.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface TopUpUseCase {
    TopUpResponse topUp(TopUpCommand command);

    record TopUpCommand(
        String providerCode,
        String accountNumber,
        BigDecimal amount,
        UUID agentId,
        String idempotencyKey
    ) {}

    record TopUpResponse(String status, String transactionId) {}
}
```

### Step 2: Create use case implementations

### Step 3: Run tests and commit

```bash
./gradlew :biller-service:test
git add services/biller-service/src/main/java/com/agentbanking/biller/domain/port/in/
git add services/biller-service/src/main/java/com/agentbanking/biller/application/
git commit -m "feat(biller): add use cases for bill validation, payment and top-up"
```

---

## Task 4: Infrastructure Layer - REST Controllers [DONE]

**BDD Scenarios:** BDD-B01, BDD-B02, BDD-B03, BDD-T01, BDD-T02, BDD-W01, BDD-W02, BDD-E01, BDD-D01, BDD-D02  
**BRD Requirements:** US-B01-B05, US-T01-T03, US-W01-W02, US-E01, US-D01-D02  
**User-Facing:** YES  

**Files:**
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/web/BillerController.java`
- Create: DTOs for requests/responses
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/web/GlobalExceptionHandler.java`

### Step 1: Create REST controller for bill payment endpoints

### Step 2: Create GlobalExceptionHandler

### Step 3: Run tests and commit

```bash
./gradlew :biller-service:test
git add services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/web/
git commit -m "feat(biller): add REST controllers for bill payment endpoints"
```

---

## Task 5: Infrastructure - Biller Gateway Adapters [DONE]

**BDD Scenarios:** BDD-B01, BDD-B02, BDD-B03, BDD-T01, BDD-T02, BDD-W01, BDD-W02, BDD-E01, BDD-D01, BDD-D02  
**BRD Requirements:** US-B01-B05, US-T01-T03, US-W01-W02, US-E01, US-D01-D02  
**User-Facing:** NO  

**Files:**
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/external/JomPayGatewayAdapter.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/external/AstroGatewayAdapter.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/external/TmGatewayAdapter.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/external/EpfGatewayAdapter.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/external/TopUpGatewayAdapter.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/external/EWalletGatewayAdapter.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/external/EsspGatewayAdapter.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/external/DuitNowGatewayAdapter.java`
- Create: `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/external/BillerGatewayFactory.java`

### Step 1: Create gateway adapters for each biller type

### Step 2: Create factory to route requests to correct adapter

### Step 3: Run tests and commit

```bash
./gradlew :biller-service:test
git add services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/external/
git commit -m "feat(biller): add gateway adapters for JomPAY, Astro, TM, EPF, top-up, e-wallet, eSSP, DuitNow"
```

---

## Task 6: Unit Tests [DONE]

**BDD Scenarios:** All BDD-B*, BDD-T*, BDD-W*, BDD-E*, BDD-D* scenarios  
**BRD Requirements:** US-B01-B05, US-T01-T03, US-W01-W02, US-E01, US-D01-D02  
**User-Facing:** NO  

**Files:**
- Create: `services/biller-service/src/test/java/com/agentbanking/biller/domain/service/BillerServiceTest.java`
- Create: `services/biller-service/src/test/java/com/agentbanking/biller/application/usecase/PayBillUseCaseImplTest.java`
- Create: `services/biller-service/src/test/java/com/agentbanking/biller/architecture/HexagonalArchitectureTest.java`

### Step 1: Write unit tests for biller service

### Step 2: Write unit tests for use cases

### Step 3: Write ArchUnit test

### Step 4: Run all tests and commit

```bash
./gradlew :biller-service:test
git add services/biller-service/src/test/
git commit -m "test(biller): add unit tests and ArchUnit validation for Biller Service"
```

---