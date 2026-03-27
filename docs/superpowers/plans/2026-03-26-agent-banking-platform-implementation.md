# Agent Banking Platform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix hexagonal architecture violations, complete core service integrations (Feign clients, Kafka), and build missing features (Agent CRUD, Balance Inquiry, Geofence/Velocity integration, Audit Logging, Settlement) per revised BRD v1.1 / BDD v1.1 / Design v1.1.

**Architecture:** Hexagonal (Ports & Adapters) per microservice. Domain layer = pure Java (ZERO framework imports). Infrastructure layer implements ports. Orchestrator coordinates saga flows.

**Tech Stack:** Java 21, Spring Boot 3.2.5, Spring Cloud 2023.0.0, PostgreSQL, Redis, Kafka, OpenFeign, Resilience4j, JUnit 5, Mockito, ArchUnit, Flyway.

---

## Phase 0: Hexagonal Architecture Fix (Foundation)

**BDD Scenarios:** All — architecture compliance enables all scenarios
**BRD Requirements:** C-8 (Hexagonal architecture per service), AGENTS.md ArchUnit enforcement

### Task 0.1: Fix Rules Service Hexagonal Architecture [DONE]

- [x] Step 1: Create inbound port interfaces (use case interfaces)
- [x] Step 2: Create application-layer use case implementations
- [x] Step 3: Remove framework annotations from domain services
- [x] Step 4: Update controller to inject use cases
- [x] Step 5: Run ArchUnit test to verify domain layer is clean
- [x] Step 6: Run all Rules Service tests
- [x] Step 7: Commit

```java
// domain/port/in/FeeQueryUseCase.java
package com.agentbanking.rules.domain.port.in;

import com.agentbanking.rules.domain.model.AgentTier;
import com.agentbanking.rules.domain.model.FeeConfigRecord;
import com.agentbanking.rules.domain.model.TransactionType;

public interface FeeQueryUseCase {
    FeeConfigRecord getFeeConfig(TransactionType transactionType, AgentTier agentTier);
}
```

```java
// domain/port/in/VelocityCheckUseCase.java
package com.agentbanking.rules.domain.port.in;

import java.math.BigDecimal;

public interface VelocityCheckUseCase {
    VelocityResult checkVelocity(String customerMykad, TransactionType txnType, BigDecimal amount);
    
    record VelocityResult(boolean passed, String errorCode) {}
}
```

```java
// domain/port/in/LimitEnforcementUseCase.java
package com.agentbanking.rules.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface LimitEnforcementUseCase {
    LimitResult checkDailyLimit(UUID agentId, TransactionType txnType, BigDecimal amount);
    
    record LimitResult(boolean passed, String errorCode) {}
}
```

- [ ] **Step 2: Create application-layer use case implementations**

```java
// application/usecase/FeeQueryUseCaseImpl.java
package com.agentbanking.rules.application.usecase;

import com.agentbanking.rules.domain.model.AgentTier;
import com.agentbanking.rules.domain.model.FeeConfigRecord;
import com.agentbanking.rules.domain.model.TransactionType;
import com.agentbanking.rules.domain.port.in.FeeQueryUseCase;
import com.agentbanking.rules.domain.port.out.FeeConfigRepository;
import com.agentbanking.rules.domain.service.FeeCalculationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FeeQueryUseCaseImpl implements FeeQueryUseCase {
    private final FeeConfigRepository feeConfigRepository;
    private final FeeCalculationService feeCalculationService;

    public FeeQueryUseCaseImpl(FeeConfigRepository feeConfigRepository, FeeCalculationService feeCalculationService) {
        this.feeConfigRepository = feeConfigRepository;
        this.feeCalculationService = feeCalculationService;
    }

    @Override
    public FeeConfigRecord getFeeConfig(TransactionType transactionType, AgentTier agentTier) {
        return feeConfigRepository.findByTransactionTypeAndAgentTier(transactionType, agentTier)
            .orElseThrow(() -> new com.agentbanking.common.exception.LedgerException(
                "ERR_FEE_CONFIG_NOT_FOUND",
                "No fee configuration found for " + transactionType + " / " + agentTier));
    }
}
```

- [ ] **Step 3: Remove framework annotations from domain services**

