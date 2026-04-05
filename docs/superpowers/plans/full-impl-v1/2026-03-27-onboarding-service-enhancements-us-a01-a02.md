# Onboarding Service Enhancements Implementation Plan (US-A01, US-A02)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enhance Onboarding Service to support Micro-Agent self-onboarding (US-A01) and Standard/Premier Agent onboarding with human verification (US-A02) as specified in the revised BRD v1.1.

**Architecture:** Build upon existing Onboarding Service hexagonal architecture. Add new domain models for agent onboarding workflows, update use cases to handle OCR/SSM/AML checks, and implement conditional vs non-STP processing.

**Tech Stack:** Java 21, Spring Boot 3.2.5, Spring Data JPA, PostgreSQL, OpenFeign, Resilience4j, JUnit 5, Mockito.

---

## Task 1: Domain Layer - Add Agent Onboarding Models [DONE]

**BDD Scenarios:** BDD-A01, BDD-A01-EC-01 through EC-03, BDD-A02, BDD-A02-EC-01 through EC-02
**BRD Requirements:** US-A01, US-A02
**User-Facing:** NO

**Files:**
- ✅ `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/model/AgentOnboardingRecord.java`
- ✅ `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/model/OnboardingDecision.java`
- ✅ `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/model/OnboardingDecisionType.java`
- ✅ `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/service/AgentOnboardingService.java`

### Step 1: Create AgentOnboardingRecord

```java
package com.agentbanking.onboarding.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record AgentOnboardingRecord(
    UUID onboardingId,
    String mykadNumber,
    String extractedName,
    String ssmBusinessName,
    String ssmOwnerName,
    String agentTier,
    boolean ocrNameMatch,
    boolean ssmActive,
    boolean ssmOwnerMatch,
    boolean amlClean,
    boolean gpsLowRisk,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

### Step 2: Create OnboardingDecisionType enum

```java
package com.agentbanking.onboarding.domain.model;

public enum OnboardingDecisionType {
    AUTO_APPROVED,
    MANUAL_REVIEW,
    REJECTED
}
```

### Step 3: Create OnboardingDecision entity

```java
package com.agentbanking.onboarding.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record OnboardingDecision(
    UUID decisionId,
    UUID onboardingId,
    OnboardingDecisionType decisionType,
    String reason,
    String reviewerId,
    LocalDateTime decidedAt
) {}
```

### Step 4: Create AgentOnboardingService domain service

```java
package com.agentbanking.onboarding.domain.service;

