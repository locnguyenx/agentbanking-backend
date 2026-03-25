# Phase 3: Onboarding Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan.

**Goal:** Build the Onboarding Service — e-KYC, MyKad verification, biometric matching, customer onboarding.

**Architecture:** Hexagonal. Domain layer zero Spring imports.

**Tech Stack:** Java 21, Spring Boot 3.x, PostgreSQL, Flyway

---

## File Structure

```
services/onboarding-service/
├── build.gradle
├── src/main/java/com/agentbanking/onboarding/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── KycVerification.java      # Entity
│   │   │   ├── KycStatus.java             # Enum
│   │   │   └── AmlStatus.java            # Enum
│   │   ├── port/
│   │   │   ├── in/OnboardingUseCase.java
│   │   │   └── out/KycRepository.java, JpnGateway.java
│   │   └── service/KycDecisionService.java
│   ├── application/usecase/
│   │   ├── VerifyMyKadUseCase.java
│   │   └── BiometricMatchUseCase.java
│   └── infrastructure/
│       ├── web/OnboardingController.java
│       └── persistence/KycRepositoryImpl.java
├── src/main/resources/
│   ├── db/migration/V1__create_onboarding_tables.sql
│   └── application.yaml
└── src/test/java/...
```

---

## Tasks

### Task 1: Project Setup

- [ ] Create build.gradle with Spring Boot 3.x, JPA, PostgreSQL, Flyway
- [ ] Create application.yaml with port 8083
- [ ] Commit

### Task 2: KycVerification Entity

**BDD Scenarios:** BDD-O01, BDD-O01-EC-01, BDD-O01-EC-02, BDD-O01-EC-03, BDD-O02, BDD-O02-EC-01 through EC-04, BDD-O03, BDD-O03-EC-01, BDD-O03-EC-02

**Files:**
- `domain/model/KycVerification.java`
- `domain/port/out/KycRepository.java`
- `db/migration/V1__create_onboarding_tables.sql`

- [ ] **Step 1: Write migration**

```sql
CREATE TABLE kyc_verification (
    verification_id UUID PRIMARY KEY,
    mykad_number VARCHAR(12) NOT NULL,
    full_name VARCHAR(200),
    date_of_birth DATE,
    age INTEGER,
    aml_status VARCHAR(20) NOT NULL,
    biometric_match VARCHAR(20),
    verification_status VARCHAR(20) NOT NULL,
    rejection_reason VARCHAR(500),
    verified_at TIMESTAMP,
    reviewed_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_kyc_mykad ON kyc_verification(mykad_number);
CREATE INDEX idx_kyc_status ON kyc_verification(verification_status);
```

- [ ] **Step 2: Write KycVerification entity with all fields**

- [ ] **Step 3: Write repository port**

- [ ] **Step 4: Write tests for entity CRUD**

- [ ] **Step 5: Commit**

### Task 3: JPN Gateway (Outbound Port)

**BRD Requirements:** FR-6.1 (JPN verify)

- [ ] **Step 1: Write JpnGateway interface**

```java
public interface JpnGateway {
    JpnResponse verifyMyKad(String mykadNumber);
    BiometricResponse matchBiometric(String verificationId, String biometricData);
}
```

- [ ] **Step 2: Write MockJpnGateway for testing (uses mock-server)**

- [ ] **Step 3: Write tests**

- [ ] **Step 4: Commit**

### Task 4: KYC Decision Service

**BDD Scenarios:** BDD-O03 (auto-approve decision matrix), BDD-O03-EC-01 (AML override), BDD-O03-EC-02 (age reject)

**FR Requirements:** FR-6.3 (auto-approve), FR-6.4 (manual review)

- [ ] **Step 1: Write failing test**

```java
@Test
void shouldAutoApproveWhenAllConditionsMet() {
    // Given: match=YES, amlStatus=CLEAN, age=35
    // When: decision is made
    // Then: status = AUTO_APPROVED
}

@Test
void shouldManualReviewWhenAmlFlagged() {
    // Given: match=YES, amlStatus=FLAGGED, age=35
    // When: decision is made
    // Then: status = MANUAL_REVIEW
}

@Test
void shouldRejectWhenUnder18() {
    // Given: match=YES, amlStatus=CLEAN, age=13
    // When: decision is made
    // Then: status = REJECTED, reason = "age below minimum"
}
```

- [ ] **Step 2: Write KycDecisionService**

```java
public class KycDecisionService {
    public KycStatus decide(boolean biometricMatch, AmlStatus amlStatus, int age) {
        // Rule 1: age < 18 → REJECT
        if (age < 18) return KycStatus.REJECTED;
        
        // Rule 2: amlStatus = BLOCKED → REJECT
        if (amlStatus == AmlStatus.BLOCKED) return KycStatus.REJECTED;
        
        // Rule 3: amlStatus = FLAGGED → MANUAL_REVIEW
        if (amlStatus == AmlStatus.FLAGGED) return KycStatus.MANUAL_REVIEW;
        
        // Rule 4: biometricMatch = NO_MATCH → MANUAL_REVIEW
        if (biometricMatch == false) return KycStatus.MANUAL_REVIEW;
        
        // All conditions pass → AUTO_APPROVED
        return KycStatus.AUTO_APPROVED;
    }
}
```

- [ ] **Step 3: Verify tests pass**

- [ ] **Step 4: Commit**

### Task 5: REST Controller

**External API:** POST /api/v1/kyc/verify, POST /api/v1/kyc/biometric

- [ ] **Step 1: Write request/response DTOs**

- [ ] **Step 2: Write controller**

- [ ] **Step 3: Write integration tests**

- [ ] **Step 4: Commit**

---

## Summary

| Task | BDD Coverage |
|------|--------------|
| 1 | Foundation |
| 2 | Entity + Repo (BDD-O01, O01-EC-01 to EC-03, O02, O02-EC-01 to EC-04) |
| 3 | JPN Gateway (FR-6.1, FR-6.2) |
| 4 | Decision matrix (BDD-O03, O03-EC-01, O03-EC-02) |
| 5 | Controller |