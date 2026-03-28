# Rules Service Implementation Plan (US-R01-R04) [DONE]

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## Status: COMPLETED (2026-03-27)

All tasks completed. Rules service is fully operational with domain, infrastructure, and application layers.

**Goal:** Implement Rules Service functionality for fee configuration, transaction limits, velocity checks, and fee calculation per user stories US-R01 through US-R04.

**Architecture:** Hexagonal (Ports & Adapters) pattern with domain layer containing pure business logic (zero framework imports), application layer containing use cases, and infrastructure layer containing adapters (JPA repositories, Feign clients, controllers).

**Tech Stack:** Java 21, Spring Boot 3.2.5, Spring Data JPA, PostgreSQL, Flyway, JUnit 5, Mockito, ArchUnit.

---

## Task 1: Domain Layer - Entities & Enums [DONE]

**BDD Scenarios:** BDD-R01, BDD-R01-PCT, BDD-R02, BDD-R03, BDD-R04  
**BRD Requirements:** US-R01, US-R02, US-R03, US-R04  
**User-Facing:** NO  

**Files:**
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/domain/model/FeeType.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/domain/model/AgentTier.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/domain/model/FeeConfigRecord.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/domain/model/VelocityRuleRecord.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/domain/model/TransactionType.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/domain/port/out/FeeConfigRepository.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/domain/port/out/VelocityRuleRepository.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/domain/service/FeeCalculationService.java`

### Step 1: Create FeeType enum

```java
package com.agentbanking.rules.domain.model;

public enum FeeType {
    FIXED,
    PERCENTAGE
}
```

### Step 2: Create AgentTier enum

```java
package com.agentbanking.rules.domain.model;

public enum AgentTier {
    MICRO,
    STANDARD,
    PREMIER
}
```

### Step 3: Create FeeConfigRecord

```java
package com.agentbanking.rules.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record FeeConfigRecord(
    String transactionType,
    AgentTier agentTier,
    FeeType feeType,
    BigDecimal customerFeeValue,
    BigDecimal agentCommissionValue,
    BigDecimal bankShareValue,
    BigDecimal dailyLimitAmount,
    Integer dailyLimitCount,
    Instant createdAt,
    Instant updatedAt
) {}
```

### Step 4: Create VelocityRuleRecord

```java
package com.agentbanking.rules.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record VelocityRuleRecord(
    String transactionType,
    String customerMykadPattern,
    BigDecimal maxAmountPerTransaction,
    BigDecimal maxAmountPerDay,
    Integer maxCountPerDay,
    Instant createdAt,
    Instant updatedAt
) {}
```

### Step 5: Create TransactionType enum

```java
package com.agentbanking.rules.domain.model;

public enum TransactionType {
    CASH_WITHDRAWAL,
    CASH_DEPOSIT,
    BALANCE_INQUIRY,
    DUTNOW_TRANSFER,
    BILL_PAYMENT,
    PREPAID_TOPUP,
    E_WALLET_WITHDRAWAL,
    E_WALLET_TOPUP,
    ESSP_PURCHASE,
    RETAIL_SALE,
    PIN_PURCHASE,
    CASH_BACK
}
```

### Step 6: Create FeeConfigRepository port

```java
package com.agentbanking.rules.domain.port.out;

import com.agentbanking.rules.domain.model.FeeConfigRecord;
import java.util.Optional;

public interface FeeConfigRepository {
    Optional<FeeConfigRecord> findByTransactionTypeAndAgentTier(String transactionType, String agentTier);
    FeeConfigRecord save(FeeConfigRecord feeConfig);
}
```

### Step 7: Create VelocityRuleRepository port

```java
package com.agentbanking.rules.domain.port.out;

import com.agentbanking.rules.domain.model.VelocityRuleRecord;
import java.util.List;
import java.util.Optional;

public interface VelocityRuleRepository {
    List<VelocityRuleRecord> findByTransactionTypeAndMykadPattern(String transactionType, String mykad);
    VelocityRuleRecord save(VelocityRuleRecord velocityRule);
}
```

### Step 8: Create FeeCalculationService domain service

```java
package com.agentbanking.rules.domain.service;

import com.agentbanking.rules.domain.model.AgentTier;
import com.agentbanking.rules.domain.model.FeeConfigRecord;
import com.agentbanking.rules.domain.model.TransactionType;
import java.math.BigDecimal;

public class FeeCalculationService {

    private final FeeConfigRepository feeConfigRepository;

    public FeeCalculationService(FeeConfigRepository feeConfigRepository) {
        this.feeConfigRepository = feeConfigRepository;
    }

    public FeeConfigRecord getFeeConfig(TransactionType transactionType, AgentTier agentTier) {
        return feeConfigRepository.findByTransactionTypeAndAgentTier(
                transactionType.name(), agentTier.name())
                .orElseThrow(() -> new IllegalArgumentException("ERR_FEE_CONFIG_NOT_FOUND"));
    }

    public BigDecimal calculateCustomerFee(BigDecimal amount, FeeConfigRecord feeConfig) {
        if (feeConfig.feeType() == FeeType.FIXED) {
            return feeConfig.customerFeeValue();
        } else {
            return amount.multiply(feeConfig.customerFeeValue()).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        }
    }

    public BigDecimal calculateAgentCommission(BigDecimal amount, FeeConfigRecord feeConfig) {
        if (feeConfig.feeType() == FeeType.FIXED) {
            return feeConfig.agentCommissionValue();
        } else {
            return amount.multiply(feeConfig.agentCommissionValue()).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        }
    }

