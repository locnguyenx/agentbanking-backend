# EOD Settlement & Reconciliation Implementation Plan (US-SM01-SM04, US-DR01-DR03)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement End-of-Day (EOD) net settlement batch job, triple-match reconciliation (Internal ↔ Terminal ↔ PayNet PSR), discrepancy detection (Ghost/Orphan/Mismatch), and maker-checker discrepancy resolution workflow per user stories US-SM01-SM04 and US-DR01-DR03.

**Architecture:** Hexagonal (Ports & Adapters) pattern. Scheduled batch job at 23:59:59 MYT. Triple-match reconciliation engine. Maker-checker workflow in Backoffice. Settlement summary stored in ledger_db.

**Tech Stack:** Java 21, Spring Boot 3.2.5, Spring Data JPA, PostgreSQL, Flyway, Spring Scheduler, JUnit 5, Mockito, ArchUnit.

---

## Task 1: Domain Layer - Settlement Entities [DONE]

**BDD Scenarios:** BDD-SM01, BDD-SM01-EC-01, BDD-SM02, BDD-SM02-EC-01, BDD-SM03, BDD-SM04
**BRD Requirements:** US-SM01, US-SM02, US-SM03, US-SM04, FR-16.1, FR-16.2, FR-16.3
**User-Facing:** NO

**Files:**
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/model/SettlementDirection.java`
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/model/SettlementStatus.java`
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/model/SettlementSummaryRecord.java`
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/model/DiscrepancyType.java`
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/model/DiscrepancyCase.java`
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/model/DiscrepancyStatus.java`
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/port/out/SettlementSummaryRepository.java`
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/port/out/DiscrepancyCaseRepository.java`
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/SettlementService.java`
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/ReconciliationService.java`

### Step 1: Create SettlementDirection enum

```java
package com.agentbanking.ledger.domain.model;

public enum SettlementDirection {
    BANK_OWES_AGENT,
    AGENT_OWES_BANK
}
```

### Step 2: Create SettlementStatus enum

```java
package com.agentbanking.ledger.domain.model;

public enum SettlementStatus {
    PENDING,
    HELD,
    SETTLED,
    DISPUTED,
    RESOLVED
}
```

### Step 3: Create DiscrepancyType enum

```java
package com.agentbanking.ledger.domain.model;

public enum DiscrepancyType {
    GHOST,    // Internal success, PayNet missing
    ORPHAN,   // Internal missing, PayNet success
    MISMATCH  // Amounts differ
}
```

### Step 4: Create SettlementSummary entity

```java
package com.agentbanking.ledger.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class SettlementSummary {
    private final UUID settlementId;
    private final UUID agentId;
    private final LocalDate businessDate;
    private BigDecimal totalWithdrawals;
    private BigDecimal totalDeposits;
    private BigDecimal totalBillPayments;
    private BigDecimal totalCommissions;
    private BigDecimal totalRetailSales;
    private BigDecimal netSettlement;
    private SettlementDirection direction;
    private SettlementStatus status;
    private final Instant createdAt;
    private Instant settledAt;

    public void calculateNetSettlement() {
        BigDecimal credits = totalWithdrawals.add(totalCommissions).add(totalRetailSales);
        BigDecimal debits = totalDeposits.add(totalBillPayments);
        this.netSettlement = credits.subtract(debits);
        this.direction = netSettlement.compareTo(BigDecimal.ZERO) >= 0
            ? SettlementDirection.BANK_OWES_AGENT
            : SettlementDirection.AGENT_OWES_BANK;
    }

    public void markSettled() {
        this.status = SettlementStatus.SETTLED;
        this.settledAt = Instant.now();
    }

    public void hold() {
        this.status = SettlementStatus.HELD;
    }

    // Getters, setters...
}
```

### Step 5: Create DiscrepancyCase entity

```java
package com.agentbanking.ledger.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class DiscrepancyCase {
    private final UUID caseId;
    private final String transactionId;
    private final DiscrepancyType discrepancyType;
    private final BigDecimal internalAmount;
    private final BigDecimal networkAmount;
    private DiscrepancyStatus status;
    private String makerAction;
    private String makerUserId;
    private String makerReason;
    private String checkerUserId;
    private String checkerAction;
    private String checkerReason;
    private final Instant createdAt;
    private Instant resolvedAt;

    public void makerPropose(String action, String userId, String reason) {
        this.makerAction = action;
        this.makerUserId = userId;
        this.makerReason = reason;
        this.status = DiscrepancyStatus.PENDING_CHECKER;
    }

    public void checkerApprove(String userId, String reason) {
        this.checkerUserId = userId;
        this.checkerAction = "APPROVED";
        this.checkerReason = reason;
        this.status = DiscrepancyStatus.RESOLVED;
        this.resolvedAt = Instant.now();
    }

    public void checkerReject(String userId, String reason) {
        this.checkerUserId = userId;
        this.checkerAction = "REJECTED";
        this.checkerReason = reason;
        this.status = DiscrepancyStatus.PENDING_MAKER;
    }

    // Getters...
}

public enum DiscrepancyStatus {
    PENDING_MAKER,
    PENDING_CHECKER,
    RESOLVED
}
```

