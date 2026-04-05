# STP Processing Implementation Plan (US-S01-S03)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Straight-Through Processing (STP) logic for 100% automated transactions, conditional STP with fallback to manual review, and non-STP maker-checker workflows per user stories US-S01 through US-S03.

**Architecture:** Rules engine integration for STP decision matrix. 100% STP for standard transactions under limits. Conditional STP for edge cases. Non-STP for manual review items. Integrates with Rules Service, Onboarding Service, and Backoffice.

**Tech Stack:** Java 21, Spring Boot 3.2.5, OpenFeign, JUnit 5, Mockito, ArchUnit.

---

## Task 1: Domain Layer - STP Decision Logic [DONE]

**BDD Scenarios:** BDD-S01, BDD-S01-EC-01, BDD-S01-EC-02, BDD-S02, BDD-S03
**BRD Requirements:** US-S01, US-S02, US-S03, FR-12.1, FR-12.2, FR-12.3
**User-Facing:** NO

**Files:**
- ✅ `services/rules-service/src/main/java/com/agentbanking/rules/domain/model/StpCategory.java`
- ✅ `services/rules-service/src/main/java/com/agentbanking/rules/domain/model/StpDecision.java`
- ✅ `services/rules-service/src/main/java/com/agentbanking/rules/domain/service/StpDecisionService.java`

### Step 1: Create StpCategory enum

```java
package com.agentbanking.rules.domain.model;

public enum StpCategory {
    FULL_STP,        // 100% automated
    CONDITIONAL_STP, // Rules engine, fallback to manual
    NON_STP          // Manual maker-checker required
}
```

### Step 2: Create StpDecision entity

```java
package com.agentbanking.rules.domain.model;

import java.math.BigDecimal;

public record StpDecision(
    StpCategory category,
    boolean approved,
    String reason,
    BigDecimal velocityRemaining,
    BigDecimal limitRemaining
) {}
```

### Step 3: Create StpDecisionService domain service

```java
package com.agentbanking.rules.domain.service;

import com.agentbanking.rules.domain.model.*;
import java.math.BigDecimal;

public class StpDecisionService {

    private final VelocityCheckService velocityCheckService;
    private final LimitEnforcementService limitEnforcementService;

    public StpDecisionService(VelocityCheckService velocityCheckService,
                               LimitEnforcementService limitEnforcementService) {
        this.velocityCheckService = velocityCheckService;
        this.limitEnforcementService = limitEnforcementService;
    }

    public StpDecision evaluate(String transactionType, String customerMykad,
                                 BigDecimal amount, AgentTier agentTier) {
        // Check velocity
        VelocityCheckService.VelocityResult velocityResult = velocityCheckService
            .checkVelocity(customerMykad, transactionType, amount);

        // Check limits
        LimitEnforcementService.LimitResult limitResult = limitEnforcementService
            .checkDailyLimit(agentTier, transactionType, amount);

        // Determine STP category
        if (velocityResult.passed() && limitResult.passed()) {
            // 100% STP for standard transactions under limits
            return new StpDecision(
                StpCategory.FULL_STP,
                true,
                "Transaction approved - within velocity and limits",
                velocityResult.remainingAmount(),
                limitResult.remainingAmount()
            );
        } else if (velocityResult.passed() || limitResult.passed()) {
            // Conditional STP - partial pass
            return new StpDecision(
                StpCategory.CONDITIONAL_STP,
                false,
                "Requires manual review - partial pass",
                velocityResult.remainingAmount(),
                limitResult.remainingAmount()
            );
        } else {
            // Non-STP - both checks failed
            return new StpDecision(
                StpCategory.NON_STP,
                false,
                "Requires manual approval - velocity/limit exceeded",
                BigDecimal.ZERO,
                BigDecimal.ZERO
            );
        }
    }

    public boolean isMicroAgentAutoApproval(String agentTier, BigDecimal amount) {
        // Micro agents: auto-approval for amounts under RM 500
        return "MICRO".equals(agentTier) && amount.compareTo(new BigDecimal("500.00")) <= 0;
    }
}
```

### Step 4: Commit

```bash
git add services/rules-service/src/main/java/com/agentbanking/rules/domain/model/StpCategory.java
git add services/rules-service/src/main/java/com/agentbanking/rules/domain/model/StpDecision.java
git add services/rules-service/src/main/java/com/agentbanking/rules/domain/service/StpDecisionService.java
git commit -m "feat(rules): add STP decision logic and category determination"
```

---

## Task 2: Application Layer - Use Cases [DONE]

**BDD Scenarios:** BDD-S01, BDD-S01-EC-01, BDD-S01-EC-02, BDD-S02, BDD-S03
**BRD Requirements:** US-S01, US-S02, US-S03, FR-12.1, FR-12.2, FR-12.3
**User-Facing:** NO

**Files:**
- ✅ `services/rules-service/src/main/java/com/agentbanking/rules/domain/port/in/StpEvaluationUseCase.java`
- ✅ `services/rules-service/src/main/java/com/agentbanking/rules/application/usecase/StpEvaluationUseCaseImpl.java`

### Step 1: Create inbound port interface

```java
package com.agentbanking.rules.domain.port.in;

import java.math.BigDecimal;

public interface StpEvaluationUseCase {
    StpEvaluationResponse evaluate(StpEvaluationCommand command);

    record StpEvaluationCommand(
        String transactionType,
        String customerMykad,
        BigDecimal amount,
        String agentTier
    ) {}

    record StpEvaluationResponse(
        String category,
        boolean approved,
        String reason
    ) {}
}
```

### Step 2: Create use case implementation

### Step 3: Run tests and commit

```bash
./gradlew :rules-service:test
git add services/rules-service/src/main/java/com/agentbanking/rules/domain/port/in/StpEvaluationUseCase.java
git add services/rules-service/src/main/java/com/agentbanking/rules/application/usecase/StpEvaluationUseCaseImpl.java
git commit -m "feat(rules): add STP evaluation use case"
```

---

## Task 3: Infrastructure Layer - REST Controller [DONE]

**BDD Scenarios:** BDD-S01, BDD-S02, BDD-S03
**BRD Requirements:** US-S01, US-S02, US-S03
**User-Facing:** NO (internal service-to-service)

**Files:**
- ✅ `services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/web/StpController.java`
- ✅ DTOs embedded in StpEvaluationUseCase port (StpEvaluationCommand, StpEvaluationResponse)

### Step 1: Create REST controller for STP evaluation endpoint

### Step 2: Run tests and commit

```bash
./gradlew :rules-service:test
git add services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/web/StpController.java
git commit -m "feat(rules): add STP evaluation REST controller"
```

---

## Task 4: Unit Tests [DONE]

**BDD Scenarios:** BDD-S01 through BDD-S03
**BRD Requirements:** US-S01, US-S02, US-S03
**User-Facing:** NO

**Files:**
- ✅ `services/rules-service/src/test/java/com/agentbanking/rules/domain/service/StpDecisionServiceTest.java`
- ✅ `services/rules-service/src/test/java/com/agentbanking/rules/application/usecase/StpEvaluationUseCaseImplTest.java`

### Step 1: Write unit tests for STP decision service

### Step 2: Write unit tests for STP evaluation use case

### Step 3: Run all tests and commit

```bash
./gradlew :rules-service:test
git add services/rules-service/src/test/java/com/agentbanking/rules/domain/service/StpDecisionServiceTest.java
git add services/rules-service/src/test/java/com/agentbanking/rules/application/usecase/StpEvaluationUseCaseImplTest.java
git commit -m "test(rules): add unit tests for STP processing"
```

---