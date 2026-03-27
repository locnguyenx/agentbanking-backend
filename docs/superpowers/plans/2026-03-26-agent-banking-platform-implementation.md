# Agent Banking Platform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix hexagonal architecture violations, complete core service integrations (Feign clients, Kafka), and build missing features (Agent CRUD, Balance Inquiry, Geofence/Velocity integration, Audit Logging, Settlement) per revised BRD v1.1 / BDD v1.1 / Design v1.1.

**Architecture:** Hexagonal (Ports & Adapters) per microservice. Domain layer = pure Java (ZERO framework imports). Infrastructure layer implements ports. Orchestrator coordinates saga flows.

**Tech Stack:** Java 21, Spring Boot 3.2.5, Spring Cloud 2023.0.0, PostgreSQL, Redis, Kafka, OpenFeign, Resilience4j, JUnit 5, Mockito, ArchUnit, Flyway.

---

## Phase 0: Hexagonal Architecture Fix (Foundation)

**BDD Scenarios:** All — architecture compliance enables all scenarios
**BRD Requirements:** C-8 (Hexagonal architecture per service), AGENTS.md ArchUnit enforcement

### Task 0.1: Fix Rules Service Hexagonal Architecture [DONE] ✓

**BDD Scenarios:** BDD-R01 through BDD-R04 (all Rules scenarios)
**BRD Requirements:** US-R01, US-R02, US-R03, US-R04, C-8

**User-Facing:** NO

**Files:**
- Create: `services/rules-service/src/main/java/.../domain/port/in/FeeQueryUseCase.java`
- Create: `services/rules-service/src/main/java/.../domain/port/in/VelocityCheckUseCase.java`
- Create: `services/rules-service/src/main/java/.../domain/port/in/LimitEnforcementUseCase.java`
- Create: `services/rules-service/src/main/java/.../application/usecase/FeeQueryUseCaseImpl.java`
- Create: `services/rules-service/src/main/java/.../application/usecase/VelocityCheckUseCaseImpl.java`
- Create: `services/rules-service/src/main/java/.../application/usecase/LimitEnforcementUseCaseImpl.java`
- Modify: `services/rules-service/src/main/java/.../domain/service/FeeCalculationService.java` — remove `@Service`, `@Transactional`
- Modify: `services/rules-service/src/main/java/.../domain/service/VelocityCheckService.java` — remove `@Service`, `@Transactional`
- Modify: `services/rules-service/src/main/java/.../domain/service/LimitEnforcementService.java` — remove `@Service`, `@Transactional`
- Modify: `services/rules-service/src/main/java/.../infrastructure/web/RulesController.java` — inject use cases, not domain services
- Create: `services/rules-service/src/main/java/.../config/RulesServiceConfig.java`

- [ ] **Step 1: Create inbound port interfaces (use case interfaces)**

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

### Task 0.2: Fix Ledger Service Hexagonal Architecture [DONE] ✓

**BDD Scenarios:** BDD-L01 through BDD-L04, BDD-W01, BDD-D01
**BRD Requirements:** US-L01 through US-L04, US-L05, US-L07, C-8

**User-Facing:** NO

**Files:**
- Create: `services/ledger-service/src/main/java/.../domain/port/in/ProcessWithdrawalUseCase.java`
- Create: `services/ledger-service/src/main/java/.../domain/port/in/ProcessDepositUseCase.java`
- Create: `services/ledger-service/src/main/java/.../domain/port/in/GetBalanceUseCase.java`
- Create: `services/ledger-service/src/main/java/.../domain/port/in/ReverseTransactionUseCase.java`
- Create: `services/ledger-service/src/main/java/.../application/usecase/`
- Modify: `services/ledger-service/src/main/java/.../domain/service/LedgerService.java` — remove framework annotations
- Modify: `services/ledger-service/src/main/java/.../infrastructure/web/LedgerController.java` — inject use cases, remove direct JPA injection

- [ ] **Step 1: Create inbound port interfaces**

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

### Task 0.3: Fix Onboarding, Switch Adapter, Biller Service Hexagonal Architecture [DONE] ✓

