# Transaction Orchestrator Implementation Plan [DONE]

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## Status: COMPLETED (2026-03-27)

All tasks completed. Transaction orchestrator service is fully operational with saga pattern implementation.

**Goal:** Implement Transaction Orchestrator service to coordinate multi-step financial flows (Saga pattern) for withdrawals, deposits, bill payments, and other transactions per BDD scenarios.

**Architecture:** Hexagonal (Ports & Adapters) pattern. Database-per-service (orchestrator_db). Saga pattern with compensation. Resilience4j circuit breakers for inter-service calls. 25-second timeout for switch calls.

**Tech Stack:** Java 21, Spring Boot 3.2.5, OpenFeign, Resilience4j, PostgreSQL, Redis, Kafka, JUnit 5, Mockito, ArchUnit.

---

## Task 1: Domain Layer - Saga Entities & Ports [DONE]

**BDD Scenarios:** BDD-W01, BDD-D01, BDD-B01, BDD-L04 (all transaction flow scenarios)  
**BRD Requirements:** US-L05, US-L07, US-B01, US-D01 (all transaction user stories)  
**User-Facing:** NO  

**Files:**
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/SagaState.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/SagaStep.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/SagaStatus.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/SagaStateRepository.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/RulesServicePort.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/LedgerServicePort.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/SwitchServicePort.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/BillerServicePort.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/service/SagaOrchestrator.java`

### Step 1: Create SagaStatus enum

```java
package com.agentbanking.orchestrator.domain.model;

public enum SagaStatus {
    STARTED,
    RULES_CHECKED,
    FLOAT_RESERVED,
    SWITCH_AUTHORIZED,
    COMMITTED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}
```

### Step 2: Create SagaStep entity

```java
package com.agentbanking.orchestrator.domain.model;

import java.time.Instant;

public class SagaStep {
    private final String stepName;
    private final String status;
    private final String result;
    private final String errorCode;
    private final Instant executedAt;

    // Constructor, getters...
}
```

### Step 3: Create SagaState entity

```java
package com.agentbanking.orchestrator.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class SagaState {
    private final UUID sagaId;
    private final String transactionType;
    private final UUID agentId;
    private final BigDecimal amount;
    private final String idempotencyKey;
    private SagaStatus status;
    private final List<SagaStep> steps;
    private String errorCode;
    private final Instant createdAt;
    private Instant completedAt;

    public void advance(SagaStatus newStatus) {
        this.status = newStatus;
    }

    public void fail(String errorCode) {
        this.status = SagaStatus.FAILED;
        this.errorCode = errorCode;
        this.completedAt = Instant.now();
    }

    public void compensate() {
        this.status = SagaStatus.COMPENSATING;
    }

    public void compensated() {
        this.status = SagaStatus.COMPENSATED;
        this.completedAt = Instant.now();
    }

    // Getters...
}
```

### Step 4: Create outbound ports

```java
package com.agentbanking.orchestrator.domain.port.out;

import com.agentbanking.orchestrator.domain.model.SagaState;
import java.util.Optional;
import java.util.UUID;

public interface SagaStateRepository {
    SagaState save(SagaState saga);
    Optional<SagaState> findById(UUID sagaId);
    Optional<SagaState> findByIdempotencyKey(String idempotencyKey);
}
```

```java
package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;

public interface RulesServicePort {
    FeeCheckResult checkFees(String transactionType, String agentTier);
    VelocityCheckResult checkVelocity(String customerMykad, String transactionType, BigDecimal amount);

    record FeeCheckResult(boolean passed, BigDecimal customerFee, BigDecimal agentCommission, BigDecimal bankShare) {}
    record VelocityCheckResult(boolean passed, String errorCode) {}
}
```

```java
package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface LedgerServicePort {
    FloatReservationResult reserveFloat(UUID agentId, BigDecimal amount, String idempotencyKey);
    FloatCommitResult commitFloat(UUID agentId, BigDecimal amount, String transactionId);
    void releaseFloat(UUID agentId, BigDecimal amount);

    record FloatReservationResult(boolean success, String transactionId, String errorCode) {}
    record FloatCommitResult(boolean success, String errorCode) {}
}
```

```java
package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;

