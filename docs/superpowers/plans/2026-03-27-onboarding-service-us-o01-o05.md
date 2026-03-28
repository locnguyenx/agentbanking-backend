# Onboarding Service Implementation Plan (US-O01-O05) [DONE]

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## Status: COMPLETED (2026-03-27)

All tasks completed. Onboarding service is fully operational with domain, infrastructure, and application layers.

**Goal:** Implement Onboarding Service for e-KYC verification, MyKad verification, biometric matching, auto-approval logic, and account opening per user stories US-O01 through US-O05.

**Architecture:** Hexagonal (Ports & Adapters) pattern. Database-per-service (onboarding_db). External calls to JPN via Feign client. Encrypted storage for MyKad (AES-256).

**Tech Stack:** Java 21, Spring Boot 3.2.5, Spring Data JPA, PostgreSQL, Flyway, OpenFeign, Resilience4j, JUnit 5, Mockito, ArchUnit.

---

## Task 1: Domain Layer - Entities & Ports [DONE]

**BDD Scenarios:** BDD-O01, BDD-O02, BDD-O03, BDD-O04, BDD-O05  
**BRD Requirements:** US-O01, US-O02, US-O03, US-O04, US-O05  
**User-Facing:** YES (KYC verification endpoints)  

**Files:**
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/model/KycStatus.java`
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/model/KycVerification.java`
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/model/AmlStatus.java`
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/port/out/KycVerificationRepository.java`
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/port/out/JpnClientPort.java`
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/port/out/AmlScreeningPort.java`
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/service/KycVerificationService.java`

### Step 1: Create KycStatus enum

```java
package com.agentbanking.onboarding.domain.model;

public enum KycStatus {
    PENDING,
    MYKAD_VERIFIED,
    BIOMETRIC_MATCHED,
    AUTO_APPROVED,
    MANUAL_REVIEW,
    APPROVED,
    REJECTED
}
```

### Step 2: Create AmlStatus enum

```java
package com.agentbanking.onboarding.domain.model;

public enum AmlStatus {
    PENDING,
    CLEAN,
    FLAGGED,
    BLOCKED
}
```

### Step 3: Create KycVerification entity

```java
package com.agentbanking.onboarding.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class KycVerification {
    private final UUID verificationId;
    private final String mykadNumber; // encrypted
    private String fullName;
    private LocalDate dateOfBirth;
    private String address;
    private KycStatus kycStatus;
    private AmlStatus amlStatus;
    private BigDecimal biometricMatchScore;
    private String rejectionReason;
    private final Instant createdAt;
    private Instant updatedAt;

    public void markMyKadVerified(String fullName, LocalDate dateOfBirth, String address) {
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.kycStatus = KycStatus.MYKAD_VERIFIED;
        this.updatedAt = Instant.now();
    }

    public void markBiometricMatched(BigDecimal score) {
        this.biometricMatchScore = score;
        if (score.compareTo(BigDecimal.valueOf(85)) >= 0) {
            this.kycStatus = KycStatus.BIOMETRIC_MATCHED;
        } else {
            this.kycStatus = KycStatus.MANUAL_REVIEW;
            this.rejectionReason = "Biometric match below threshold";
        }
        this.updatedAt = Instant.now();
    }

    public void markAutoApproved() {
        if (this.kycStatus == KycStatus.BIOMETRIC_MATCHED && this.amlStatus == AmlStatus.CLEAN) {
            this.kycStatus = KycStatus.AUTO_APPROVED;
        }
        this.updatedAt = Instant.now();
    }

    public void markManualReview() {
        this.kycStatus = KycStatus.MANUAL_REVIEW;
        this.updatedAt = Instant.now();
    }

    public void approve() {
        this.kycStatus = KycStatus.APPROVED;
        this.updatedAt = Instant.now();
    }

    public void reject(String reason) {
        this.kycStatus = KycStatus.REJECTED;
        this.rejectionReason = reason;
        this.updatedAt = Instant.now();
    }

    public boolean isEligibleForAutoApproval() {
        return this.kycStatus == KycStatus.BIOMETRIC_MATCHED
            && this.amlStatus == AmlStatus.CLEAN;
    }

    // Getters...
}
```

### Step 4: Create outbound ports

```java
package com.agentbanking.onboarding.domain.port.out;

