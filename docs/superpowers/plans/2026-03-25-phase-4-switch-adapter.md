# Phase 4: Switch Adapter Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan.

**Goal:** Build the Switch Adapter Service — ISO 8583 card transactions, ISO 20022 DuitNow, HSM PIN verification.

**Architecture:** Hexagonal. Tier 4 Translation Layer — converts internal JSON to ISO formats.

**Tech Stack:** Java 21, Spring Boot 3.x, PostgreSQL, TCP/IP sockets for ISO 8583

---

## File Structure

```
services/switch-adapter-service/
├── build.gradle
├── src/main/java/com/agentbanking/switchadapter/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── SwitchTransaction.java
│   │   │   ├── Iso8583Message.java
│   │   │   └── Iso20022Message.java
│   │   ├── port/
│   │   │   ├── in/CardAuthorizationUseCase.java
│   │   │   ├── out/PayNetGateway.java
│   │   │   └── out/HsmGateway.java
│   │   └── service/
│   │       ├── Iso8583Translator.java
│   │       └── Iso20022Translator.java
│   ├── application/usecase/
│   │   ├── AuthorizeCardUseCase.java
│   │   ├── ProcessReversalUseCase.java
│   │   └── DuitNowTransferUseCase.java
│   └── infrastructure/
│       ├── web/SwitchController.java
│       ├── tcp/Iso8583Client.java
│       └── persistence/SwitchTransactionRepository.java
├── src/main/resources/
│   ├── db/migration/V1__create_switch_tables.sql
│   └── application.yaml
└── src/test/java/...
```

---

## Tasks

### Task 1: Project Setup [DONE]

### Task 2: HSM Gateway (PIN Verification) [DONE]

### Task 3: PayNet Gateway (Card Authorization) [DONE]

### Task 4: Card Authorization Use Case [DONE]

### Task 5: Reversal Logic (MTI 0400) [DONE]

### Task 6: REST Controller [DONE]

---

## Phase 4: Switch Adapter Service - COMPLETE ✅

- [ ] Create build.gradle with Spring Boot 3.x, JPA, PostgreSQL
- [ ] Create application.yaml with port 8084
- [ ] Commit

### Task 2: HSM Gateway (PIN Verification)

**BDD Scenarios:** BDD-W01-EC-01 (invalid PIN), FR-3.1

- [ ] **Step 1: Write HsmGateway interface**

```java
public interface HsmGateway {
    PinVerificationResult verifyPin(String pinBlock, String pan);
    KeyBundle generateKey(String keyId);
}
```

- [ ] **Step 2: Write MockHsmGateway (connects to mock-server) for testing**

- [ ] **Step 3: Write tests**

- [ ] **Step 4: Commit**

### Task 3: PayNet Gateway (Card Authorization)

**BDD Scenarios:** BDD-W01 (successful auth), BDD-W01-EC-01 (decline), BDD-W01-EC-02 (reversal)

**BRD Requirements:** FR-3.1 (card auth), FR-3.4 (reversal), FR-9.1 (DuitNow)

- [ ] **Step 1: Write PayNetGateway interface**

```java
public interface PayNetGateway {
    AuthorizationResult authorize(AuthorizationRequest request);
    ReversalResult reverse(ReversalRequest request);
    DuitNowResult transfer(DuitNowRequest request);
}
```

- [ ] **Step 2: Write Iso8583Translator (internal JSON → ISO 8583 bitmap)**

```java
public class Iso8583Translator {
    public byte[] toIso8583(AuthorizationRequest req) {
        // Build ISO 8583 message:
        // MTI: 0100 (authorization request)
        // Bitmap: bit 2 (PAN), bit 3 (proc code), bit 4 (amount), bit 7 (datetime), etc.
        // Return ISO 8583 binary
    }
}
```

- [ ] **Step 3: Write Iso20022Translator (internal JSON → ISO 20022 XML)**

- [ ] **Step 4: Write tests for translation**

- [ ] **Step 5: Commit**

### Task 4: Card Authorization Use Case

**BDD Scenarios:** BDD-W01 (happy path), BDD-W01-EC-01 (decline), BDD-W01-EC-04 (limit), BDD-W01-EC-05 (geofence), BDD-W01-EC-06 (GPS unavailable)

- [ ] **Step 1: Write AuthorizeCardUseCase**

```java
@Service
public class AuthorizeCardUseCase {
    public AuthorizationResponse execute(AuthorizationCommand cmd) {
        // 1. Validate request
        // 2. Verify PIN via HSM
        // 3. Translate to ISO 8583
        // 4. Send to PayNet
        // 5. Translate response from ISO 8583
        // 6. Return result
    }
}
```

- [ ] **Step 2: Write tests covering all BDD scenarios**

- [ ] **Step 3: Commit**

### Task 5: Reversal Logic (MTI 0400)

**BDD Scenarios:** BDD-W01-EC-02 (printer failure), BDD-W01-EC-03 (network drop), BDD-W01-EC-08 (manual investigation)

**FR Requirements:** FR-3.4 (reversal)

- [ ] **Step 1: Write reversal table migration**

```sql
CREATE TABLE reversal_queue (
    queue_id UUID PRIMARY KEY,
    original_transaction_id UUID NOT NULL,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_retry_at TIMESTAMP
);
```

- [ ] **Step 2: Write ProcessReversalUseCase**

- [ ] **Step 3: Write Store & Forward logic**

- [ ] **Step 4: Commit**

### Task 6: REST Controller

**Internal API:** POST /internal/auth, POST /internal/reversal, POST /internal/duitnow

- [ ] **Step 1: Write controller**

- [ ] **Step 2: Write tests**

- [ ] **Step 3: Commit**

---

## Summary

| Task | BDD/FR Coverage |
|------|-----------------|
| 1 | Foundation |
| 2 | HSM PIN (FR-3.1, BDD-W01-EC-01) |
| 3 | ISO translation, PayNet gateway (FR-3.1, FR-9.1) |
| 4 | Card authorization (BDD-W01, W01-EC-01, EC-04, EC-05, EC-06) |
| 5 | Reversal MTI 0400 (FR-3.4, BDD-W01-EC-02, EC-03, EC-08) |
| 6 | Controller |