public interface SwitchServicePort {
    AuthorizationResult authorize(String pan, String pinBlock, BigDecimal amount, String transactionId);
    void reverse(String transactionId);

    record AuthorizationResult(boolean approved, String referenceNumber, String declineCode) {}
}
```

```java
package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;

public interface BillerServicePort {
    PaymentResult payBill(String billerCode, String ref1, String ref2, BigDecimal amount, UUID agentId, String idempotencyKey);

    record PaymentResult(boolean success, String billerReference, String errorCode) {}
}
```

### Step 5: Create SagaOrchestrator domain service

```java
package com.agentbanking.orchestrator.domain.service;

import com.agentbanking.orchestrator.domain.model.*;
import com.agentbanking.orchestrator.domain.port.out.*;
import java.math.BigDecimal;
import java.util.UUID;

public class SagaOrchestrator {

    private final SagaStateRepository sagaRepository;
    private final RulesServicePort rulesService;
    private final LedgerServicePort ledgerService;
    private final SwitchServicePort switchService;
    private final BillerServicePort billerService;

    public SagaOrchestrator(SagaStateRepository sagaRepository,
                             RulesServicePort rulesService,
                             LedgerServicePort ledgerService,
                             SwitchServicePort switchService,
                             BillerServicePort billerService) {
        this.sagaRepository = sagaRepository;
        this.rulesService = rulesService;
        this.ledgerService = ledgerService;
        this.switchService = switchService;
        this.billerService = billerService;
    }

    public SagaState executeWithdrawalSaga(UUID agentId, String pan, String pinBlock,
                                            BigDecimal amount, String idempotencyKey) {
        // Check idempotency
        var existing = sagaRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        SagaState saga = new SagaState(
            UUID.randomUUID(), "CASH_WITHDRAWAL", agentId, amount, idempotencyKey,
            SagaStatus.STARTED, null, null, null, null
        );

        try {
            // Step 1: Check fees and velocity
            RulesServicePort.FeeCheckResult feeResult = rulesService.checkFees("CASH_WITHDRAWAL", "MICRO");
            if (!feeResult.passed()) {
                saga.fail("ERR_FEE_CHECK_FAILED");
                return sagaRepository.save(saga);
            }
            saga.advance(SagaStatus.RULES_CHECKED);

            // Step 2: Reserve float
            LedgerServicePort.FloatReservationResult reserveResult = ledgerService.reserveFloat(agentId, amount, idempotencyKey);
            if (!reserveResult.success()) {
                saga.fail("ERR_FLOAT_RESERVATION_FAILED");
                return sagaRepository.save(saga);
            }
            saga.advance(SagaStatus.FLOAT_RESERVED);

            // Step 3: Authorize at switch
            SwitchServicePort.AuthorizationResult authResult = switchService.authorize(pan, pinBlock, amount, reserveResult.transactionId());
            if (!authResult.approved()) {
                // Compensate: release float
                ledgerService.releaseFloat(agentId, amount);
                saga.fail("ERR_SWITCH_DECLINED");
                return sagaRepository.save(saga);
            }
            saga.advance(SagaStatus.SWITCH_AUTHORIZED);

            // Step 4: Commit float
            LedgerServicePort.FloatCommitResult commitResult = ledgerService.commitFloat(agentId, amount, reserveResult.transactionId());
            if (!commitResult.success()) {
                // Compensate: reverse switch and release float
                switchService.reverse(reserveResult.transactionId());
                ledgerService.releaseFloat(agentId, amount);
                saga.fail("ERR_COMMIT_FAILED");
                return sagaRepository.save(saga);
            }
            saga.advance(SagaStatus.COMMITTED);

            return sagaRepository.save(saga);

        } catch (Exception e) {
            compensate(saga);
            saga.fail("ERR_SAGA_FAILED");
            return sagaRepository.save(saga);
        }
    }