### Step 6: Create SettlementCalculationService domain service

```java
package com.agentbanking.ledger.domain.service;

import com.agentbanking.ledger.domain.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class SettlementCalculationService {

    public SettlementSummary calculateDailySettlement(UUID agentId, LocalDate businessDate,
                                                       TransactionSummaryPort txnSummary) {
        SettlementSummary summary = new SettlementSummary(
            UUID.randomUUID(), agentId, businessDate,
            txnSummary.getTotalWithdrawals(agentId, businessDate),
            txnSummary.getTotalDeposits(agentId, businessDate),
            txnSummary.getTotalBillPayments(agentId, businessDate),
            txnSummary.getTotalCommissions(agentId, businessDate),
            txnSummary.getTotalRetailSales(agentId, businessDate),
            null, null, SettlementStatus.PENDING, null, null
        );
        summary.calculateNetSettlement();
        return summary;
    }
}
```

### Step 7: Create ReconciliationService domain service

```java
package com.agentbanking.ledger.domain.service;

import com.agentbanking.ledger.domain.model.*;
import java.math.BigDecimal;
import java.util.List;

public class ReconciliationService {

    public List<DiscrepancyCase> reconcile(List<Transaction> internalTransactions,
                                            List<NetworkTransaction> networkTransactions) {
        // Triple-match logic: compare internal ↔ network
        // Create DiscrepancyCase for Ghost, Orphan, Mismatch
        // ...
    }
}
```

### Step 8: Commit

```bash
git add services/ledger-service/src/main/java/com/agentbanking/ledger/domain/
git commit -m "feat(ledger): add settlement and reconciliation domain entities and services"
```

---

## Task 2: Infrastructure Layer - JPA Adapters [DONE]

**BDD Scenarios:** BDD-SM01, BDD-SM02, BDD-SM03, BDD-SM04, BDD-DR01, BDD-DR02, BDD-DR03
**BRD Requirements:** US-SM01, US-SM02, US-SM03, US-SM04, US-DR01, US-DR02, US-DR03
**User-Facing:** NO

**Files:**
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/entity/SettlementSummaryEntity.java`
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/entity/DiscrepancyCaseEntity.java`
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/repository/SettlementSummaryJpaRepository.java`
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/repository/SettlementSummaryRepositoryAdapter.java`
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/repository/DiscrepancyCaseJpaRepository.java`
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/repository/DiscrepancyCaseRepositoryAdapter.java`
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/mapper/DiscrepancyCaseMapper.java`
- ✅ `services/ledger-service/src/main/resources/db/migration/V3__settlement_summary.sql`
- ✅ `services/ledger-service/src/main/resources/db/migration/V5__discrepancy_case.sql`

### Step 1: Create Flyway migration

```sql
CREATE TABLE settlement_summary (
    settlement_id UUID PRIMARY KEY,
    agent_id UUID NOT NULL,
    business_date DATE NOT NULL,
    total_withdrawals DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_deposits DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_bill_payments DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_commissions DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_retail_sales DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    net_settlement DECIMAL(15,2) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    settled_at TIMESTAMP,
    CONSTRAINT uk_settlement_agent_date UNIQUE (agent_id, business_date)
);