import com.agentbanking.onboarding.domain.model.KycVerification;
import java.util.Optional;
import java.util.UUID;

public interface KycVerificationRepository {
    Optional<KycVerification> findById(UUID verificationId);
    Optional<KycVerification> findByMykadNumber(String mykadNumber);
    KycVerification save(KycVerification verification);
}
```

```java
package com.agentbanking.onboarding.domain.port.out;

import java.time.LocalDate;

public interface JpnClientPort {
    JpnVerificationResult verifyMyKad(String mykadNumber);
    BiometricMatchResult matchBiometric(String mykadNumber, String biometricData);

    record JpnVerificationResult(
        boolean valid,
        String fullName,
        LocalDate dateOfBirth,
        String address
    ) {}

    record BiometricMatchResult(
        boolean matched,
        java.math.BigDecimal matchScore
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

### Step 5: Create KycVerificationService domain service

```java
package com.agentbanking.onboarding.domain.service;

import com.agentbanking.onboarding.domain.model.KycVerification;
import com.agentbanking.onboarding.domain.model.KycStatus;
import com.agentbanking.onboarding.domain.port.out.*;
import java.math.BigDecimal;
import java.util.UUID;

public class KycVerificationService {

    private final KycVerificationRepository repository;
    private final JpnClientPort jpnClient;
    private final AmlScreeningPort amlClient;

    public KycVerificationService(KycVerificationRepository repository,
                                   JpnClientPort jpnClient,
                                   AmlScreeningPort amlClient) {
        this.repository = repository;
        this.jpnClient = jpnClient;
        this.amlClient = amlClient;
    }

    public KycVerification verifyMyKad(String mykadNumber) {
        // Check existing verification
        var existing = repository.findByMykadNumber(mykadNumber);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Call JPN for MyKad verification
        JpnClientPort.JpnVerificationResult result = jpnClient.verifyMyKad(mykadNumber);

        if (!result.valid()) {
            throw new IllegalArgumentException("ERR_MYKAD_VERIFICATION_FAILED");
        }

        // Create KYC verification
        KycVerification verification = new KycVerification(
            UUID.randomUUID(), mykadNumber, result.fullName(),
            result.dateOfBirth(), result.address(), KycStatus.PENDING, null, null, null, null
        );
        verification.markMyKadVerified(result.fullName(), result.dateOfBirth(), result.address());

        // AML screening
        var amlStatus = amlClient.screen(mykadNumber, result.fullName());
        verification.setAmlStatus(amlStatus);

        return repository.save(verification);
    }

    public KycVerification matchBiometric(String verificationId, String biometricData) {
        KycVerification verification = repository.findById(UUID.fromString(verificationId))
            .orElseThrow(() -> new IllegalArgumentException("ERR_VERIFICATION_NOT_FOUND"));

        // Call JPN for biometric matching
        JpnClientPort.BiometricMatchResult result = jpnClient.matchBiometric(
            verification.getMykadNumber(), biometricData);

        verification.markBiometricMatched(result.matchScore());

        // Auto-approval check
        if (verification.isEligibleForAutoApproval()) {
            verification.markAutoApproved();
        }

        return repository.save(verification);
    }
}
```

### Step 6: Commit

```bash
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/
git commit -m "feat(onboarding): add domain entities, ports and KYC verification service"
```

---

## Task 2: Infrastructure Layer - JPA Adapters [DONE]

**BDD Scenarios:** BDD-O01, BDD-O02, BDD-O03  
**BRD Requirements:** US-O01, US-O02, US-O03  
**User-Facing:** NO  

**Files:**
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/persistence/entity/KycVerificationEntity.java`
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/persistence/repository/KycVerificationJpaRepository.java`
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/persistence/adapter/KycVerificationRepositoryAdapter.java`
- Create: Flyway migration: `services/onboarding-service/src/main/resources/db/migration/V4__create_kyc_verification.sql`

### Step 1: Create Flyway migration

```sql
CREATE TABLE kyc_verification (
    verification_id UUID PRIMARY KEY,
    mykad_number VARCHAR(255) NOT NULL, -- encrypted
    full_name VARCHAR(200),
    date_of_birth DATE,
    address TEXT,
    kyc_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    aml_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    biometric_match_score DECIMAL(5,2),
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_kyc_mykad UNIQUE (mykad_number)
);
```

### Step 2: Create JPA entity and repository adapter

### Step 3: Run tests and commit

```bash
./gradlew :onboarding-service:test
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/persistence/
git commit -m "feat(onboarding): add JPA entities and repository adapters"
```

---

## Task 3: Application Layer - Use Cases [DONE]

**BDD Scenarios:** BDD-O01, BDD-O02, BDD-O03, BDD-O04, BDD-O05  
**BRD Requirements:** US-O01, US-O02, US-O03, US-O04, US-O05  
**User-Facing:** YES  

**Files:**
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/port/in/VerifyMyKadUseCase.java`
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/port/in/BiometricMatchUseCase.java`
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/application/usecase/VerifyMyKadUseCaseImpl.java`
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/application/usecase/BiometricMatchUseCaseImpl.java`

### Step 1: Create inbound port interfaces

### Step 2: Create use case implementations

### Step 3: Run tests and commit

```bash
./gradlew :onboarding-service:test
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/port/in/
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/application/
git commit -m "feat(onboarding): add use cases for MyKad verification and biometric matching"
```

---

## Task 4: Infrastructure Layer - REST Controllers [DONE]

**BDD Scenarios:** BDD-O01, BDD-O02, BDD-O03  
**BRD Requirements:** US-O01, US-O02, US-O03  
**User-Facing:** YES  

**Files:**
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/web/KycController.java`
- Create: DTOs for requests/responses
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/web/GlobalExceptionHandler.java`

### Step 1: Create REST controller

### Step 2: Create GlobalExceptionHandler

### Step 3: Run tests and commit

```bash
./gradlew :onboarding-service:test
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/web/
git commit -m "feat(onboarding): add REST controllers for KYC endpoints"
```

---

## Task 5: Infrastructure - JPN & AML Feign Clients [DONE]

**BDD Scenarios:** BDD-O01, BDD-O02, BDD-O03, BDD-EFM01  
**BRD Requirements:** US-O01, US-O02, US-O03, US-EFM01  
**User-Facing:** NO  

**Files:**
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/external/JpnFeignClient.java`
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/external/AmlFeignClient.java`
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/external/JpnClientAdapter.java`
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/external/AmlScreeningAdapter.java`

### Step 1: Create Feign clients with circuit breaker

### Step 2: Create adapters implementing domain ports

### Step 3: Run tests and commit

```bash
./gradlew :onboarding-service:test
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/external/
git commit -m "feat(onboarding): add Feign clients for JPN and AML screening"
```

---

## Task 6: Unit Tests [DONE]

**BDD Scenarios:** All BDD-O* scenarios  
**BRD Requirements:** US-O01 through US-O05  
**User-Facing:** NO  

**Files:**
- Create: `services/onboarding-service/src/test/java/com/agentbanking/onboarding/domain/service/KycVerificationServiceTest.java`
- Create: `services/onboarding-service/src/test/java/com/agentbanking/onboarding/application/usecase/VerifyMyKadUseCaseImplTest.java`
- Create: `services/onboarding-service/src/test/java/com/agentbanking/onboarding/architecture/HexagonalArchitectureTest.java`

### Step 1: Write unit tests for KYC verification service

### Step 2: Write unit tests for use cases

### Step 3: Write ArchUnit test

### Step 4: Run all tests and commit

```bash
./gradlew :onboarding-service:test
git add services/onboarding-service/src/test/
git commit -m "test(onboarding): add unit tests and ArchUnit validation for Onboarding Service"
```

---