**BDD Scenarios:** BDD-O01 through BDD-O05, BDD-V01, BDD-B01 through BDD-B04, BDD-T01 through BDD-T03
**BRD Requirements:** US-O01 through US-O05, US-V01, US-B01 through US-B05, US-T01 through US-T03, C-8

**User-Facing:** NO

**Files:**
- Create inbound ports for each service
- Create application-layer use case implementations for each service
- Remove framework annotations from domain services in all 3 services
- Update controllers to inject use cases

- [ ] **Step 1: Onboarding Service — create inbound ports**

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

### Task 0.4: Rewrite ArchUnit Tests to Use Proper API [DONE] ✓

**BDD Scenarios:** N/A (architecture validation)
**BRD Requirements:** C-8, AGENTS.md ArchUnit enforcement

**User-Facing:** NO

**Files:**
- Modify: All 5 `HexagonalArchitectureTest.java` files
- Add to each `build.gradle`: `testImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'`

- [ ] **Step 1: Add ArchUnit dependency to all service build.gradle files**

```groovy
// services/rules-service/build.gradle (and all other services)
testImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'
testImplementation 'com.tngtech.archunit:archunit-junit5-api:1.3.0'
```

- [ ] **Step 2: Rewrite HexagonalArchitectureTest using proper ArchUnit API**

```java
package com.agentbanking.rules.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "com.agentbanking.rules")
class HexagonalArchitectureTest {

    @ArchTest
    void domainLayerMustNotContainJpaAnnotations(Classes classes) {
        classes().that().resideInAnyPackage("..domain..")
            .should().notBeAnnotatedWith(jakarta.persistence.Entity.class)
            .should().notBeAnnotatedWith(jakarta.persistence.Table.class)
            .should().notBeAnnotatedWith(jakarta.persistence.Column.class)
            .should().notBeAnnotatedWith(jakarta.persistence.Id.class);
    }

    @ArchTest
    void domainLayerMustNotContainSpringAnnotations(Classes classes) {
        classes().that().resideInAnyPackage("..domain..")
            .should().notBeAnnotatedWith(org.springframework.stereotype.Service.class)
            .should().notBeAnnotatedWith(org.springframework.stereotype.Repository.class)
            .should().notBeAnnotatedWith(org.springframework.stereotype.Component.class)
            .should().notBeAnnotatedWith(org.springframework.transaction.annotation.Transactional.class);
    }

    @ArchTest
    void domainLayerMustNotUseEntityManager(Classes classes) {
        classes().that().resideInAnyPackage("..domain..")
            .should().notDependOnClassesThat()
            .areAssignableFrom(jakarta.persistence.EntityManager.class);
    }
}
```

- [ ] **Step 3: Copy to all 5 services with appropriate package names**

- [ ] **Step 4: Run all ArchUnit tests**

Run: `./gradlew test --tests "*HexagonalArchitectureTest*"`
Expected: ALL PASS

- [ ] **Step 5: Commit**

---

## Phase 1: Infrastructure Configuration (Redis, Kafka, Feign)

**BDD Scenarios:** BDD-L04-EC-02 (idempotency), BDD-W01-SMS (SMS notification), all commission scenarios
**BRD Requirements:** FR-2.4 (idempotency), FR-18.2 (Store & Forward), C-4, C-5, C-9

### Task 1.1: Create Spring Configuration Classes for All Services [DONE] ✓

**BDD Scenarios:** BDD-L04-EC-02 (idempotency caching)
**BRD Requirements:** FR-2.4, C-4, C-5, C-9

**User-Facing:** NO

**Files:**
- Create: `services/ledger-service/src/main/java/.../config/RedisConfig.java`
- Create: `services/ledger-service/src/main/java/.../config/KafkaConfig.java`
- Create: `services/ledger-service/src/main/java/.../config/FeignClientConfig.java`
- Create: `services/rules-service/src/main/java/.../config/RedisConfig.java`
- Create: `services/switch-adapter-service/src/main/java/.../config/KafkaConfig.java`
- Create: `services/biller-service/src/main/java/.../config/FeignClientConfig.java`

- [ ] **Step 1: Create Redis configuration for Ledger Service**