CREATE TABLE discrepancy_case (
    case_id UUID PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL,
    discrepancy_type VARCHAR(20) NOT NULL,
    internal_amount DECIMAL(15,2),
    network_amount DECIMAL(15,2),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_MAKER',
    maker_action VARCHAR(50),
    maker_user_id VARCHAR(50),
    maker_reason TEXT,
    checker_user_id VARCHAR(50),
    checker_action VARCHAR(50),
    checker_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP
);
```

### Step 2: Create JPA entities and repository adapters

### Step 3: Run tests and commit

```bash
./gradlew :ledger-service:test
git add services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/persistence/
git commit -m "feat(ledger): add JPA entities for settlement and reconciliation"
```

---

## Task 3: Application Layer - Batch Job & Use Cases [DONE]

**BDD Scenarios:** BDD-SM01, BDD-SM02, BDD-SM03, BDD-SM04, BDD-DR01, BDD-DR02, BDD-DR03
**BRD Requirements:** US-SM01, US-SM02, US-SM03, US-SM04, US-DR01, US-DR02, US-DR03
**User-Facing:** NO

**Files:**
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/application/job/EodSettlementJob.java`
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/application/job/ReconciliationJob.java`
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/port/in/ProcessDiscrepancyUseCase.java`
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/application/usecase/ProcessDiscrepancyUseCaseImpl.java`

### Step 1: Create EOD Settlement batch job

```java
package com.agentbanking.ledger.application.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EodSettlementJob {

    private final SettlementCalculationService settlementService;
    private final SettlementSummaryRepository settlementRepository;

    @Scheduled(cron = "0 59 23 * * *", zone = "Asia/Kuala_Lumpur")
    public void executeDailySettlement() {
        // 1. Get all active agents
        // 2. For each agent, calculate daily settlement
        // 3. Save settlement summary
        // 4. Generate CBS file if no discrepancies
    }
}
```

### Step 2: Create Reconciliation batch job

```java
package com.agentbanking.ledger.application.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReconciliationJob {

    private final ReconciliationService reconciliationService;

    @Scheduled(cron = "0 30 0 * * *", zone = "Asia/Kuala_Lumpur")
    public void executeReconciliation() {
        // 1. Extract internal ledger data
        // 2. Extract PayNet PSR data
        // 3. Perform triple-match
        // 4. Create discrepancy cases for Ghost/Orphan/Mismatch
    }
}
```

### Step 3: Create discrepancy resolution use case (maker-checker)

```java
package com.agentbanking.ledger.domain.port.in;

public interface ProcessDiscrepancyUseCase {
    void makerPropose(MakerCommand command);
    void checkerApprove(CheckerCommand command);
    void checkerReject(CheckerCommand command);

    record MakerCommand(String caseId, String action, String userId, String reason) {}
    record CheckerCommand(String caseId, String userId, String reason) {}
}
```

### Step 4: Run tests and commit

```bash
./gradlew :ledger-service:test
git add services/ledger-service/src/main/java/com/agentbanking/ledger/application/job/
git add services/ledger-service/src/main/java/com/agentbanking/ledger/domain/port/in/ProcessDiscrepancyUseCase.java
git add services/ledger-service/src/main/java/com/agentbanking/ledger/application/usecase/ProcessDiscrepancyUseCaseImpl.java
git commit -m "feat(ledger): add EOD settlement and reconciliation batch jobs"
```

---

## Task 4: Infrastructure Layer - REST Controllers [DONE]

**BDD Scenarios:** BDD-SM01, BDD-SM02, BDD-DR01, BDD-DR02, BDD-DR03
**BRD Requirements:** US-SM01, US-SM02, US-DR01, US-DR02, US-DR03
**User-Facing:** NO (internal service-to-service)

**Files:**
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/web/SettlementController.java`
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/web/ReconciliationController.java`
- ✅ DTOs embedded in ProcessDiscrepancyUseCase port

### Step 1: Create REST controller for settlement endpoints

### Step 2: Create REST controller for discrepancy resolution endpoints

### Step 3: Run tests and commit

```bash
./gradlew :ledger-service:test
git add services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/web/SettlementController.java
git add services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/web/ReconciliationController.java
git commit -m "feat(ledger): add REST controllers for settlement and reconciliation"
```

---

## Task 5: Unit Tests [DONE]

**BDD Scenarios:** BDD-SM01 through BDD-SM04, BDD-DR01 through BDD-DR03
**BRD Requirements:** US-SM01, US-SM02, US-SM03, US-SM04, US-DR01, US-DR02, US-DR03
**User-Facing:** NO

**Files:**
- ✅ `services/ledger-service/src/test/java/com/agentbanking/ledger/domain/service/SettlementServiceTest.java` (covers SettlementCalculationService scenarios)
- ✅ `services/ledger-service/src/test/java/com/agentbanking/ledger/domain/service/ReconciliationServiceTest.java`
- ✅ `services/ledger-service/src/test/java/com/agentbanking/ledger/application/usecase/ProcessDiscrepancyUseCaseImplTest.java`

### Step 1: Write unit tests for settlement calculation

### Step 2: Write unit tests for reconciliation service

### Step 3: Write unit tests for discrepancy resolution

### Step 4: Run all tests and commit

```bash
./gradlew :ledger-service:test
git add services/ledger-service/src/test/java/com/agentbanking/ledger/domain/service/SettlementCalculationServiceTest.java
git add services/ledger-service/src/test/java/com/agentbanking/ledger/domain/service/ReconciliationServiceTest.java
git add services/ledger-service/src/test/java/com/agentbanking/ledger/application/usecase/ProcessDiscrepancyUseCaseImplTest.java
git commit -m "test(ledger): add unit tests for settlement and reconciliation"
```

---