    public BigDecimal calculateBankShare(BigDecimal amount, FeeConfigRecord feeConfig) {
        if (feeConfig.feeType() == FeeType.FIXED) {
            return feeConfig.bankShareValue();
        } else {
            return amount.multiply(feeConfig.bankShareValue()).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        }
    }
}
```

### Step 9: Commit

```bash
git add services/rules-service/src/main/java/com/agentbanking/rules/domain/
git commit -m "feat(rules): add domain entities, enums, ports and fee calculation service"
```

---

## Task 2: Infrastructure Layer - JPA Adapters [DONE]

**BDD Scenarios:** BDD-R01, BDD-R02, BDD-R03  
**BRD Requirements:** US-R01, US-R02, US-R03  
**User-Facing:** NO  

**Files:**
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/persistence/entity/FeeConfigEntity.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/persistence/entity/VelocityRuleEntity.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/persistence/repository/FeeConfigJpaRepository.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/persistence/repository/VelocityRuleJpaRepository.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/persistence/adapter/FeeConfigRepositoryAdapter.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/persistence/adapter/VelocityRuleRepositoryAdapter.java`
- Create: Flyway migration: `services/rules-service/src/main/resources/db/migration/V2__fee_config_velocity_rule.sql`

### Step 1: Create Flyway migration

```sql
CREATE TABLE fee_config (
    id BIGSERIAL PRIMARY KEY,
    transaction_type VARCHAR(50) NOT NULL,
    agent_tier VARCHAR(20) NOT NULL,
    fee_type VARCHAR(20) NOT NULL,
    customer_fee_value DECIMAL(15,2) NOT NULL,
    agent_commission_value DECIMAL(15,2) NOT NULL,
    bank_share_value DECIMAL(15,2) NOT NULL,
    daily_limit_amount DECIMAL(15,2) NOT NULL,
    daily_limit_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_fee_config UNIQUE (transaction_type, agent_tier)
);

CREATE TABLE velocity_rule (
    id BIGSERIAL PRIMARY KEY,
    transaction_type VARCHAR(50) NOT NULL,
    customer_mykad_pattern VARCHAR(20) NOT NULL,
    max_amount_per_transaction DECIMAL(15,2),
    max_amount_per_day DECIMAL(15,2),
    max_count_per_day INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### Step 2: Create JPA entities and repository adapters

### Step 3: Run tests and commit

```bash
./gradlew :rules-service:test
git add services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/persistence/
git commit -m "feat(rules): add JPA entities and repository adapters"
```

---

## Task 3: Application Layer - Use Cases [DONE]

**BDD Scenarios:** BDD-R01, BDD-R01-PCT, BDD-R02, BDD-R03, BDD-R04  
**BRD Requirements:** US-R01, US-R02, US-R03, US-R04  
**User-Facing:** NO  

**Files:**
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/domain/port/in/FeeQueryUseCase.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/domain/port/in/VelocityCheckUseCase.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/domain/port/in/LimitEnforcementUseCase.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/application/usecase/FeeQueryUseCaseImpl.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/application/usecase/VelocityCheckUseCaseImpl.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/application/usecase/LimitEnforcementUseCaseImpl.java`

### Step 1: Create inbound port interfaces

### Step 2: Create use case implementations

### Step 3: Run tests and commit

```bash
./gradlew :rules-service:test
git add services/rules-service/src/main/java/com/agentbanking/rules/domain/port/in/
git add services/rules-service/src/main/java/com/agentbanking/rules/application/
git commit -m "feat(rules): add use cases for fee query, velocity check, limit enforcement"
```

---

## Task 4: Infrastructure Layer - REST Controllers [DONE]

**BDD Scenarios:** BDD-R01, BDD-R02, BDD-R03, BDD-R04  
**BRD Requirements:** US-R01, US-R02, US-R03, US-R04  
**User-Facing:** NO (internal service-to-service endpoints)  

**Files:**
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/web/RulesController.java`
- Create: DTOs for request/response
- Modify: `services/rules-service/src/main/java/com/agentbanking/rules/config/RulesServiceConfig.java`

### Step 1: Create REST controller

### Step 2: Run integration tests

### Step 3: Commit

```bash
./gradlew :rules-service:test
git add services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/web/
git commit -m "feat(rules): add REST controllers for internal APIs"
```

---

## Task 5: Unit Tests [DONE]

**BDD Scenarios:** BDD-R01 through BDD-R04 and all edge cases  
**BRD Requirements:** US-R01, US-R02, US-R03, US-R04  
**User-Facing:** NO  

**Files:**
- Create: `services/rules-service/src/test/java/com/agentbanking/rules/domain/service/FeeCalculationServiceTest.java`
- Create: `services/rules-service/src/test/java/com/agentbanking/rules/application/usecase/FeeQueryUseCaseImplTest.java`
- Create: `services/rules-service/src/test/java/com/agentbanking/rules/application/usecase/VelocityCheckUseCaseImplTest.java`
- Create: `services/rules-service/src/test/java/com/agentbanking/rules/architecture/HexagonalArchitectureTest.java`

### Step 1: Write unit tests for fee calculation (BDD-R01, BDD-R01-PCT)

### Step 2: Write unit tests for velocity checks (BDD-R03, BDD-R03-EC-01 through EC-04)

### Step 3: Write unit tests for limit enforcement (BDD-R02, BDD-R02-EC-01 through EC-04)

### Step 4: Write ArchUnit test

### Step 5: Run all tests and commit

```bash
./gradlew :rules-service:test
git add services/rules-service/src/test/
git commit -m "test(rules): add unit tests and ArchUnit validation for Rules Service"
```

---