```java
// config/RedisConfig.java
@Configuration
@EnableRedisRepositories
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

- [ ] **Step 2: Create Kafka configuration for Ledger Service**

```java
// config/KafkaConfig.java
@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic transactionCompletedTopic() {
        return TopicBuilder.name("transaction-completed")
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    @Bean
    public NewTopic reversalInitiatedTopic() {
        return TopicBuilder.name("reversal-initiated")
            .partitions(3)
            .replicas(1)
            .build();
    }
}
```

- [ ] **Step 3: Create Feign client configuration with Resilience4j**

```java
// config/FeignClientConfig.java
@Configuration
public class FeignClientConfig {
    @Bean
    public CircuitBreakerFactory circuitBreakerFactory() {
        return new Resilience4JCircuitBreakerFactory();
    }
}
```

- [ ] **Step 4: Verify configuration loads**

Run: `./gradlew :ledger-service:bootRun`
Expected: Service starts without configuration errors

- [ ] **Step 5: Commit**

---

### Task 1.2: Create Kafka Producer/Consumer Infrastructure [DONE] ✓

**BDD Scenarios:** BDD-W01-SMS (SMS notification), BDD-L02 (journal entries), BDD-EFM01 (EFM events)
**BRD Requirements:** FR-2.2, C-5

**User-Facing:** NO

**Files:**
- Create: `services/ledger-service/src/main/java/.../infrastructure/messaging/TransactionEventPublisher.java`
- Create: `services/ledger-service/src/main/java/.../infrastructure/messaging/TransactionEvent.java`
- Create: `services/ledger-service/src/main/java/.../infrastructure/messaging/ReversalEventPublisher.java`
- Create: `services/ledger-service/src/main/java/.../infrastructure/messaging/ReversalEvent.java`

- [ ] **Step 1: Create transaction event record**

```java
public record TransactionEvent(
    String transactionId,
    String transactionType,
    BigDecimal amount,
    String agentId,
    String status,
    String timestamp
) {}
```

- [ ] **Step 2: Create Kafka producer**

```java
@Component
public class TransactionEventPublisher {
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    
    public void publishCompleted(TransactionEvent event) {
        kafkaTemplate.send("transaction-completed", event.transactionId(), event);
    }
}
```

- [ ] **Step 3: Wire publisher into use case implementation**

In `ProcessWithdrawalUseCaseImpl`, after successful commit, call `transactionEventPublisher.publishCompleted(...)`.

- [ ] **Step 4: Run tests**

- [ ] **Step 5: Commit**

---

## Phase 2: Missing Core Features

### Task 2.1: Implement Agent Management (CRUD) [DONE] ✓

**BDD Scenarios:** BDD-BO01, BDD-BO01-EC-01, BDD-BO01-EC-02, BDD-BO01-EC-03
**BRD Requirements:** US-BO01, FR-13.1

**User-Facing:** NO

**Files:**
- Create: `services/onboarding-service/src/main/java/.../domain/model/AgentRecord.java`
- Create: `services/onboarding-service/src/main/java/.../domain/model/AgentStatus.java`
- Create: `services/onboarding-service/src/main/java/.../domain/model/AgentTier.java`
- Create: `services/onboarding-service/src/main/java/.../domain/port/in/CreateAgentUseCase.java`
- Create: `services/onboarding-service/src/main/java/.../domain/port/in/UpdateAgentUseCase.java`
- Create: `services/onboarding-service/src/main/java/.../domain/port/in/DeactivateAgentUseCase.java`
- Create: `services/onboarding-service/src/main/java/.../domain/port/in/ListAgentsUseCase.java`
- Create: `services/onboarding-service/src/main/java/.../domain/port/out/AgentRepository.java`
- Create: `services/onboarding-service/src/main/java/.../domain/service/AgentService.java`
- Create: `services/onboarding-service/src/main/java/.../infrastructure/persistence/entity/AgentEntity.java`
- Create: `services/onboarding-service/src/main/java/.../infrastructure/persistence/repository/AgentJpaRepository.java`
- Create: `services/onboarding-service/src/main/java/.../infrastructure/persistence/repository/AgentRepositoryImpl.java`
- Create: `services/onboarding-service/src/main/java/.../infrastructure/persistence/mapper/AgentMapper.java`
- Modify: `services/onboarding-service/src/main/java/.../infrastructure/web/OnboardingController.java`
- Create: `services/onboarding-service/src/test/java/.../domain/service/AgentServiceTest.java`
- Create: Flyway migration: `services/onboarding-service/src/main/resources/db/migration/V3__create_agent_table.sql`

- [ ] **Step 1: Write failing test for agent creation (BDD-BO01)**

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

### Task 2.2: Implement Customer Balance Inquiry [DONE] ✓

**BDD Scenarios:** BDD-L04, BDD-L04-EC-01
**BRD Requirements:** US-L04, FR-5.1

**User-Facing:** NO

**Files:**
- Create: `services/ledger-service/src/main/java/.../domain/port/in/CustomerBalanceInquiryUseCase.java`
- Create: `services/ledger-service/src/main/java/.../infrastructure/external/ExternalSwitchAdapter.java`
- Modify: `services/ledger-service/src/main/java/.../infrastructure/web/LedgerController.java` — add POST /balance-inquiry endpoint

- [ ] **Step 1: Write failing test for balance inquiry (BDD-L04)**

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

### Task 2.3: Integrate Geofence Validation into Transaction Flow

**BDD Scenarios:** BDD-W01-EC-05, BDD-W01-EC-06
**BRD Requirements:** NFR-4.2, FR-3.3

**User-Facing:** NO

**Files:**
- Modify: `services/ledger-service/src/main/java/.../domain/service/LedgerService.java` — add geofence check
- Modify: `services/ledger-service/src/main/java/.../application/usecase/ProcessWithdrawalUseCaseImpl.java` — pass GPS to domain service

- [ ] **Step 1: Write failing test for geofence validation (BDD-W01-EC-05)**

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

### Task 3.1: Implement Transaction Orchestrator Saga

**BDD Scenarios:** BDD-W01 (full withdrawal flow), BDD-V01 (reversal)
**BRD Requirements:** US-L05, FR-3.1 through FR-3.5, FR-18.1 through FR-18.4

**User-Facing:** NO

**Files:**
- Create: `services/orchestrator-service/` (new service)
- Create: `services/orchestrator-service/src/main/java/.../domain/service/TransactionOrchestrator.java`
- Create: `services/orchestrator-service/src/main/java/.../infrastructure/web/OrchestratorController.java`
- Create: `services/orchestrator-service/build.gradle`

- [ ] **Step 1: Create orchestrator service module**

- [ ] **Step 2: Define saga flow: BlockFloat → SwitchAuth → Commit**

```java
public class TransactionOrchestrator {
    public TransactionResult executeSaga(WithdrawalRequest request) {
        // Step 1: Check idempotency
        // Step 2: Call Rules Service (velocity, limits, fees)
        // Step 3: Call Ledger Service (BlockFloat)
        // Step 4: Call Switch Adapter (Auth)
        // Step 5: If success → Ledger Commit; If fail → Ledger Rollback
        // Step 6: Publish Kafka events
        // Step 7: Cache response
    }
}
```

- [ ] **Step 3: Implement Resilience4j circuit breaker + timeout (25s for switch)**

- [ ] **Step 4: Implement fallback → trigger reversal on timeout**

- [ ] **Step 5: Run tests**

- [ ] **Step 6: Commit**

---

## Phase 4: Audit Logging

### Task 4.1: Implement Audit Log Entity and Service [DONE] ✓

**BDD Scenarios:** BDD-BO05, BDD-BO05-EC-01
**BRD Requirements:** US-BO05, FR-13.5, NFR-4.3

**User-Facing:** NO

**Files:**
- Create: `services/common/src/main/java/.../audit/AuditLogRecord.java`
- Create: `services/common/src/main/java/.../audit/AuditLogService.java`
- Create: `services/common/src/main/java/.../audit/AuditAction.java`

- [ ] **Step 1: Create audit log domain model**

```java
public record AuditLogRecord(
    UUID auditId,
    String entityType,
    UUID entityId,
    AuditAction action,
    String performedBy,
    String changes,  // JSON
    String ipAddress,
    LocalDateTime timestamp
) {}
```

- [ ] **Step 2: Create audit log service (append-only, immutable)**

- [ ] **Step 3: Wire into ledger service (log all financial transactions)**

- [ ] **Step 4: Wire into onboarding service (log agent CRUD)**

- [ ] **Step 5: Create backoffice endpoint for audit log viewing**

- [ ] **Step 6: Commit**

---

## Phase 5: EOD Settlement

### Task 5.1: Implement EOD Net Settlement Batch Job [DONE] ✓

**BDD Scenarios:** BDD-SM01 through BDD-SM02-EC-01
**BRD Requirements:** US-SM01, US-SM02, FR-16.1 through FR-16.5

**User-Facing:** NO

**Files:**
- Create: `services/ledger-service/src/main/java/.../domain/model/SettlementSummaryRecord.java`
- Create: `services/ledger-service/src/main/java/.../domain/port/out/SettlementSummaryRepository.java`
- Create: `services/ledger-service/src/main/java/.../domain/service/SettlementService.java`
- Create: `services/ledger-service/src/main/java/.../infrastructure/persistence/entity/SettlementSummaryEntity.java`
- Create: `services/ledger-service/src/main/java/.../infrastructure/persistence/repository/SettlementSummaryJpaRepository.java`
- Create: `services/ledger-service/src/main/java/.../application/job/EodSettlementJob.java`
- Create: Flyway migration for `settlement_summary` table

- [x] **Step 1: Create SettlementSummary domain model and entity**
- [x] **Step 2: Write failing test for settlement calculation (BDD-SM01)**
- [x] **Step 3: Implement settlement calculation service**
- [x] **Step 4: Implement @Scheduled job at 23:59:59 MYT**
- [x] **Step 5: Implement CBS file generation (CSV)**
- [x] **Step 6: Run tests**
- [x] **Step 7: Commit**

- [ ] **Step 2: Write failing test for settlement calculation (BDD-SM01)**

```java
@Test
void shouldCalculatePositiveNetSettlement() {
    // Given: Withdrawals=10000, Deposits=3000, BillPayments=2000, Commissions=500
    // When: calculateNetSettlement
    // Then: net=5500, direction=BANK_OWES_AGENT
}
```

- [ ] **Step 3: Implement settlement calculation service**

```java
// Formula: (Withdrawals + Commissions + RetailSales) - (Deposits + BillPayments)
```

- [ ] **Step 4: Implement @Scheduled job at 23:59:59 MYT**

- [ ] **Step 5: Implement CBS file generation (CSV)**

- [ ] **Step 6: Run tests**

- [ ] **Step 7: Commit**

---

## Phase 6: Integration Tests

### Task 6.1: Add Testcontainers Integration Tests [DONE] ✓

**BDD Scenarios:** All — integration tests validate end-to-end flows
**BRD Requirements:** All

**User-Facing:** NO

**Files:**
- Create: `services/ledger-service/src/test/java/.../integration/LedgerIntegrationTest.java`
- Create: `services/rules-service/src/test/java/.../integration/RulesIntegrationTest.java`
- Create: `docker-compose.test.yaml`

- [x] **Step 1: Add Testcontainers dependency**
- [x] **Step 2: Create integration test for withdrawal flow**
- [x] **Step 3: Run integration tests**
- [x] **Step 4: Commit**

```groovy
testImplementation 'org.testcontainers:postgresql:1.19.3'
testImplementation 'org.testcontainers:junit-jupiter:1.19.3'
```

- [ ] **Step 2: Create integration test for withdrawal flow**

```java
@SpringBootTest
@Testcontainers
class LedgerIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Test
    void shouldProcessWithdrawalEndToEnd() {
        // Given: Agent exists, float has balance
        // When: Withdrawal request via REST
        // Then: Balance updated, journal entries created
    }
}
```

- [ ] **Step 3: Run integration tests**

Run: `./gradlew integrationTest`
Expected: PASS

- [ ] **Step 4: Commit**

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