Edit `FeeCalculationService.java`:
- Remove `import org.springframework.stereotype.Service;`
- Remove `import org.springframework.transaction.annotation.Transactional;`
- Remove `@Service` annotation
- Remove `@Transactional` annotation
- Remove `private static final Logger logger = LoggerFactory.getLogger(...)` (SLF4J import)

Repeat for `VelocityCheckService.java` and `LimitEnforcementService.java`.

- [ ] **Step 4: Update controller to inject use cases**

```java
// infrastructure/web/RulesController.java
@RestController
@RequestMapping("/internal")
public class RulesController {
    private final FeeQueryUseCase feeQueryUseCase;
    private final VelocityCheckUseCase velocityCheckUseCase;
    private final LimitEnforcementUseCase limitEnforcementUseCase;

    // Constructor injection of use cases (not domain services)
    // Endpoints delegate to use cases
}
```

- [ ] **Step 5: Run ArchUnit test to verify domain layer is clean**

Run: `./gradlew :rules-service:test --tests "*HexagonalArchitectureTest*"`
Expected: PASS (domain layer has no Spring/JPA annotations)

- [ ] **Step 6: Run all Rules Service tests**

Run: `./gradlew :rules-service:test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add services/rules-service/src/main/java/.../domain/port/in/
git add services/rules-service/src/main/java/.../application/usecase/
git add services/rules-service/src/main/java/.../domain/service/
git add services/rules-service/src/main/java/.../infrastructure/web/RulesController.java
git commit -m "refactor(rules): fix hexagonal architecture — add inbound ports, move annotations to application layer"
```

---

### Task 0.2: Fix Ledger Service Hexagonal Architecture [DONE]

- [x] Step 1: Create inbound port interfaces
- [x] Step 2: Create application-layer use case implementations
- [x] Step 3: Remove framework annotations from LedgerService.java
- [x] Step 4: Fix LedgerController — remove direct JPA injection
- [x] Step 5: Run tests and verify
- [x] Step 6: Commit

```java
// domain/port/in/ProcessWithdrawalUseCase.java
public interface ProcessWithdrawalUseCase {
    TransactionRecord processWithdrawal(WithdrawalCommand command);
    
    record WithdrawalCommand(UUID agentId, BigDecimal amount, String currency, 
                              String idempotencyKey, String customerCardMasked,
                              BigDecimal geofenceLat, BigDecimal geofenceLng) {}
}
```

```java
// domain/port/in/GetBalanceUseCase.java
public interface GetBalanceUseCase {
    AgentBalanceResponse getAgentBalance(UUID agentId);
    
    record AgentBalanceResponse(BigDecimal balance, BigDecimal reservedBalance, 
                                 BigDecimal availableBalance, String currency) {}
}
```

- [ ] **Step 2: Create application-layer use case implementations**

Implementations that:
1. Check idempotency via IdempotencyCache
2. Delegate to domain service for business logic
3. Return domain records

- [ ] **Step 3: Remove framework annotations from LedgerService.java**

- [ ] **Step 4: Fix LedgerController — remove direct JPA injection**

Currently `LedgerController` injects `TransactionJpaRepository` directly. Change to inject `GetBalanceUseCase` and use the outbound port interface.

- [ ] **Step 5: Run tests and verify**

Run: `./gradlew :ledger-service:test`
Expected: PASS

- [ ] **Step 6: Commit**

---

### Task 0.3: Fix Onboarding, Switch Adapter, Biller Service Hexagonal Architecture [DONE]

- [x] Step 1: Onboarding Service — create inbound ports
- [x] Step 2: Switch Adapter — create inbound ports
- [x] Step 3: Biller Service — create inbound ports
- [x] Step 4: Create application-layer use case implementations for all 3 services
- [x] Step 5: Remove framework annotations from domain services
- [x] Step 6: Update controllers
- [x] Step 7: Run all tests
- [x] Step 8: Commit

```java
// domain/port/in/VerifyMyKadUseCase.java
public interface VerifyMyKadUseCase {
    KycVerificationRecord verifyMyKad(String mykadNumber);
}

// domain/port/in/BiometricMatchUseCase.java
public interface BiometricMatchUseCase {
    KycVerificationRecord matchBiometric(String verificationId, String biometricData);
}
```

