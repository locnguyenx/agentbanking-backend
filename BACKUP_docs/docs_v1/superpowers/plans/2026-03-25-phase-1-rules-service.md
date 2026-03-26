# Phase 1: Rules Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Rules Service — fee engine, daily limits, and velocity checks for the Agent Banking Platform.

**Architecture:** Hexagonal architecture. Domain layer has zero Spring/JPA imports. Controllers call use cases which call domain services.

**Tech Stack:** Java 21, Spring Boot 3.x, Spring Data JPA, PostgreSQL, Flyway, Redis

**Specs:** `docs/superpowers/specs/agent-banking-platform/`

---

## File Structure

```
services/rules-service/
├── build.gradle
├── settings.gradle
├── src/main/java/com/agentbanking/rules/
│   ├── RulesServiceApplication.java
│   ├── domain/
│   │   ├── model/
│   │   │   ├── FeeConfig.java           # Entity (JPA)
│   │   │   ├── VelocityRule.java         # Entity (JPA)
│   │   │   ├── FeeType.java              # Enum: FIXED, PERCENTAGE
│   │   │   ├── AgentTier.java            # Enum: MICRO, STANDARD, PREMIER
│   │   │   ├── TransactionType.java      # Enum
│   │   │   └── Money.java                # Value object (BigDecimal wrapper)
│   │   ├── port/
│   │   │   ├── in/
│   │   │   │   ├── FeeConfigUseCase.java      # Inbound port interface
│   │   │   │   ├── VelocityCheckUseCase.java
│   │   │   │   └── LimitCheckUseCase.java
│   │   │   └── out/
│   │   │       ├── FeeConfigRepository.java    # Outbound port interface
│   │   │       └── VelocityRuleRepository.java
│   │   └── service/
│   │       ├── FeeCalculationService.java      # Domain service (no framework)
│   │       ├── LimitEnforcementService.java
│   │       └── VelocityCheckService.java
│   ├── application/
│   │   └── usecase/
│   │       ├── CalculateFeeUseCase.java
│   │       ├── CheckVelocityUseCase.java
│   │       └── CheckLimitUseCase.java
│   ├── infrastructure/
│   │   ├── web/
│   │   │   ├── dto/
│   │   │   │   ├── FeeConfigRequest.java
│   │   │   │   ├── FeeConfigResponse.java
│   │   │   │   └── VelocityCheckRequest.java
│   │   │   └── RulesController.java
│   │   └── persistence/
│   │       ├── FeeConfigRepositoryImpl.java
│   │       ├── VelocityRuleRepositoryImpl.java
│   │       └── RulesFlywayConfig.java
│   └── config/
│       └── RulesBeans.java
├── src/main/resources/
│   ├── db/migration/V1__create_rules_tables.sql
│   └── application.yaml
└── src/test/java/com/agentbanking/rules/
    ├── domain/service/FeeCalculationServiceTest.java
    ├── application/usecase/CalculateFeeUseCaseTest.java
    └── infrastructure/web/RulesControllerTest.java
```

---

## Tasks

### Task 1: Project Setup [DONE]

### Task 2: Domain Layer — Enums and Value Objects [DONE]

### Task 3: FeeConfig Entity and Repository Port [DONE]

### Task 4: Fee Calculation Domain Service [DONE]

### Task 5: Limit and Velocity Services [DONE]

### Task 6: REST Controller [DONE]

---

## Phase 1: Rules Service - COMPLETE ✅

**BDD Scenarios:** Foundation for all Rules operations

**Files:**
- Create: `services/rules-service/build.gradle`
- Create: `services/rules-service/settings.gradle`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/RulesServiceApplication.java`
- Create: `services/rules-service/src/main/resources/application.yaml`

- [ ] **Step 1: Write build.gradle**

```gradle
plugins {
    id 'java-library'
    id 'org.springframework.boot' version '3.2.5'
    id 'io.spring.dependency-management' version '1.1.5'
    id 'org.flywaydb.flyway' version '10.4.1'
}