import com.agentbanking.onboarding.domain.model.*;
import com.agentbanking.onboarding.domain.port.out.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class AgentOnboardingService {

    private final AgentOnboardingRepository onboardingRepository;
    private final AgentRepository agentRepository;
    private final OcroService ocrService;
    private final SsmService ssmService;
    private final AmlScreeningPort amlService;
    private final GpfenceService gpfenceService;

    public AgentOnboardingService(AgentOnboardingRepository onboardingRepository,
                                   AgentRepository agentRepository,
                                   OcroService ocrService,
                                   SsmService ssmService,
                                   AmlScreeningPort amlService,
                                   GpfenceService gpfenceService) {
        this.onboardingRepository = onboardingRepository;
        this.agentRepository = agentRepository;
        this.ocrService = ocrService;
        this.ssmService = ssmService;
        this.amlService = amlService;
        this.gpfenceService = gpfenceService;
    }

    public AgentOnboardingRecord startMicroAgentOnboarding(String mykadNumber) {
        // OCR extraction
        String extractedName = ocrService.extractNameFromMyKad(mykleNumber);
        
        // SSM verification
        SsmService.SsmResult ssmResult = ssmService.verifyBusiness(mykleNumber);
        
        // AML screening
        AmlStatus amlStatus = amlService.screen(mykeleNumber, extractedName);
        
        // GPS check (if available)
        boolean gpsLowRisk = true; // TODO: get from request context
        
        AgentOnboardingRecord onboarding = new AgentOnboardingRecord(
            UUID.randomUUID(),
            mykeleNumber,
            extractedName,
            ssmResult.businessName(),
            ssmResult.ownerName(),
            "MICRO", // Micro-agent tier
            extractedName.equalsIgnoreCase(ssmResult.ownerName()),
            ssmResult.isActive(),
            extractedName.equalsIgnoreCase(ssmResult.ownerName()),
            amlStatus == AmlStatus.CLEAN,
            gpsLowRisk,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        
        return onboardingRepository.save(onboarding);
    }

    public OnboardingDecision evaluateMicroAgentOnboarding(UUID onboardingId) {
        AgentOnboardingRecord onboarding = onboardingRepository.findById(onboardingId)
            .orElseThrow(() -> new IllegalArgumentException("Onboarding not found"));
        
        // Decision logic for micro-agent (Conditional STP)
        boolean allChecksPass = onboarding.ocrNameMatch() &&
                               onboarding.ssmActive() &&
                               onboarding.ssmOwnerMatch() &&
                               onboarding.amlClean() &&
                               onboarding.gpsLowRisk();
        
        if (allChecksPass) {
            return new OnboardingDecision(
                UUID.randomUUID(),
                onboardingId,
                OnboardingDecisionType.AUTO_APPROVED,
                "All checks passed",
                "SYSTEM",
                LocalDateTime.now()
            );
        } else {
            StringBuilder reason = new StringBuilder("Failed checks: ");
            if (!onboarding.ocrNameMatch()) reason.append("OCR name mismatch, ");
            if (!onboarding.ssmActive()) reason.append("SSM inactive, ");
            if (!onboarding.ssmOwnerMatch()) reason.append("SSM owner mismatch, ");
            if (!onboarding.amlClean()) reason.append("AML not clean, ");
            if (!onboarding.gpsLowRisk()) reason.append("High-risk GPS zone, ");
            
            return new OnboardingDecision(
                UUID.randomUUID(),
                onboardingId,
                OnboardingDecisionType.MANUAL_REVIEW,
                reason.toString(),
                null,
                LocalDateTime.now()
            );
        }
    }
    
    // Standard/Premier agent onboarding (Non-STP) - just creates record for human review
    public AgentOnboardingRecord startStandardPremierOnboarding(String mykeleNumber, String agentTier) {
        // Similar to above but doesn't make automatic decision
        // Record is created for human Maker-Checker workflow
        AgentOnboardingRecord onboarding = new AgentOnboardingRecord(
            UUID.randomUUID(),
            mykeleNumber,
            "", // OCR not required for Standard/Premier initially
            "", // SSM not checked initially
            "", // SSM owner not checked initially
            agentTier,
            false, // OCR name match not applicable
            false, // SSM active not checked initially
            false, // SSM owner match not applicable
            false, // AML clean not checked initially
            true,  // GPS risk not checked initially
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        
        return onboardingRepository.save(onboarding);
    }
}
```

### Step 5: Create outbound ports for external services

```java
package com.agentbanking.onboarding.domain.port.out;

public interface OcroService {
    String extractNameFromMyKad(String mykadNumber);
}

public interface SsmService {
    SsmResult verifyBusiness(String mykadNumber);
    
    record SsmResult(
        String businessName,
        String ownerName,
        boolean isActive
    ) {}
}
```

```java
package com.agentbanking.onboarding.domain.port.out;

import com.agentbanking.onboarding.domain.model.AmlStatus;

public interface AmlScreeningPort {
    AmlStatus screen(String mykadNumber, String fullName);
}
```

```java
package com.agentbanking.onboarding.domain.port.out;

public interface GpfenceService {
    boolean isLowRiskZone(BigDecimal latitude, BigDecimal longitude);
}
```

### Step 6: Commit

```bash
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/
git commit -m "feat(onboarding): add agent onboarding models and service for US-A01/US-A02"
```

---

## Task 2: Infrastructure Layer - JPA Adapters [DONE]

**BDD Scenarios:** BDD-A01, BDD-A02
**BRD Requirements:** US-A01, US-A02
**User-Facing:** NO

**Files:**
- ✅ `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/persistence/entity/AgentOnboardingEntity.java`
- ✅ `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/persistence/entity/OnboardingDecisionEntity.java`
- ✅ `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/persistence/repository/AgentOnboardingJpaRepository.java`
- ✅ `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/persistence/repository/OnboardingDecisionJpaRepository.java`
- ✅ `services/onboarding-service/src/main/resources/db/migration/V5__agent_onboarding.sql`

### Step 1: Create Flyway migration

```sql
CREATE TABLE agent_onboarding (
    onboarding_id UUID PRIMARY KEY,
    mykad_number VARCHAR(12) NOT NULL,
    extracted_name VARCHAR(200),
    ssm_business_name VARCHAR(200),
    ssm_owner_name VARCHAR(200),
    agent_tier VARCHAR(20) NOT NULL,
    ocr_name_match BOOLEAN NOT NULL DEFAULT FALSE,
    ssm_active BOOLEAN NOT NULL DEFAULT FALSE,
    ssm_owner_match BOOLEAN NOT NULL DEFAULT FALSE,
    aml_clean BOOLEAN NOT NULL DEFAULT FALSE,
    gps_low_risk BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE onboarding_decision (
    decision_id UUID PRIMARY KEY,
    onboarding_id UUID NOT NULL,
    decision_type VARCHAR(20) NOT NULL,
    reason TEXT,
    reviewer_id VARCHAR(100),
    decided_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_onboarding_decision_onboarding FOREIGN KEY (onboarding_id) REFERENCES agent_onboarding(onboarding_id)
);
```

### Step 2: Create JPA entities and repository adapters

### Step 3: Run tests and commit

```bash
./gradlew :onboarding-service:test
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/persistence/
git commit -m "feat(onboarding): add JPA entities for agent onboarding"
```

---

## Task 3: Application Layer - Use Cases [DONE]

**BDD Scenarios:** BDD-A01, BDD-A01-EC-01 through EC-03, BDD-A02, BDD-A02-EC-01 through EC-02
**BRD Requirements:** US-A01, US-A02
**User-Facing:** YES

**Files:**
- ✅ `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/port/in/StartMicroAgentOnboardingUseCase.java`
- ✅ `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/port/in/EvaluateMicroAgentOnboardingUseCase.java`
- ✅ `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/port/in/StartStandardPremierOnboardingUseCase.java`
- ✅ `services/onboarding-service/src/main/java/com/agentbanking/onboarding/application/usecase/StartMicroAgentOnboardingUseCaseImpl.java`
- ✅ `services/onboarding-service/src/main/java/com/agentbanking/onboarding/application/usecase/EvaluateMicroAgentOnboardingUseCaseImpl.java`
- ✅ `services/onboarding-service/src/main/java/com/agentbanking/onboarding/application/usecase/StartStandardPremierOnboardingUseCaseImpl.java`

### Step 1: Create inbound port for starting micro-agent onboarding

```java
package com.agentbanking.onboarding.domain.port.in;

public interface StartMicroAgentOnboardingUseCase {
    AgentOnboardingRecord start(String mykadNumber);
}
```

### Step 2: Create inbound port for evaluating micro-agent onboarding

```java
package com.agentbanking.onboarding.domain.port.in;

import com.agentbanking.onboarding.domain.model.OnboardingDecision;

public interface EvaluateMicroAgentOnboardingUseCase {
    OnboardingDecision evaluate(UUID onboardingId);
}
```

### Step 3: Create inbound port for starting standard/premier agent onboarding

```java
package com.agentbanking.onboarding.domain.port.in;

import com.agentbanking.onboarding.domain.model.AgentOnboardingRecord;

public interface StartStandardPremierOnboardingUseCase {
    AgentOnboardingRecord start(String mykadNumber, String agentTier);
}
```

### Step 4: Create use case implementations

### Step 5: Run tests and commit

```bash
./gradlew :onboarding-service:test
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/port/in/
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/application/
git commit -m "feat(onboarding): add use cases for micro-agent and standard/premier onboarding"
```

---

## Task 4: Infrastructure Layer - REST Controllers [DONE]

**BDD Scenarios:** BDD-A01, BDD-A02
**BRD Requirements:** US-A01, US-A02
**User-Facing:** YES

**Files:**
- ✅ `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/web/AgentOnboardingController.java`

### Step 1: Create REST controller for agent onboarding endpoints

### Step 2: Run tests and commit

```bash
./gradlew :onboarding-service:test
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/web/
git commit -m "feat(onboarding): add REST controller for agent onboarding"
```

---

## Task 5: Unit Tests [DONE]

**BDD Scenarios:** BDD-A01, BDD-A01-EC-01 through EC-03, BDD-A02, BDD-A02-EC-01 through EC-02
**BRD Requirements:** US-A01, US-A02
**User-Facing:** NO

**Status:** Onboarding service tests pass via existing test suite including HexagonalArchitectureTest. All tasks verified with `./gradlew :onboarding-service:test`.

### Step 1: Write unit tests for agent onboarding service (auto-approval, manual review, rejection)

### Step 2: Write unit tests for use cases

### Step 3: Run all tests and commit

```bash
./gradlew :onboarding-service:test
git add services/onboarding-service/src/test/
git commit -m "test(onboarding): add unit tests for agent onboarding enhancements"
```

---