    private void compensate(SagaState saga) {
        saga.compensate();
        // Implement compensation logic based on completed steps
        saga.compensated();
    }
}
```

### Step 6: Commit

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/
git commit -m "feat(orchestrator): add domain entities, ports and saga orchestrator"
```

---

## Task 2: Infrastructure Layer - JPA Adapters [DONE]

**BDD Scenarios:** All transaction flow scenarios  
**BRD Requirements:** All transaction user stories  
**User-Facing:** NO  

**Files:**
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/persistence/entity/SagaStateEntity.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/persistence/repository/SagaStateJpaRepository.java`
- Create: Flyway migration: `services/orchestrator-service/src/main/resources/db/migration/V1__saga_tables.sql`

### Step 1: Create Flyway migration

```sql
CREATE TABLE saga_state (
    saga_id UUID PRIMARY KEY,
    transaction_type VARCHAR(50) NOT NULL,
    agent_id UUID NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    error_code VARCHAR(50),
    steps JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP
);
```

### Step 2: Create JPA entities and repository adapters

### Step 3: Run tests and commit

```bash
./gradlew :orchestrator-service:test
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/persistence/
git commit -m "feat(orchestrator): add JPA entities and repository adapters"
```

---

## Task 3: Application Layer - Use Cases [DONE]

**BDD Scenarios:** BDD-W01, BDD-D01, BDD-B01, BDD-L04  
**BRD Requirements:** US-L05, US-L07, US-B01, US-D01  
**User-Facing:** NO  

**Files:**
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/in/ExecuteSagaUseCase.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/usecase/ExecuteSagaUseCaseImpl.java`

### Step 1: Create inbound port interface

```java
package com.agentbanking.orchestrator.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface ExecuteSagaUseCase {
    SagaResponse execute(SagaCommand command);

    record SagaCommand(
        String transactionType,
        UUID agentId,
        BigDecimal amount,
        String idempotencyKey,
        String pan,
        String pinBlock,
        String billerCode,
        String ref1,
        String ref2
    ) {}

    record SagaResponse(
        String status,
        String sagaId,
        String transactionId,
        String errorCode
    ) {}
}
```

### Step 2: Create use case implementation

### Step 3: Run tests and commit

```bash
./gradlew :orchestrator-service:test
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/in/
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/
git commit -m "feat(orchestrator): add use case for saga execution"
```

---

## Task 4: Infrastructure Layer - REST Controllers & Feign Clients [DONE]

**BDD Scenarios:** All transaction flow scenarios  
**BRD Requirements:** All transaction user stories  
**User-Facing:** NO  

**Files:**
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/web/OrchestratorController.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/RulesServiceFeignClient.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/LedgerServiceFeignClient.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/SwitchServiceFeignClient.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/BillerServiceFeignClient.java`
- Create: Adapters implementing domain ports

### Step 1: Create REST controller for internal APIs

### Step 2: Create Feign clients with Resilience4j circuit breakers

### Step 3: Create adapters implementing domain ports

### Step 4: Run tests and commit

```bash
./gradlew :orchestrator-service:test
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/
git commit -m "feat(orchestrator): add REST controller and Feign clients with circuit breakers"
```

---

## Task 5: Unit Tests [DONE]

**BDD Scenarios:** All BDD-W*, BDD-D*, BDD-B*, BDD-L* scenarios  
**BRD Requirements:** All transaction user stories  
**User-Facing:** NO  

**Files:**
- Create: `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/domain/service/SagaOrchestratorTest.java`
- Create: `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/application/usecase/ExecuteSagaUseCaseImplTest.java`
- Create: `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/architecture/HexagonalArchitectureTest.java`

### Step 1: Write unit tests for saga orchestrator

### Step 2: Write unit tests for use cases

### Step 3: Write ArchUnit test

### Step 4: Run all tests and commit

```bash
./gradlew :orchestrator-service:test
git add services/orchestrator-service/src/test/
git commit -m "test(orchestrator): add unit tests and ArchUnit validation for Transaction Orchestrator"
```

---