group = 'com.agentbanking'
version = '0.0.1-SNAPSHOT'

java {
    sourceCompatibility = '21'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.postgresql:postgresql'
    implementation project(':common')
    
    flywayPostgresImplementation 'org.flywaydb:flyway-database-postgresql'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('bootJar') {
    archiveBaseName = 'rules-service'
}
```

- [ ] **Step 2: Write application.yaml**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rules_db
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: none
  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: 8081
```

- [ ] **Step 3: Write application class**

```java
package com.agentbanking.rules;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RulesServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RulesServiceApplication.class, args);
    }
}
```

- [ ] **Step 4: Verify compiles**

Run: `./gradlew :services:rules-service:compileJava`

- [ ] **Step 5: Commit**

```bash
git add services/rules-service/
git commit -m "feat(rules-service): scaffold project structure"
```

---

### Task 2: Domain Layer — Enums and Value Objects

**BDD Scenarios:** All fee and limit calculations

**Files:**
- Create: `domain/model/AgentTier.java`
- Create: `domain/model/FeeType.java`
- Create: `domain/model/TransactionType.java`
- Create: `domain/model/Money.java`

- [ ] **Step 1: Write AgentTier.java**

```java
package com.agentbanking.rules.domain.model;

public enum AgentTier {
    MICRO, STANDARD, PREMIER
}
```

- [ ] **Step 2: Write FeeType.java**

```java
package com.agentbanking.rules.domain.model;

public enum FeeType {
    FIXED, PERCENTAGE
}
```

- [ ] **Step 3: Write TransactionType.java**

```java
package com.agentbanking.rules.domain.model;

public enum TransactionType {
    CASH_WITHDRAWAL,
    CASH_DEPOSIT,
    BALANCE_INQUIRY,
    DUITNOW_TRANSFER,
    JOMPAY,
    ASTRO_RPN,
    TM_RPN,
    EPF_PAYMENT,
    CELCOM_TOPUP,
    M1_TOPUP,
    SARAWAK_PAY_WITHDRAWAL,
    SARAWAK_PAY_TOPUP,
    ESSP_PURCHASE,
    PIN_PURCHASE,
    CASHLESS_PAYMENT
}
```

- [ ] **Step 4: Write Money.java (value object)**

```java
package com.agentbanking.rules.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount) {
    public Money {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount.setScale(2, RoundingMode.HALF_UP));
    }

    public Money multiply(BigDecimal factor) {
        return new Money(amount.multiply(factor).setScale(2, RoundingMode.HALF_UP));
    }
}
```

- [ ] **Step 5: Write failing test**

```java
@Test void testMoneyCreation() {
    Money m = Money.of(new BigDecimal("100.00"));
    assertEquals("100.00", m.amount().toPlainString());
}

@Test void testMoneyMultiply() {
    Money m = Money.of(new BigDecimal("100.00"));
    Money result = m.multiply(new BigDecimal("0.05"));
    assertEquals("5.00", result.amount().toPlainString());
}
```

- [ ] **Step 6: Verify tests pass**

- [ ] **Step 7: Commit**

---

### Task 3: FeeConfig Entity and Repository Port

**BDD Scenarios:** BDD-R01, BDD-R01-PCT, BDD-R01-EC-01, BDD-R01-EC-02, BDD-R01-EC-03

**Files:**
- Create: `domain/model/FeeConfig.java`
- Create: `domain/port/out/FeeConfigRepository.java`
- Create: `db/migration/V1__create_rules_tables.sql`

- [ ] **Step 1: Write migration**

```sql
CREATE TABLE fee_config (
    fee_config_id UUID PRIMARY KEY,
    transaction_type VARCHAR(50) NOT NULL,
    agent_tier VARCHAR(20) NOT NULL,
    fee_type VARCHAR(20) NOT NULL,
    customer_fee_value DECIMAL(15,4) NOT NULL,
    agent_commission_value DECIMAL(15,4) NOT NULL,
    bank_share_value DECIMAL(15,4) NOT NULL,
    daily_limit_amount DECIMAL(15,2),
    daily_limit_count INTEGER,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_fee_config UNIQUE (transaction_type, agent_tier, effective_from)
);

CREATE INDEX idx_fee_config_lookup ON fee_config(transaction_type, agent_tier, effective_from);

CREATE TABLE velocity_rule (
    rule_id UUID PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    max_transactions_per_day INTEGER NOT NULL,
    max_amount_per_day DECIMAL(15,2) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    transaction_type VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_velocity_active ON velocity_rule(is_active);
```

- [ ] **Step 2: Write FeeConfig entity (JPA)**

```java
package com.agentbanking.rules.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fee_config")
public class FeeConfig {
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
    
    // Getters and setters
}
```

- [ ] **Step 3: Write repository port (interface)**

```java
package com.agentbanking.rules.domain.port.out;

import com.agentbanking.rules.domain.model.AgentTier;
import com.agentbanking.rules.domain.model.FeeConfig;
import com.agentbanking.rules.domain.model.TransactionType;
import java.time.LocalDate;
import java.util.Optional;

public interface FeeConfigRepository {
    Optional<FeeConfig> findByTransactionTypeAndAgentTier(
        TransactionType transactionType, 
        AgentTier agentTier,
        LocalDate asOfDate);
}
```

- [ ] **Step 4: Write test**

```java
@Test void shouldFindActiveFeeConfig() {
    Optional<FeeConfig> config = repo.findByTransactionTypeAndAgentTier(
        TransactionType.CASH_WITHDRAWAL, AgentTier.MICRO, LocalDate.now());
    assertTrue(config.isPresent());
}
```

- [ ] **Step 5: Implement repository (JPA)**

```java
@Repository
public class FeeConfigRepositoryImpl implements FeeConfigRepository {
    @PersistenceContext
    private EntityManager em;
    
    @Override
    public Optional<FeeConfig> findByTransactionTypeAndAgentTier(...) {
        // JPQL query with effective date range
    }
}
```

- [ ] **Step 6: Commit**

---

### Task 4: Fee Calculation Domain Service

**BDD Scenarios:** BDD-R01, BDD-R01-PCT, BDD-R04, BDD-R01-EC-03

**Files:**
- Create: `domain/service/FeeCalculationService.java`

- [ ] **Step 1: Write failing test**

```java
@Test
void shouldCalculateFixedFee() {
    FeeConfig config = FeeConfig.builder()
        .feeType(FeeType.FIXED)
        .customerFeeValue(new BigDecimal("1.00"))
        .agentCommissionValue(new BigDecimal("0.20"))
        .bankShareValue(new BigDecimal("0.80"))
        .build();
    
    var result = service.calculate(new BigDecimal("500.00"), config);
    assertEquals(new BigDecimal("1.00"), result.customerFee());
}

@Test
void shouldCalculatePercentageFee() {
    FeeConfig config = FeeConfig.builder()
        .feeType(FeeType.PERCENTAGE)
        .customerFeeValue(new BigDecimal("0.005"))
        .agentCommissionValue(new BigDecimal("0.002"))
        .bankShareValue(new BigDecimal("0.003"))
        .build();
    
    var result = service.calculate(new BigDecimal("333.33"), config);
    assertEquals(new BigDecimal("1.67"), result.customerFee()); // HALF_UP rounding
}
```

- [ ] **Step 2: Write FeeCalculationService**

```java
package com.agentbanking.rules.domain.service;

import com.agentbanking.rules.domain.model.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class FeeCalculationService {
    
    public FeeCalculationResult calculate(BigDecimal amount, FeeConfig config) {
        BigDecimal customerFee;
        
        if (config.getFeeType() == FeeType.FIXED) {
            customerFee = config.getCustomerFeeValue();
        } else {
            customerFee = amount.multiply(config.getCustomerFeeValue())
                .setScale(2, RoundingMode.HALF_UP);
        }
        
        BigDecimal agentCommission = calculateComponent(amount, config.getAgentCommissionValue(), config.getFeeType());
        BigDecimal bankShare = calculateComponent(amount, config.getBankShareValue(), config.getFeeType());
        
        return new FeeCalculationResult(customerFee, agentCommission, bankShare);
    }
    
    private BigDecimal calculateComponent(BigDecimal amount, BigDecimal value, FeeType feeType) {
        if (feeType == FeeType.FIXED) return value;
        return amount.multiply(value).setScale(2, RoundingMode.HALF_UP);
    }
    
    public record FeeCalculationResult(BigDecimal customerFee, BigDecimal agentCommission, BigDecimal bankShare) {}
}
```

- [ ] **Step 3: Verify tests pass**

- [ ] **Step 4: Commit**

---

### Task 5: Limit and Velocity Services

**BDD Scenarios:** BDD-R02, BDD-R02-EC-01 through EC-04, BDD-R03, BDD-R03-EC-01 through EC-04

**Files:**
- Create: `domain/service/LimitEnforcementService.java`
- Create: `domain/service/VelocityCheckService.java`

- [ ] **Step 1: Write LimitEnforcementService**

```java
public class LimitEnforcementService {
    public boolean checkDailyLimit(BigDecimal amount, FeeConfig config, BigDecimal todayTotalAmount, int todayTransactionCount) {
        // Check amount limit
        if (config.getDailyLimitAmount() != null) {
            BigDecimal projected = todayTotalAmount.add(amount);
            if (projected.compareTo(config.getDailyLimitAmount()) > 0) {
                return false;
            }
        }
        // Check count limit
        if (config.getDailyLimitCount() != null) {
            if (todayTransactionCount >= config.getDailyLimitCount()) {
                return false;
            }
        }
        return true;
    }
}
```

- [ ] **Step 2: Write VelocityCheckService**

```java
public class VelocityCheckService {
    public boolean check(String mykad, int transactionCountToday, BigDecimal amountToday) {
        // Find active velocity rule
        // Check count vs maxTransactionsPerDay
        // Check amount vs maxAmountPerDay
        return true;
    }
}
```

- [ ] **Step 3: Write tests covering all edge cases**

- [ ] **Step 4: Commit**

---

### Task 6: REST Controller

**BDD Scenarios:** All

**Files:**
- Create: `infrastructure/web/dto/...Request.java`
- Create: `infrastructure/web/RulesController.java`

- [ ] **Step 1: Write controller**

```java
@RestController
@RequestMapping("/internal")
public class RulesController {
    
    @GetMapping("/fees/{transactionType}/{agentTier}")
    public ResponseEntity<FeeConfigResponse> getFeeConfig(
            @PathVariable TransactionType type,
            @PathVariable AgentTier tier) {
        // Call use case, return response
    }
    
    @PostMapping("/check-velocity")
    public ResponseEntity<Void> checkVelocity(@RequestBody VelocityCheckRequest request) {
        // Call use case, return 200 or error
    }
    
    @GetMapping("/limits/{transactionType}/{agentTier}")
    public ResponseEntity<LimitResponse> getLimits(...) {}
}
```

- [ ] **Step 2: Write tests**

- [ ] **Step 3: Commit**

---

## Summary

| Task | Coverage |
|------|----------|
| 1 | Project scaffold |
| 2 | Domain primitives |
| 3 | FeeConfig entity + repository |
| 4 | Fee calculation (FR-1.1, FR-1.4, BDD-R01, R04) |
| 5 | Limit + velocity (FR-1.2, FR-1.3, BDD-R02, R03) |
| 6 | REST controller |