- [ ] **Step 2: Switch Adapter — create inbound ports**

```java
// domain/port/in/AuthorizeTransactionUseCase.java
public interface AuthorizeTransactionUseCase {
    SwitchTransactionRecord authorize(CardAuthCommand command);
    
    record CardAuthCommand(String pan, String pinBlock, BigDecimal amount, 
                           String terminalId, String idempotencyKey) {}
}

// domain/port/in/ProcessReversalUseCase.java
public interface ProcessReversalUseCase {
    SwitchTransactionRecord reverse(String originalTransactionId);
}
```

- [ ] **Step 3: Biller Service — create inbound ports**

```java
// domain/port/in/ValidateBillUseCase.java
public interface ValidateBillUseCase {
    BillValidationResult validate(String billerCode, String ref1);
    
    record BillValidationResult(boolean valid, String accountName, BigDecimal amountDue) {}
}

// domain/port/in/PayBillUseCase.java
public interface PayBillUseCase {
    BillPaymentRecord payBill(PayBillCommand command);
    
    record PayBillCommand(String billerCode, String ref1, BigDecimal amount, 
                           String paymentMethod, UUID agentId) {}
}
```

- [ ] **Step 4: Create application-layer use case implementations for all 3 services**

- [ ] **Step 5: Remove framework annotations from domain services**

- [ ] **Step 6: Update controllers**

- [ ] **Step 7: Run all tests**

Run: `./gradlew test`
Expected: PASS

- [ ] **Step 8: Commit**

---

### Task 0.4: Rewrite ArchUnit Tests to Use Proper API [DONE]

- [x] Step 1: Add ArchUnit dependency to all service build.gradle files
- [x] Step 2: Rewrite HexagonalArchitectureTest using proper ArchUnit API
- [x] Step 3: Copy to all 5 services with appropriate package names
- [x] Step 4: Run all ArchUnit tests
- [x] Step 5: Commit

---

## Phase 1: Infrastructure Configuration (Redis, Kafka, Feign)

### Task 1.1: Create Spring Configuration Classes for All Services [DONE]

- [x] Step 1: Create Redis configuration for Ledger Service
- [x] Step 2: Create Kafka configuration for Ledger Service
- [x] Step 3: Create Feign client configuration with Resilience4j
- [x] Step 4: Verify configuration loads
- [x] Step 5: Commit

---

### Task 1.2: Create Kafka Producer/Consumer Infrastructure [DONE]

- [x] Step 1: Create transaction event record
- [x] Step 2: Create Kafka producer
- [x] Step 3: Wire publisher into use case implementation
- [x] Step 4: Run tests
- [x] Step 5: Commit**

---

## Phase 2: Missing Core Features

### Task 2.1: Implement Agent Management (CRUD) [DONE]

- [x] Step 1: Write failing test for agent creation (BDD-BO01)
- [x] Step 2: Run test to verify it fails
- [x] Step 3: Create domain model, ports, service
- [x] Step 4: Create Flyway migration
- [x] Step 5: Create entity, JPA repo, mapper, repository impl
- [x] Step 6: Update controller with agent CRUD endpoints
- [x] Step 7: Run test to verify it passes
- [x] Step 8: Write edge case test (duplicate MyKad - BDD-BO01-EC-01)
- [x] Step 9: Implement duplicate check
- [x] Step 10: Write edge case test (deactivate with pending - BDD-BO01-EC-02)
- [x] Step 11: Implement pending transaction check
- [x] Step 12: Run all tests
- [x] Step 13: Commit

```java
// AgentServiceTest.java
@Test
void shouldCreateAgentWithActiveStatus() {
    // Given: CreateAgentCommand with valid data
    // When: agentService.createAgent(command)
    // Then: Agent created with status ACTIVE, AgentFloat initialized at 0.00
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :onboarding-service:test --tests "*AgentServiceTest*"`
Expected: FAIL

- [ ] **Step 3: Create domain model, ports, service**

```java
// domain/model/AgentRecord.java
public record AgentRecord(
    UUID agentId,
    String agentCode,
    String businessName,
    AgentTier tier,
    AgentStatus status,
    BigDecimal merchantGpsLat,
    BigDecimal merchantGpsLng,
    String mykadNumber,  // encrypted
    String phoneNumber,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

```java
// domain/port/out/AgentRepository.java
public interface AgentRepository {
    AgentRecord save(AgentRecord agent);
    Optional<AgentRecord> findById(UUID agentId);
    Optional<AgentRecord> findByMykadNumber(String mykadNumber);
    List<AgentRecord> findAll(Pageable pageable);
}
```

- [ ] **Step 4: Create Flyway migration**

```sql
CREATE TABLE agent (
    agent_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_code VARCHAR(20) UNIQUE NOT NULL,
    business_name VARCHAR(200) NOT NULL,
    tier VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    merchant_gps_lat DECIMAL(9,6) NOT NULL,
    merchant_gps_lng DECIMAL(9,6) NOT NULL,
    mykad_number VARCHAR(255) NOT NULL, -- encrypted
    phone_number VARCHAR(15) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agent_mykad ON agent(mykad_number);
```

- [ ] **Step 5: Create entity, JPA repo, mapper, repository impl**

- [ ] **Step 6: Update controller with agent CRUD endpoints**

```java
@PostMapping("/backoffice/agents")
public ResponseEntity<AgentResponse> createAgent(@Valid @RequestBody CreateAgentRequest request) {
    // Delegate to CreateAgentUseCase
}

@PutMapping("/backoffice/agents/{id}")
public ResponseEntity<AgentResponse> updateAgent(@PathVariable UUID id, ...) {
    // Delegate to UpdateAgentUseCase
}

@DeleteMapping("/backoffice/agents/{id}")
public ResponseEntity<Void> deactivateAgent(@PathVariable UUID id) {
    // Delegate to DeactivateAgentUseCase
}

@GetMapping("/backoffice/agents")
public ResponseEntity<List<AgentResponse>> listAgents(...) {
    // Delegate to ListAgentsUseCase
}
```

- [ ] **Step 7: Run test to verify it passes**

- [ ] **Step 8: Write edge case test (duplicate MyKad - BDD-BO01-EC-01)**

```java
@Test
void shouldRejectDuplicateAgentCreation() {
    // Given: Agent exists with mykadNumber "880101011234"
    // When: createAgent with same MyKad
    // Then: ERR_DUPLICATE_AGENT
}
```

- [ ] **Step 9: Implement duplicate check**

- [ ] **Step 10: Write edge case test (deactivate with pending - BDD-BO01-EC-02)**

- [ ] **Step 11: Implement pending transaction check**

- [ ] **Step 12: Run all tests**

- [ ] **Step 13: Commit**

---

### Task 2.2: Implement Customer Balance Inquiry [DONE]

- [x] Step 1: Write failing test for balance inquiry (BDD-L04)
- [x] Step 2: Create inbound port
- [x] Step 3: Implement — call Switch Adapter to get customer balance from card network
- [x] Step 4: Run tests
- [x] Step 5: Commit

- [ ] **Step 2: Create inbound port**

```java
public interface CustomerBalanceInquiryUseCase {
    CustomerBalanceResponse inquire(CustomerInquiryCommand command);
    
    record CustomerInquiryCommand(String encryptedCardData, String pinBlock) {}
    record CustomerBalanceResponse(BigDecimal balance, String currency, String accountMasked) {}
}
```

- [ ] **Step 3: Implement — call Switch Adapter to get customer balance from card network**

- [ ] **Step 4: Run tests**

- [ ] **Step 5: Commit**

---

### Task 2.3: Integrate Geofence Validation into Transaction Flow [PENDING]

- [ ] Step 1: Write failing test for geofence validation (BDD-W01-EC-05)
- [ ] Step 2: Wire GeofenceChecker from common module into LedgerService
- [ ] Step 3: Run test
- [ ] Step 4: Write GPS unavailable test (BDD-W01-EC-06)
- [ ] Step 5: Commit

---

### Task 2.4: Integrate Velocity Checks into Transaction Flow [PENDING]

- [ ] Step 1: Create Feign client interface
- [ ] Step 2: Wire into use case — call Rules Service before Ledger debit
- [ ] Step 3: Write test for velocity exceeded (BDD-R03-EC-01)
- [ ] Step 4: Implement
- [ ] Step 5: Commit

```java
@Test
void shouldRejectWithdrawalOutsideGeofence() {
    // Given: Agent registered at (3.1390, 101.6869), POS at (3.2000, 101.7000) — 7km away
    // When: processWithdrawal
    // Then: ERR_GEOFENCE_VIOLATION
}
```

- [ ] **Step 2: Wire GeofenceChecker from common module into LedgerService**

- [ ] **Step 3: Run test**

- [ ] **Step 4: Write GPS unavailable test (BDD-W01-EC-06)**

- [ ] **Step 5: Commit**

---

### Task 2.4: Integrate Velocity Checks into Transaction Flow

**BDD Scenarios:** BDD-R03, BDD-R03-EC-01 through EC-04, BDD-EFM01, BDD-EFM04
**BRD Requirements:** US-R03, FR-1.3, FR-14.5

**User-Facing:** NO

**Files:**
- Create: `services/ledger-service/src/main/java/.../infrastructure/external/RulesServiceFeignClient.java`
- Modify: `services/ledger-service/src/main/java/.../application/usecase/ProcessWithdrawalUseCaseImpl.java` — call Rules Service before processing

- [ ] **Step 1: Create Feign client interface**

```java
@FeignClient(name = "rules-service", url = "${rules-service.url}")
public interface RulesServiceFeignClient {
    @PostMapping("/internal/check-velocity")
    VelocityCheckResult checkVelocity(@RequestBody VelocityCheckRequest request);
}
```

- [ ] **Step 2: Wire into use case — call Rules Service before Ledger debit**

- [ ] **Step 3: Write test for velocity exceeded (BDD-R03-EC-01)**

- [ ] **Step 4: Implement**

- [ ] **Step 5: Commit**

---

## Phase 3: Transaction Orchestrator

### Task 3.1: Implement Transaction Orchestrator Saga [PENDING]

- [ ] Step 1: Create orchestrator service module
- [ ] Step 2: Define saga flow: BlockFloat → SwitchAuth → Commit
- [ ] Step 3: Implement Resilience4j circuit breaker + timeout (25s for switch)
- [ ] Step 4: Implement fallback → trigger reversal on timeout
- [ ] Step 5: Run tests
- [ ] Step 6: Commit

---

## Phase 4: Audit Logging

### Task 4.1: Implement Audit Log Entity and Service [PENDING]

- [ ] Step 1: Create audit log domain model
- [ ] Step 2: Create audit log service (append-only, immutable)
- [ ] Step 3: Wire into ledger service (log all financial transactions)
- [ ] Step 4: Wire into onboarding service (log agent CRUD)
- [ ] Step 5: Create backoffice endpoint for audit log viewing
- [ ] Step 6: Commit

---

## Phase 5: EOD Settlement

### Task 5.1: Implement EOD Net Settlement Batch Job [PENDING]

- [ ] Step 1: Create SettlementSummary domain model and entity
- [ ] Step 2: Write failing test for settlement calculation (BDD-SM01)
- [ ] Step 3: Implement settlement calculation service
- [ ] Step 4: Implement @Scheduled job at 23:59:59 MYT
- [ ] Step 5: Implement CBS file generation (CSV)
- [ ] Step 6: Run tests
- [ ] Step 7: Commit

---

## Phase 6: Integration Tests

### Task 6.1: Add Testcontainers Integration Tests [PENDING]

- [ ] Step 1: Add Testcontainers dependency
- [ ] Step 2: Create integration test for withdrawal flow
- [ ] Step 3: Run integration tests
- [ ] Step 4: Commit**

---

## Summary

| Phase | Tasks | Key Deliverables |
|-------|-------|-----------------|
| **Phase 0** | 4 tasks | Hexagonal architecture fixed, proper ArchUnit tests |
| **Phase 1** | 2 tasks | Redis, Kafka, Feign configuration and infrastructure |
| **Phase 2** | 4 tasks | Agent CRUD, Balance Inquiry, Geofence, Velocity |
| **Phase 3** | 1 task | Transaction Orchestrator saga |
| **Phase 4** | 1 task | Audit logging |
| **Phase 5** | 1 task | EOD Settlement batch job |
| **Phase 6** | 1 task | Integration tests with Testcontainers |
| **Total** | **14 tasks** | |

**Estimated effort:** 8-12 days for 1 developer
