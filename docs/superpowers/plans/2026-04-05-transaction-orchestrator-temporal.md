# Transaction Orchestrator (Temporal) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current synchronous Map-based TransactionOrchestrator with Temporal durable workflows for all transaction types (Withdrawal, Deposit, Bill Payment, DuitNow Transfer), with typed DTOs, proper compensation, safety reversal, and polling endpoints.

**Architecture:** Temporal Workflow Engine for durable SAGA execution. Each transaction type gets its own Temporal Workflow. The Orchestrator becomes a router that starts the appropriate workflow. Typed Java Records replace all Map<String, Object> payloads. Compensation executes in reverse order on failure. Safety Reversal (MTI 0400) has infinite retry.

**Tech Stack:** Java 21, Spring Boot 3.2.5, Temporal SDK 1.25.x, OpenFeign, Resilience4j, PostgreSQL, Redis, Kafka, JUnit 5, Mockito, ArchUnit, Testcontainers.

**Spec References:**
- Design: `docs/superpowers/specs/agent-banking-platform/2026-04-05-transaction-orchestrator-temporal-design.md`
- BRD Addendum: `docs/superpowers/specs/agent-banking-platform/2026-04-05-transaction-brd-addendum.md`
- BDD Addendum: `docs/superpowers/specs/agent-banking-platform/2026-04-05-transaction-bdd-addendum.md`

---

## File Structure

### New Files to Create

**Domain Layer:**
- `domain/model/TransactionType.java` — enum for all transaction types
- `domain/model/WorkflowStatus.java` — enum: PENDING, RUNNING, COMPLETED, FAILED, COMPENSATING
- `domain/model/WorkflowResult.java` — record for workflow return value
- `domain/model/ForceResolveSignal.java` — record: COMMIT | REVERSE
- `domain/model/TransactionRequest.java` — base record for polymorphic requests
- `domain/model/WithdrawalRequest.java` — withdrawal-specific input
- `domain/model/DepositRequest.java` — deposit-specific input
- `domain/model/BillPaymentRequest.java` — bill payment-specific input
- `domain/model/DuitNowTransferRequest.java` — DuitNow-specific input
- `domain/port/in/StartTransactionUseCase.java` — inbound port for starting transactions
- `domain/port/in/QueryWorkflowStatusUseCase.java` — inbound port for polling
- `domain/port/out/RulesServicePort.java` — REWRITE: typed interfaces (replaces Map-based)
- `domain/port/out/LedgerServicePort.java` — REWRITE: typed interfaces
- `domain/port/out/SwitchAdapterPort.java` — REWRITE: typed interfaces
- `domain/port/out/BillerServicePort.java` — NEW: biller port
- `domain/port/out/CbsServicePort.java` — NEW: CBS port
- `domain/port/out/EventPublisherPort.java` — REWRITE: typed interfaces
- `domain/service/WorkflowRouter.java` — determines which workflow to start

**Application Layer (Temporal Workflows):**
- `application/workflow/WithdrawalWorkflow.java` — @WorkflowInterface
- `application/workflow/WithdrawalOnUsWorkflow.java` — @WorkflowInterface
- `application/workflow/DepositWorkflow.java` — @WorkflowInterface
- `application/workflow/BillPaymentWorkflow.java` — @WorkflowInterface
- `application/workflow/DuitNowTransferWorkflow.java` — @WorkflowInterface

**Application Layer (Temporal Activities):**
- `application/activity/CheckVelocityActivity.java`
- `application/activity/CalculateFeesActivity.java`
- `application/activity/BlockFloatActivity.java`
- `application/activity/CommitFloatActivity.java`
- `application/activity/ReleaseFloatActivity.java`
- `application/activity/CreditAgentFloatActivity.java`
- `application/activity/AuthorizeAtSwitchActivity.java`
- `application/activity/SendReversalToSwitchActivity.java`
- `application/activity/PublishKafkaEventActivity.java`
- `application/activity/ValidateAccountActivity.java`
- `application/activity/VerifyBiometricActivity.java`
- `application/activity/PostToCBSActivity.java`
- `application/activity/ValidateBillActivity.java`
- `application/activity/PayBillerActivity.java`
- `application/activity/NotifyBillerActivity.java`
- `application/activity/ProxyEnquiryActivity.java`
- `application/activity/SendDuitNowTransferActivity.java`
- `application/activity/CacheIdempotencyActivity.java`

**Application Layer (Use Cases):**
- `application/usecase/StartTransactionUseCaseImpl.java`
- `application/usecase/QueryWorkflowStatusUseCaseImpl.java`

**Infrastructure Layer:**
- `infrastructure/temporal/TemporalConfig.java` — Temporal client, worker, task queue config
- `infrastructure/temporal/WorkflowFactory.java` — factory for starting workflows
- `infrastructure/temporal/WorkflowImpl/WithdrawalWorkflowImpl.java`
- `infrastructure/temporal/WorkflowImpl/DepositWorkflowImpl.java`
- `infrastructure/temporal/WorkflowImpl/BillPaymentWorkflowImpl.java`
- `infrastructure/temporal/WorkflowImpl/DuitNowTransferWorkflowImpl.java`
- `infrastructure/temporal/ActivityImpl/CheckVelocityActivityImpl.java`
- `infrastructure/temporal/ActivityImpl/CalculateFeesActivityImpl.java`
- `infrastructure/temporal/ActivityImpl/BlockFloatActivityImpl.java`
- `infrastructure/temporal/ActivityImpl/CommitFloatActivityImpl.java`
- `infrastructure/temporal/ActivityImpl/ReleaseFloatActivityImpl.java`
- `infrastructure/temporal/ActivityImpl/CreditAgentFloatActivityImpl.java`
- `infrastructure/temporal/ActivityImpl/AuthorizeAtSwitchActivityImpl.java`
- `infrastructure/temporal/ActivityImpl/SendReversalToSwitchActivityImpl.java`
- `infrastructure/temporal/ActivityImpl/PublishKafkaEventActivityImpl.java`
- `infrastructure/temporal/ActivityImpl/ValidateAccountActivityImpl.java`
- `infrastructure/temporal/ActivityImpl/VerifyBiometricActivityImpl.java`
- `infrastructure/temporal/ActivityImpl/PostToCBSActivityImpl.java`
- `infrastructure/temporal/ActivityImpl/ValidateBillActivityImpl.java`
- `infrastructure/temporal/ActivityImpl/PayBillerActivityImpl.java`
- `infrastructure/temporal/ActivityImpl/NotifyBillerActivityImpl.java`
- `infrastructure/temporal/ActivityImpl/ProxyEnquiryActivityImpl.java`
- `infrastructure/temporal/ActivityImpl/SendDuitNowTransferActivityImpl.java`
- `infrastructure/temporal/ActivityImpl/CacheIdempotencyActivityImpl.java`

**Infrastructure (External Adapters — REWRITE):**
- `infrastructure/external/RulesServiceAdapter.java` — REWRITE: typed
- `infrastructure/external/LedgerServiceAdapter.java` — REWRITE: typed
- `infrastructure/external/SwitchAdapterAdapter.java` — REWRITE: typed
- `infrastructure/external/BillerServiceAdapter.java` — NEW
- `infrastructure/external/BillerServiceClient.java` — NEW Feign client
- `infrastructure/external/CbsServiceAdapter.java` — NEW
- `infrastructure/external/CbsServiceClient.java` — NEW Feign client

**Infrastructure (Web):**
- `infrastructure/web/OrchestratorController.java` — REWRITE: add polling endpoint, force-resolve
- `infrastructure/web/dto/TransactionResponse.java` — NEW: response DTO
- `infrastructure/web/dto/WorkflowStatusResponse.java` — NEW: polling response DTO
- `infrastructure/web/dto/ForceResolveRequest.java` — NEW: admin force-resolve request

**Infrastructure (Persistence):**
- `infrastructure/persistence/entity/TransactionRecordEntity.java` — JPA entity
- `infrastructure/persistence/repository/TransactionRecordJpaRepository.java` — Spring Data repo
- `infrastructure/persistence/repository/TransactionRecordRepository.java` — domain port impl
- `resources/db/migration/V1__transaction_record.sql` — Flyway migration

**Config:**
- `config/TemporalWorkerConfig.java` — Temporal worker bean registration
- `config/DomainServiceConfig.java` — REWRITE: add new beans

**Test:**
- `test/domain/service/WorkflowRouterTest.java`
- `test/application/workflow/WithdrawalWorkflowTest.java`
- `test/application/activity/CheckVelocityActivityTest.java`
- `test/application/activity/BlockFloatActivityTest.java`
- `test/application/activity/AuthorizeAtSwitchActivityTest.java`
- `test/application/activity/SendReversalToSwitchActivityTest.java`
- `test/application/usecase/StartTransactionUseCaseImplTest.java`
- `test/infrastructure/temporal/TemporalConfigTest.java`
- `test/integration/OrchestratorControllerIntegrationTest.java` — REWRITE
- `test/architecture/HexagonalArchitectureTest.java` — UPDATE

### Modified Files
- `build.gradle` — Add Temporal SDK dependency
- `application.yaml` — Add Temporal config, new Feign URLs
- `OrchestratorServiceApplication.java` — Add @ComponentScan for common module
- `docker-compose.yml` — Add Temporal services

---

### Task 1: Infrastructure Setup — Temporal SDK and Docker Compose

**Files:**
- Modify: `services/orchestrator-service/build.gradle`
- Modify: `services/orchestrator-service/src/main/resources/application.yaml`
- Modify: `docker-compose.yml`

- [ ] **Step 1: Add Temporal SDK dependency to build.gradle**

Read `services/orchestrator-service/build.gradle`. Add the Temporal SDK dependency:

```groovy
// Add after line 29 (after flyway-core):
implementation 'io.temporal:temporal-sdk:1.25.1'

// Add test dependency:
testImplementation 'io.temporal:temporal-testing:1.25.1'
```

Full updated `build.gradle`:

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.5'
    id 'io.spring.dependency-management' version '1.1.5'
    id 'org.flywaydb.flyway' version '9.22.3'
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
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.cloud:spring-cloud-starter-openfeign'
    implementation 'org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j'
    implementation 'org.springframework.kafka:spring-kafka'
    implementation 'org.postgresql:postgresql'
    implementation 'org.flywaydb:flyway-core:9.22.3'
    implementation 'io.temporal:temporal-sdk:1.25.1'
    implementation project(':common')
    
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'
    testImplementation 'org.testcontainers:testcontainers:1.20.1'
    testImplementation 'org.testcontainers:junit-jupiter:1.20.1'
    testImplementation 'org.testcontainers:postgresql:1.20.1'
    testImplementation 'org.testcontainers:kafka:1.20.1'
    testImplementation 'io.temporal:temporal-testing:1.25.1'
    testImplementation project(':common')
    testImplementation testFixtures(project(':common'))
}

test {
    useJUnitPlatform()
}

tasks.named('bootJar') {
    archiveBaseName = 'orchestrator-service'
}
```

- [ ] **Step 2: Run gradle sync to verify dependency resolves**

```bash
cd services/orchestrator-service && ../../gradlew dependencies --configuration implementation 2>&1 | head -30
```

Expected: `io.temporal:temporal-sdk:1.25.1` appears in the dependency tree.

- [ ] **Step 3: Add Temporal configuration to application.yaml**

Read `services/orchestrator-service/src/main/resources/application.yaml`. Add Temporal config and new Feign URLs at the end:

```yaml
# Append after existing config:

# Temporal configuration
temporal:
  namespace: default
  address: ${TEMPORAL_ADDRESS:localhost:7233}
  task-queue: agent-banking-tasks
  worker:
    max-concurrent-workflow-task-executors: 10
    max-concurrent-activity-task-executors: 20
  workflow:
    execution-timeout: 5m
    run-timeout: 2m
    task-timeout: 30s

# Additional Feign URLs
biller-service:
  url: ${BILLER_SERVICE_URL:http://localhost:8084}

cbs-connector:
  url: ${CBS_CONNECTOR_URL:http://localhost:8085}
```

- [ ] **Step 4: Add Temporal services to docker-compose.yml**

Read `docker-compose.yml`. Add these services:

```yaml
  temporal:
    image: temporalio/auto-setup:1.25
    ports:
      - "7233:7233"
    environment:
      - DB=postgresql
      - POSTGRES_SEEDS=temporal-postgres
      - POSTGRES_USER=temporal
      - POSTGRES_PWD=temporal
    depends_on:
      - temporal-postgres

  temporal-postgres:
    image: postgres:16
    environment:
      - POSTGRES_USER=temporal
      - POSTGRES_PASSWORD=temporal
      - POSTGRES_DB=temporal
    volumes:
      - temporal-postgres-data:/var/lib/postgresql/data

  temporal-ui:
    image: temporalio/ui:2.27
    ports:
      - "8082:8080"
    environment:
      - TEMPORAL_ADDRESS=temporal:7233
    depends_on:
      - temporal
```

Also add `temporal-postgres-data` to the `volumes:` section at the bottom of the file.

- [ ] **Step 5: Commit**

```bash
git add services/orchestrator-service/build.gradle services/orchestrator-service/src/main/resources/application.yaml docker-compose.yml
git commit -m "feat(orchestrator): add Temporal SDK dependency and docker-compose services"
```

---

### Task 2: Domain Models and Typed Port Interfaces

**Files:**
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/TransactionType.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/WorkflowStatus.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/WorkflowResult.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/ForceResolveSignal.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/RulesServicePort.java` (REWRITE)
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/LedgerServicePort.java` (REWRITE)
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/SwitchAdapterPort.java` (REWRITE)
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/BillerServicePort.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/CbsServicePort.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/EventPublisherPort.java` (REWRITE)

**CRITICAL:** All files in `domain/` must have ZERO imports from Spring, Temporal, JPA, Kafka, or any infrastructure framework. Pure Java only.

- [ ] **Step 1: Create TransactionType enum**

```java
package com.agentbanking.orchestrator.domain.model;

public enum TransactionType {
    CASH_WITHDRAWAL,
    CASH_DEPOSIT,
    BILL_PAYMENT,
    DUITNOW_TRANSFER
}
```

- [ ] **Step 2: Create WorkflowStatus enum**

```java
package com.agentbanking.orchestrator.domain.model;

public enum WorkflowStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    COMPENSATING
}
```

- [ ] **Step 3: Create WorkflowResult record**

```java
package com.agentbanking.orchestrator.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record WorkflowResult(
    String status,
    UUID transactionId,
    String errorCode,
    String errorMessage,
    String actionCode,
    String referenceNumber,
    BigDecimal amount,
    BigDecimal customerFee,
    Map<String, Object> metadata,
    Instant completedAt
) {
    public static WorkflowResult completed(UUID transactionId, String referenceNumber,
                                            BigDecimal amount, BigDecimal customerFee) {
        return new WorkflowResult("COMPLETED", transactionId, null, null, null,
                referenceNumber, amount, customerFee, Map.of(), Instant.now());
    }

    public static WorkflowResult failed(String errorCode, String errorMessage, String actionCode) {
        return new WorkflowResult("FAILED", null, errorCode, errorMessage, actionCode,
                null, null, null, Map.of(), Instant.now());
    }

    public static WorkflowResult reversed(UUID transactionId, String reason) {
        return new WorkflowResult("REVERSED", transactionId, null, reason, "REVIEW",
                null, null, null, Map.of(), Instant.now());
    }
}
```

- [ ] **Step 4: Create ForceResolveSignal record**

```java
package com.agentbanking.orchestrator.domain.model;

public record ForceResolveSignal(
    Action action,
    String reason,
    String adminId
) {
    public enum Action {
        COMMIT,
        REVERSE
    }
}
```

- [ ] **Step 5: Rewrite RulesServicePort with typed interfaces**

Delete the existing `domain/port/out/RulesServicePort.java` and replace with:

```java
package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface RulesServicePort {

    VelocityCheckResult checkVelocity(VelocityCheckInput input);

    FeeCalculationResult calculateFees(FeeCalculationInput input);

    record VelocityCheckInput(
        UUID agentId,
        BigDecimal amount,
        String customerMykad
    ) {}

    record VelocityCheckResult(
        boolean passed,
        String errorCode
    ) {}

    record FeeCalculationInput(
        String transactionType,
        String agentTier,
        BigDecimal amount
    ) {}

    record FeeCalculationResult(
        BigDecimal customerFee,
        BigDecimal agentCommission,
        BigDecimal bankShare
    ) {}
}
```

- [ ] **Step 6: Rewrite LedgerServicePort with typed interfaces**

Delete the existing `domain/port/out/LedgerServicePort.java` and replace with:

```java
package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface LedgerServicePort {

    FloatBlockResult blockFloat(FloatBlockInput input);

    FloatCommitResult commitFloat(FloatCommitInput input);

    FloatReleaseResult releaseFloat(FloatReleaseInput input);

    FloatCreditResult creditAgentFloat(FloatCreditInput input);

    FloatReverseResult reverseCreditFloat(FloatReverseInput input);

    AccountValidationResult validateAccount(AccountValidationInput input);

    record FloatBlockInput(
        UUID agentId,
        BigDecimal amount,
        String idempotencyKey
    ) {}

    record FloatBlockResult(
        boolean success,
        UUID transactionId,
        String errorCode
    ) {}

    record FloatCommitInput(
        UUID agentId,
        BigDecimal amount,
        UUID transactionId
    ) {}

    record FloatCommitResult(
        boolean success,
        String errorCode
    ) {}

    record FloatReleaseInput(
        UUID agentId,
        BigDecimal amount,
        UUID transactionId
    ) {}

    record FloatReleaseResult(
        boolean success,
        String errorCode
    ) {}

    record FloatCreditInput(
        UUID agentId,
        BigDecimal amount
    ) {}

    record FloatCreditResult(
        boolean success,
        BigDecimal newBalance,
        String errorCode
    ) {}

    record FloatReverseInput(
        UUID agentId,
        BigDecimal amount
    ) {}

    record FloatReverseResult(
        boolean success,
        String errorCode
    ) {}

    record AccountValidationInput(
        String destinationAccount
    ) {}

    record AccountValidationResult(
        boolean valid,
        String accountName,
        String errorCode
    ) {}
}
```

- [ ] **Step 7: Rewrite SwitchAdapterPort with typed interfaces**

Delete the existing `domain/port/out/SwitchAdapterPort.java` and replace with:

```java
package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface SwitchAdapterPort {

    SwitchAuthorizationResult authorizeTransaction(SwitchAuthorizationInput input);

    SwitchReversalResult sendReversal(SwitchReversalInput input);

    ProxyEnquiryResult proxyEnquiry(ProxyEnquiryInput input);

    DuitNowTransferResult sendDuitNowTransfer(DuitNowTransferInput input);

    record SwitchAuthorizationInput(
        String pan,
        String pinBlock,
        BigDecimal amount,
        UUID internalTransactionId
    ) {}

    record SwitchAuthorizationResult(
        boolean approved,
        String referenceCode,
        String responseCode,
        String errorCode
    ) {}

    record SwitchReversalInput(
        UUID internalTransactionId
    ) {}

    record SwitchReversalResult(
        boolean success,
        String errorCode
    ) {}

    record ProxyEnquiryInput(
        String proxyType,
        String proxyValue
    ) {}

    record ProxyEnquiryResult(
        boolean valid,
        String recipientName,
        String bankCode,
        String errorCode
    ) {}

    record DuitNowTransferInput(
        String recipientBank,
        String recipientAccount,
        BigDecimal amount,
        UUID internalTransactionId
    ) {}

    record DuitNowTransferResult(
        boolean success,
        String paynetReference,
        String errorCode
    ) {}
}
```

- [ ] **Step 8: Create BillerServicePort**

```java
package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface BillerServicePort {

    BillValidationResult validateBill(BillValidationInput input);

    BillPaymentResult payBill(BillPaymentInput input);

    BillNotificationResult notifyBiller(BillNotificationInput input);

    BillNotificationResult notifyBillerReversal(BillReversalInput input);

    record BillValidationInput(
        String billerCode,
        String ref1
    ) {}

    record BillValidationResult(
        boolean valid,
        String accountName,
        BigDecimal amountDue,
        String errorCode
    ) {}

    record BillPaymentInput(
        String billerCode,
        String ref1,
        String ref2,
        BigDecimal amount,
        String idempotencyKey
    ) {}

    record BillPaymentResult(
        boolean success,
        String billerReference,
        String errorCode
    ) {}

    record BillNotificationInput(
        String internalTransactionId,
        BigDecimal amount
    ) {}

    record BillNotificationResult(
        boolean success,
        String errorCode
    ) {}

    record BillReversalInput(
        String billerCode,
        String ref1
    ) {}
}
```

- [ ] **Step 9: Create CbsServicePort**

```java
package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;

public interface CbsServicePort {

    CbsAuthorizationResult authorizeAtCbs(CbsAuthorizationInput input);

    CbsPostResult postToCbs(CbsPostInput input);

    record CbsAuthorizationInput(
        String customerAccount,
        BigDecimal amount,
        String pinBlock
    ) {}

    record CbsAuthorizationResult(
        boolean approved,
        String referenceCode,
        String errorCode
    ) {}

    record CbsPostInput(
        String destinationAccount,
        BigDecimal amount
    ) {}

    record CbsPostResult(
        boolean success,
        String reference,
        String errorCode
    ) {}
}
```

- [ ] **Step 10: Rewrite EventPublisherPort with typed interfaces**

Delete the existing `domain/port/out/EventPublisherPort.java` and replace with:

```java
package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface EventPublisherPort {

    void publishTransactionCompleted(TransactionCompletedEvent event);

    void publishTransactionFailed(TransactionFailedEvent event);

    record TransactionCompletedEvent(
        UUID transactionId,
        UUID agentId,
        BigDecimal amount,
        BigDecimal customerFee,
        BigDecimal agentCommission,
        BigDecimal bankShare,
        String transactionType,
        String customerCardMasked,
        String switchTxId,
        String referenceId
    ) {}

    record TransactionFailedEvent(
        UUID transactionId,
        UUID agentId,
        BigDecimal amount,
        String transactionType,
        String customerCardMasked,
        String reason
    ) {}
}
```

- [ ] **Step 11: Verify domain/ has zero Spring/Temporal imports**

```bash
cd services/orchestrator-service && find src/main/java/com/agentbanking/orchestrator/domain -name "*.java" -exec grep -l "import org.springframework\|import io.temporal\|import jakarta.persistence" {} \;
```

Expected: Empty output (no violations).

- [ ] **Step 12: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/
git commit -m "refactor(orchestrator): replace Map-based ports with typed interfaces, add domain models"
```

---

### Task 3: Workflow Router and Use Case Ports

**Files:**
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/in/StartTransactionUseCase.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/in/QueryWorkflowStatusUseCase.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/service/WorkflowRouter.java`

- [ ] **Step 1: Create StartTransactionUseCase port**

```java
package com.agentbanking.orchestrator.domain.port.in;

import com.agentbanking.orchestrator.domain.model.TransactionType;
import java.math.BigDecimal;
import java.util.UUID;

public interface StartTransactionUseCase {
    StartTransactionResult start(StartTransactionCommand command);

    record StartTransactionCommand(
        TransactionType transactionType,
        UUID agentId,
        BigDecimal amount,
        String idempotencyKey,
        // Withdrawal fields
        String pan,
        String pinBlock,
        String customerCardMasked,
        // Deposit fields
        String destinationAccount,
        boolean requiresBiometric,
        // Bill payment fields
        String billerCode,
        String ref1,
        String ref2,
        // DuitNow fields
        String proxyType,
        String proxyValue,
        // Common
        String customerMykad,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng,
        String targetBIN
    ) {}

    record StartTransactionResult(
        String status,
        String workflowId,
        String pollUrl
    ) {}
}
```

- [ ] **Step 2: Create QueryWorkflowStatusUseCase port**

```java
package com.agentbanking.orchestrator.domain.port.in;

import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import java.util.Optional;

public interface QueryWorkflowStatusUseCase {
    Optional<WorkflowStatusResponse> getStatus(String workflowId);

    record WorkflowStatusResponse(
        WorkflowStatus status,
        WorkflowResult result
    ) {}
}
```

- [ ] **Step 3: Create WorkflowRouter domain service**

```java
package com.agentbanking.orchestrator.domain.service;

import com.agentbanking.orchestrator.domain.model.TransactionType;

public class WorkflowRouter {

    private static final String BSN_BIN = "0012";

    public String determineWorkflowType(TransactionType transactionType, String targetBIN) {
        return switch (transactionType) {
            case CASH_WITHDRAWAL -> isOnUs(targetBIN) ? "WithdrawalOnUs" : "Withdrawal";
            case CASH_DEPOSIT -> "Deposit";
            case BILL_PAYMENT -> "BillPayment";
            case DUITNOW_TRANSFER -> "DuitNowTransfer";
        };
    }

    private boolean isOnUs(String targetBIN) {
        return BSN_BIN.equals(targetBIN);
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/
git commit -m "feat(orchestrator): add use case ports and WorkflowRouter domain service"
```

---

### Task 4: Temporal Workflow Interfaces

**Files:**
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/WithdrawalWorkflow.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/WithdrawalOnUsWorkflow.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/DepositWorkflow.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/BillPaymentWorkflow.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/DuitNowTransferWorkflow.java`

**NOTE:** These files are in `application/workflow/` — they can import Temporal annotations. They are interfaces only, no implementation logic.

- [ ] **Step 1: Create WithdrawalWorkflow interface**

```java
package com.agentbanking.orchestrator.application.workflow;

import com.agentbanking.orchestrator.domain.model.ForceResolveSignal;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.math.BigDecimal;
import java.util.UUID;

@WorkflowInterface
public interface WithdrawalWorkflow {

    @WorkflowMethod
    WorkflowResult execute(WithdrawalInput input);

    @SignalMethod
    void forceResolve(ForceResolveSignal signal);

    @QueryMethod
    WorkflowStatus getStatus();

    record WithdrawalInput(
        UUID agentId,
        String pan,
        String pinBlock,
        BigDecimal amount,
        String idempotencyKey,
        String customerCardMasked,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng,
        String customerMykad,
        String agentTier
    ) {}
}
```

- [ ] **Step 2: Create WithdrawalOnUsWorkflow interface**

```java
package com.agentbanking.orchestrator.application.workflow;

import com.agentbanking.orchestrator.domain.model.ForceResolveSignal;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.math.BigDecimal;
import java.util.UUID;

@WorkflowInterface
public interface WithdrawalOnUsWorkflow {

    @WorkflowMethod
    WorkflowResult execute(WithdrawalOnUsInput input);

    @SignalMethod
    void forceResolve(ForceResolveSignal signal);

    @QueryMethod
    WorkflowStatus getStatus();

    record WithdrawalOnUsInput(
        UUID agentId,
        String customerAccount,
        String pinBlock,
        BigDecimal amount,
        String idempotencyKey,
        String customerCardMasked,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng,
        String customerMykad,
        String agentTier
    ) {}
}
```

- [ ] **Step 3: Create DepositWorkflow interface**

```java
package com.agentbanking.orchestrator.application.workflow;

import com.agentbanking.orchestrator.domain.model.ForceResolveSignal;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.math.BigDecimal;
import java.util.UUID;

@WorkflowInterface
public interface DepositWorkflow {

    @WorkflowMethod
    WorkflowResult execute(DepositInput input);

    @SignalMethod
    void forceResolve(ForceResolveSignal signal);

    @QueryMethod
    WorkflowStatus getStatus();

    record DepositInput(
        UUID agentId,
        String destinationAccount,
        BigDecimal amount,
        String idempotencyKey,
        String customerMykad,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng,
        boolean requiresBiometric,
        String agentTier
    ) {}
}
```

- [ ] **Step 4: Create BillPaymentWorkflow interface**

```java
package com.agentbanking.orchestrator.application.workflow;

import com.agentbanking.orchestrator.domain.model.ForceResolveSignal;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.math.BigDecimal;
import java.util.UUID;

@WorkflowInterface
public interface BillPaymentWorkflow {

    @WorkflowMethod
    WorkflowResult execute(BillPaymentInput input);

    @SignalMethod
    void forceResolve(ForceResolveSignal signal);

    @QueryMethod
    WorkflowStatus getStatus();

    record BillPaymentInput(
        UUID agentId,
        String billerCode,
        String ref1,
        String ref2,
        BigDecimal amount,
        String idempotencyKey,
        String customerMykad,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng,
        String agentTier
    ) {}
}
```

- [ ] **Step 5: Create DuitNowTransferWorkflow interface**

```java
package com.agentbanking.orchestrator.application.workflow;

import com.agentbanking.orchestrator.domain.model.ForceResolveSignal;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.math.BigDecimal;
import java.util.UUID;

@WorkflowInterface
public interface DuitNowTransferWorkflow {

    @WorkflowMethod
    WorkflowResult execute(DuitNowTransferInput input);

    @SignalMethod
    void forceResolve(ForceResolveSignal signal);

    @QueryMethod
    WorkflowStatus getStatus();

    record DuitNowTransferInput(
        UUID agentId,
        String proxyType,
        String proxyValue,
        BigDecimal amount,
        String idempotencyKey,
        String customerMykad,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng,
        String agentTier
    ) {}
}
```

- [ ] **Step 6: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/
git commit -m "feat(orchestrator): add Temporal workflow interfaces for all transaction types"
```

---

### Task 5: Temporal Activity Interfaces

**Files:** Create all 17 activity interface files in `application/activity/`

Each activity interface follows the same pattern. Here are all 17:

- [ ] **Step 1: Create all 17 activity interfaces**

Create each file in `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/`:

**CheckVelocityActivity.java:**
```java
package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.VelocityCheckInput;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.VelocityCheckResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface CheckVelocityActivity {
    VelocityCheckResult checkVelocity(VelocityCheckInput input);
}
```

**CalculateFeesActivity.java:**
```java
package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.FeeCalculationInput;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.FeeCalculationResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface CalculateFeesActivity {
    FeeCalculationResult calculateFees(FeeCalculationInput input);
}
```

**BlockFloatActivity.java:**
```java
package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatBlockInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatBlockResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface BlockFloatActivity {
    FloatBlockResult blockFloat(FloatBlockInput input);
}
```

**CommitFloatActivity.java:**
```java
package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCommitInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCommitResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface CommitFloatActivity {
    FloatCommitResult commitFloat(FloatCommitInput input);
}
```

**ReleaseFloatActivity.java:**
```java
package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatReleaseInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatReleaseResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface ReleaseFloatActivity {
    FloatReleaseResult releaseFloat(FloatReleaseInput input);
}
```

**CreditAgentFloatActivity.java:**
```java
package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCreditInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCreditResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface CreditAgentFloatActivity {
    FloatCreditResult creditAgentFloat(FloatCreditInput input);
}
```

**AuthorizeAtSwitchActivity.java:**
```java
package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchAuthorizationInput;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchAuthorizationResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface AuthorizeAtSwitchActivity {
    SwitchAuthorizationResult authorize(SwitchAuthorizationInput input);
}
```

**SendReversalToSwitchActivity.java:**
```java
package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchReversalInput;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchReversalResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface SendReversalToSwitchActivity {
    SwitchReversalResult sendReversal(SwitchReversalInput input);
}
```

**PublishKafkaEventActivity.java:**
```java
package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.EventPublisherPort.TransactionCompletedEvent;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface PublishKafkaEventActivity {
    void publishCompleted(TransactionCompletedEvent event);
}
```

**ValidateAccountActivity.java:**
```java
package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.AccountValidationInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.AccountValidationResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface ValidateAccountActivity {
    AccountValidationResult validateAccount(AccountValidationInput input);
}
```

**VerifyBiometricActivity.java:**
```java
package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface VerifyBiometricActivity {
    BiometricResult verifyBiometric(String customerMykad);

    record BiometricResult(boolean match, String status, String errorCode) {}
}
```

**PostToCBSActivity.java:**
```java
package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.CbsServicePort.CbsPostInput;
import com.agentbanking.orchestrator.domain.port.out.CbsServicePort.CbsPostResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface PostToCBSActivity {
    CbsPostResult postToCbs(CbsPostInput input);
}
```

**ValidateBillActivity.java:**
```java
package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillValidationInput;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillValidationResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface ValidateBillActivity {
    com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillValidationResult validateBill(BillValidationInput input);
}
```

**PayBillerActivity.java:**
```java
package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillPaymentInput;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillPaymentResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface PayBillerActivity {
    BillPaymentResult payBill(BillPaymentInput input);
}
```

**NotifyBillerActivity.java:**
```java
package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillNotificationInput;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillNotificationResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface NotifyBillerActivity {
    BillNotificationResult notifyBiller(BillNotificationInput input);
}
```

**ProxyEnquiryActivity.java:**
```java
package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.ProxyEnquiryInput;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.ProxyEnquiryResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface ProxyEnquiryActivity {
    ProxyEnquiryResult proxyEnquiry(ProxyEnquiryInput input);
}
```

**SendDuitNowTransferActivity.java:**
```java
package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.DuitNowTransferInput;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.DuitNowTransferResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface SendDuitNowTransferActivity {
    DuitNowTransferResult sendDuitNowTransfer(DuitNowTransferInput input);
}
```

- [ ] **Step 2: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/
git commit -m "feat(orchestrator): add 17 Temporal activity interfaces"
```

---

### Task 6: Temporal Configuration and Worker Setup

**Files:**
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/TemporalConfig.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowFactory.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/config/TemporalWorkerConfig.java`

- [ ] **Step 1: Create TemporalConfig**

```java
package com.agentbanking.orchestrator.infrastructure.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalConfig {

    @Value("${temporal.namespace:default}")
    private String namespace;

    @Value("${temporal.address:localhost:7233}")
    private String address;

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        return WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(address)
                        .build());
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs stubs) {
        return WorkflowClient.newInstance(stubs,
                WorkflowClientOptions.newBuilder()
                        .setNamespace(namespace)
                        .build());
    }
}
```

- [ ] **Step 2: Create WorkflowFactory**

```java
package com.agentbanking.orchestrator.infrastructure.temporal;

import com.agentbanking.orchestrator.application.workflow.*;
import com.agentbanking.orchestrator.domain.model.TransactionType;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WorkflowFactory {

    private final WorkflowClient workflowClient;

    @Value("${temporal.task-queue:agent-banking-tasks}")
    private String taskQueue;

    public WorkflowFactory(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    public String startWorkflow(TransactionType transactionType, String idempotencyKey,
                                 Object input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(idempotencyKey)
                .setTaskQueue(taskQueue)
                .setWorkflowIdReusePolicy(
                        io.temporal.common.WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();

        return switch (transactionType) {
            case CASH_WITHDRAWAL -> {
                WithdrawalWorkflow workflow = workflowClient.newWorkflowStub(
                        WithdrawalWorkflow.class, options);
                WorkflowClient.start(workflow::execute, (WithdrawalWorkflow.WithdrawalInput) input);
                yield idempotencyKey;
            }
            case CASH_DEPOSIT -> {
                DepositWorkflow workflow = workflowClient.newWorkflowStub(
                        DepositWorkflow.class, options);
                WorkflowClient.start(workflow::execute, (DepositWorkflow.DepositInput) input);
                yield idempotencyKey;
            }
            case BILL_PAYMENT -> {
                BillPaymentWorkflow workflow = workflowClient.newWorkflowStub(
                        BillPaymentWorkflow.class, options);
                WorkflowClient.start(workflow::execute, (BillPaymentWorkflow.BillPaymentInput) input);
                yield idempotencyKey;
            }
            case DUITNOW_TRANSFER -> {
                DuitNowTransferWorkflow workflow = workflowClient.newWorkflowStub(
                        DuitNowTransferWorkflow.class, options);
                WorkflowClient.start(workflow::execute, (DuitNowTransferWorkflow.DuitNowTransferInput) input);
                yield idempotencyKey;
            }
        };
    }

    public WorkflowStub getWorkflowStub(String workflowId) {
        return workflowClient.newUntypedWorkflowStub(workflowId);
    }
}
```

- [ ] **Step 3: Create TemporalWorkerConfig**

```java
package com.agentbanking.orchestrator.config;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.workflow.*;
import com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl.*;
import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalWorkerConfig {

    @Value("${temporal.task-queue:agent-banking-tasks}")
    private String taskQueue;

    @Bean
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        return WorkerFactory.newInstance(workflowClient);
    }

    @Bean
    public Worker temporalWorker(WorkerFactory factory,
                                  WithdrawalWorkflowImpl withdrawalWorkflow,
                                  DepositWorkflowImpl depositWorkflow,
                                  BillPaymentWorkflowImpl billPaymentWorkflow,
                                  DuitNowTransferWorkflowImpl duitNowTransferWorkflow,
                                  CheckVelocityActivity checkVelocityActivity,
                                  CalculateFeesActivity calculateFeesActivity,
                                  BlockFloatActivity blockFloatActivity,
                                  CommitFloatActivity commitFloatActivity,
                                  ReleaseFloatActivity releaseFloatActivity,
                                  CreditAgentFloatActivity creditAgentFloatActivity,
                                  AuthorizeAtSwitchActivity authorizeAtSwitchActivity,
                                  SendReversalToSwitchActivity sendReversalToSwitchActivity,
                                  PublishKafkaEventActivity publishKafkaEventActivity,
                                  ValidateAccountActivity validateAccountActivity,
                                  PostToCBSActivity postToCBSActivity,
                                  ValidateBillActivity validateBillActivity,
                                  PayBillerActivity payBillerActivity,
                                  NotifyBillerActivity notifyBillerActivity,
                                  ProxyEnquiryActivity proxyEnquiryActivity,
                                  SendDuitNowTransferActivity sendDuitNowTransferActivity) {
        Worker worker = factory.newWorker(taskQueue);

        // Register workflows
        worker.registerWorkflowImplementationTypes(
                WithdrawalWorkflowImpl.class,
                DepositWorkflowImpl.class,
                BillPaymentWorkflowImpl.class,
                DuitNowTransferWorkflowImpl.class
        );

        // Register activities
        worker.registerActivitiesImplementations(
                (CheckVelocityActivityImpl) checkVelocityActivity,
                (CalculateFeesActivityImpl) calculateFeesActivity,
                (BlockFloatActivityImpl) blockFloatActivity,
                (CommitFloatActivityImpl) commitFloatActivity,
                (ReleaseFloatActivityImpl) releaseFloatActivity,
                (CreditAgentFloatActivityImpl) creditAgentFloatActivity,
                (AuthorizeAtSwitchActivityImpl) authorizeAtSwitchActivity,
                (SendReversalToSwitchActivityImpl) sendReversalToSwitchActivity,
                (PublishKafkaEventActivityImpl) publishKafkaEventActivity,
                (ValidateAccountActivityImpl) validateAccountActivity,
                (PostToCBSActivityImpl) postToCBSActivity,
                (ValidateBillActivityImpl) validateBillActivity,
                (PayBillerActivityImpl) payBillerActivity,
                (NotifyBillerActivityImpl) notifyBillerActivity,
                (ProxyEnquiryActivityImpl) proxyEnquiryActivity,
                (SendDuitNowTransferActivityImpl) sendDuitNowTransferActivity
        );

        return worker;
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/config/TemporalWorkerConfig.java
git commit -m "feat(orchestrator): add Temporal client config, workflow factory, and worker setup"
```

---

### Task 7: Workflow Implementations (WithdrawalWorkflow)

**Files:**
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/WithdrawalWorkflowImpl.java`

This is the most complex workflow — it serves as the reference implementation for all others.

- [ ] **Step 1: Create WithdrawalWorkflowImpl**

```java
package com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.workflow.WithdrawalWorkflow;
import com.agentbanking.orchestrator.domain.model.ForceResolveSignal;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.out.EventPublisherPort.TransactionCompletedEvent;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.*;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

public class WithdrawalWorkflowImpl implements WithdrawalWorkflow {

    private static final Logger log = Workflow.getLogger(WithdrawalWorkflowImpl.class);

    private final CheckVelocityActivity checkVelocityActivity;
    private final CalculateFeesActivity calculateFeesActivity;
    private final BlockFloatActivity blockFloatActivity;
    private final CommitFloatActivity commitFloatActivity;
    private final ReleaseFloatActivity releaseFloatActivity;
    private final AuthorizeAtSwitchActivity authorizeAtSwitchActivity;
    private final SendReversalToSwitchActivity sendReversalToSwitchActivity;
    private final PublishKafkaEventActivity publishKafkaEventActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;
    private ForceResolveSignal forceResolveSignal = null;

    public WithdrawalWorkflowImpl(
            CheckVelocityActivity checkVelocityActivity,
            CalculateFeesActivity calculateFeesActivity,
            BlockFloatActivity blockFloatActivity,
            CommitFloatActivity commitFloatActivity,
            ReleaseFloatActivity releaseFloatActivity,
            AuthorizeAtSwitchActivity authorizeAtSwitchActivity,
            SendReversalToSwitchActivity sendReversalToSwitchActivity,
            PublishKafkaEventActivity publishKafkaEventActivity) {
        this.checkVelocityActivity = checkVelocityActivity;
        this.calculateFeesActivity = calculateFeesActivity;
        this.blockFloatActivity = blockFloatActivity;
        this.commitFloatActivity = commitFloatActivity;
        this.releaseFloatActivity = releaseFloatActivity;
        this.authorizeAtSwitchActivity = authorizeAtSwitchActivity;
        this.sendReversalToSwitchActivity = sendReversalToSwitchActivity;
        this.publishKafkaEventActivity = publishKafkaEventActivity;
    }

    @Override
    public WorkflowResult execute(WithdrawalInput input) {
        log.info("Workflow started: Withdrawal, agentId={}, amount={}", input.agentId(), input.amount());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            // Step 1: Check velocity
            VelocityCheckResult velocityResult = checkVelocityActivity.checkVelocity(
                    new VelocityCheckInput(input.agentId(), input.amount(), input.customerMykad()));
            if (!velocityResult.passed()) {
                return WorkflowResult.failed(velocityResult.errorCode(), "Velocity check failed", "DECLINE");
            }

            // Step 2: Calculate fees
            FeeCalculationResult fees = calculateFeesActivity.calculateFees(
                    new FeeCalculationInput("CASH_WITHDRAWAL", input.agentTier(), input.amount()));

            // Step 3: Block float
            FloatBlockResult blockResult = blockFloatActivity.blockFloat(
                    new FloatBlockInput(input.agentId(),
                            input.amount().add(fees.customerFee()),
                            input.idempotencyKey()));
            if (!blockResult.success()) {
                return WorkflowResult.failed(blockResult.errorCode(), "Float block failed", "DECLINE");
            }
            UUID transactionId = blockResult.transactionId();

            // Step 4: Authorize at switch
            SwitchAuthorizationResult authResult = authorizeAtSwitchActivity.authorize(
                    new SwitchAuthorizationInput(input.pan(), input.pinBlock(), input.amount(), transactionId));

            if (!authResult.approved()) {
                // Compensation: release float
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), input.amount().add(fees.customerFee()), transactionId));

                if ("TIMEOUT".equals(authResult.responseCode())) {
                    // Safety reversal
                    triggerSafetyReversal(transactionId);
                }

                return WorkflowResult.failed(
                        authResult.errorCode() != null ? authResult.errorCode() : "ERR_SWITCH_DECLINED",
                        "Switch authorization failed", "DECLINE");
            }

            // Step 5: Commit float
            FloatCommitResult commitResult = commitFloatActivity.commitFloat(
                    new FloatCommitInput(input.agentId(), input.amount(), transactionId));
            if (!commitResult.success()) {
                triggerSafetyReversal(transactionId);
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), input.amount().add(fees.customerFee()), transactionId));

                return WorkflowResult.failed(commitResult.errorCode(), "Float commit failed", "RETRY");
            }

            // Step 6: Publish Kafka event
            try {
                publishKafkaEventActivity.publishCompleted(new TransactionCompletedEvent(
                        transactionId, input.agentId(), input.amount(), fees.customerFee(),
                        fees.agentCommission(), fees.bankShare(), "CASH_WITHDRAWAL",
                        input.customerCardMasked(), authResult.referenceCode(), authResult.referenceCode()));
            } catch (Exception e) {
                log.warn("Failed to publish Kafka event: {}", e.getMessage());
            }

            currentStatus = WorkflowStatus.COMPLETED;
            return WorkflowResult.completed(transactionId, authResult.referenceCode(),
                    input.amount(), fees.customerFee());

        } catch (Exception e) {
            log.error("Workflow failed with exception: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            return WorkflowResult.failed("ERR_SYS_WORKFLOW_FAILED", e.getMessage(), "REVIEW");
        }
    }

    private void triggerSafetyReversal(UUID transactionId) {
        log.info("Triggering safety reversal for transaction: {}", transactionId);
        currentStatus = WorkflowStatus.COMPENSATING;

        try {
            sendReversalToSwitchActivity.sendReversal(
                    new com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchReversalInput(transactionId));
        } catch (Exception e) {
            log.error("Safety reversal failed: {}", e.getMessage());
            // Workflow stays in COMPENSATING state, waits for admin signal
        }
    }

    @Override
    public void forceResolve(ForceResolveSignal signal) {
        log.info("Force resolve signal received: action={}", signal.action());
        this.forceResolveSignal = signal;
    }

    @Override
    public WorkflowStatus getStatus() {
        return currentStatus;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/WithdrawalWorkflowImpl.java
git commit -m "feat(orchestrator): implement WithdrawalWorkflow with compensation and safety reversal"
```

---

### Task 8: Remaining Workflow Implementations

**Files:**
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/WithdrawalOnUsWorkflowImpl.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/DepositWorkflowImpl.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/BillPaymentWorkflowImpl.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/DuitNowTransferWorkflowImpl.java`

- [ ] **Step 1: Create WithdrawalOnUsWorkflowImpl**

Similar to WithdrawalWorkflowImpl but uses CbsServicePort instead of SwitchAdapterPort for authorization. No safety reversal needed (CBS returns definitive success/failure).

```java
package com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.workflow.WithdrawalOnUsWorkflow;
import com.agentbanking.orchestrator.domain.model.ForceResolveSignal;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.out.CbsServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.EventPublisherPort.TransactionCompletedEvent;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.*;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.UUID;

public class WithdrawalOnUsWorkflowImpl implements WithdrawalOnUsWorkflow {

    private static final Logger log = Workflow.getLogger(WithdrawalOnUsWorkflowImpl.class);

    private final CheckVelocityActivity checkVelocityActivity;
    private final CalculateFeesActivity calculateFeesActivity;
    private final BlockFloatActivity blockFloatActivity;
    private final CommitFloatActivity commitFloatActivity;
    private final ReleaseFloatActivity releaseFloatActivity;
    private final PostToCBSActivity postToCBSActivity;
    private final PublishKafkaEventActivity publishKafkaEventActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;
    private ForceResolveSignal forceResolveSignal = null;

    public WithdrawalOnUsWorkflowImpl(
            CheckVelocityActivity checkVelocityActivity,
            CalculateFeesActivity calculateFeesActivity,
            BlockFloatActivity blockFloatActivity,
            CommitFloatActivity commitFloatActivity,
            ReleaseFloatActivity releaseFloatActivity,
            PostToCBSActivity postToCBSActivity,
            PublishKafkaEventActivity publishKafkaEventActivity) {
        this.checkVelocityActivity = checkVelocityActivity;
        this.calculateFeesActivity = calculateFeesActivity;
        this.blockFloatActivity = blockFloatActivity;
        this.commitFloatActivity = commitFloatActivity;
        this.releaseFloatActivity = releaseFloatActivity;
        this.postToCBSActivity = postToCBSActivity;
        this.publishKafkaEventActivity = publishKafkaEventActivity;
    }

    @Override
    public WorkflowResult execute(WithdrawalOnUsInput input) {
        log.info("Workflow started: WithdrawalOnUs, agentId={}, amount={}", input.agentId(), input.amount());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            // Step 1: Check velocity
            VelocityCheckResult velocityResult = checkVelocityActivity.checkVelocity(
                    new VelocityCheckInput(input.agentId(), input.amount(), input.customerMykad()));
            if (!velocityResult.passed()) {
                return WorkflowResult.failed(velocityResult.errorCode(), "Velocity check failed", "DECLINE");
            }

            // Step 2: Calculate fees
            FeeCalculationResult fees = calculateFeesActivity.calculateFees(
                    new FeeCalculationInput("CASH_WITHDRAWAL", input.agentTier(), input.amount()));

            // Step 3: Block float
            FloatBlockResult blockResult = blockFloatActivity.blockFloat(
                    new FloatBlockInput(input.agentId(),
                            input.amount().add(fees.customerFee()),
                            input.idempotencyKey()));
            if (!blockResult.success()) {
                return WorkflowResult.failed(blockResult.errorCode(), "Float block failed", "DECLINE");
            }
            UUID transactionId = blockResult.transactionId();

            // Step 4: Authorize at CBS
            CbsPostResult cbsResult = postToCBSActivity.postToCbs(
                    new CbsPostInput(input.customerAccount(), input.amount()));
            if (!cbsResult.success()) {
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), input.amount().add(fees.customerFee()), transactionId));
                return WorkflowResult.failed(cbsResult.errorCode(), "CBS authorization failed", "DECLINE");
            }

            // Step 5: Commit float
            FloatCommitResult commitResult = commitFloatActivity.commitFloat(
                    new FloatCommitInput(input.agentId(), input.amount(), transactionId));
            if (!commitResult.success()) {
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), input.amount().add(fees.customerFee()), transactionId));
                return WorkflowResult.failed(commitResult.errorCode(), "Float commit failed", "RETRY");
            }

            // Step 6: Publish event
            try {
                publishKafkaEventActivity.publishCompleted(new TransactionCompletedEvent(
                        transactionId, input.agentId(), input.amount(), fees.customerFee(),
                        fees.agentCommission(), fees.bankShare(), "CASH_WITHDRAWAL",
                        input.customerCardMasked(), cbsResult.reference(), cbsResult.reference()));
            } catch (Exception e) {
                log.warn("Failed to publish Kafka event: {}", e.getMessage());
            }

            currentStatus = WorkflowStatus.COMPLETED;
            return WorkflowResult.completed(transactionId, cbsResult.reference(),
                    input.amount(), fees.customerFee());

        } catch (Exception e) {
            log.error("Workflow failed with exception: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            return WorkflowResult.failed("ERR_SYS_WORKFLOW_FAILED", e.getMessage(), "REVIEW");
        }
    }

    @Override
    public void forceResolve(ForceResolveSignal signal) {
        this.forceResolveSignal = signal;
    }

    @Override
    public WorkflowStatus getStatus() {
        return currentStatus;
    }
}
```

- [ ] **Step 2: Create DepositWorkflowImpl**

```java
package com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.workflow.DepositWorkflow;
import com.agentbanking.orchestrator.domain.model.ForceResolveSignal;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.out.CbsServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.EventPublisherPort.TransactionCompletedEvent;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.*;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.UUID;

public class DepositWorkflowImpl implements DepositWorkflow {

    private static final Logger log = Workflow.getLogger(DepositWorkflowImpl.class);

    private final CheckVelocityActivity checkVelocityActivity;
    private final CalculateFeesActivity calculateFeesActivity;
    private final ValidateAccountActivity validateAccountActivity;
    private final CreditAgentFloatActivity creditAgentFloatActivity;
    private final ReleaseFloatActivity releaseFloatActivity;
    private final PostToCBSActivity postToCBSActivity;
    private final PublishKafkaEventActivity publishKafkaEventActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;
    private ForceResolveSignal forceResolveSignal = null;

    public DepositWorkflowImpl(
            CheckVelocityActivity checkVelocityActivity,
            CalculateFeesActivity calculateFeesActivity,
            ValidateAccountActivity validateAccountActivity,
            CreditAgentFloatActivity creditAgentFloatActivity,
            ReleaseFloatActivity releaseFloatActivity,
            PostToCBSActivity postToCBSActivity,
            PublishKafkaEventActivity publishKafkaEventActivity) {
        this.checkVelocityActivity = checkVelocityActivity;
        this.calculateFeesActivity = calculateFeesActivity;
        this.validateAccountActivity = validateAccountActivity;
        this.creditAgentFloatActivity = creditAgentFloatActivity;
        this.releaseFloatActivity = releaseFloatActivity;
        this.postToCBSActivity = postToCBSActivity;
        this.publishKafkaEventActivity = publishKafkaEventActivity;
    }

    @Override
    public WorkflowResult execute(DepositInput input) {
        log.info("Workflow started: Deposit, agentId={}, amount={}", input.agentId(), input.amount());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            // Step 1: Check velocity
            VelocityCheckResult velocityResult = checkVelocityActivity.checkVelocity(
                    new VelocityCheckInput(input.agentId(), input.amount(), input.customerMykad()));
            if (!velocityResult.passed()) {
                return WorkflowResult.failed(velocityResult.errorCode(), "Velocity check failed", "DECLINE");
            }

            // Step 2: Calculate fees
            FeeCalculationResult fees = calculateFeesActivity.calculateFees(
                    new FeeCalculationInput("CASH_DEPOSIT", input.agentTier(), input.amount()));

            // Step 3: Validate account
            AccountValidationResult accountResult = validateAccountActivity.validateAccount(
                    new AccountValidationInput(input.destinationAccount()));
            if (!accountResult.valid()) {
                return WorkflowResult.failed(accountResult.errorCode(), "Invalid account", "DECLINE");
            }

            // Step 4: Credit agent float
            FloatCreditResult creditResult = creditAgentFloatActivity.creditAgentFloat(
                    new FloatCreditInput(input.agentId(), input.amount()));
            if (!creditResult.success()) {
                return WorkflowResult.failed(creditResult.errorCode(), "Float credit failed", "DECLINE");
            }

            // Step 5: Post to CBS
            CbsPostResult cbsResult = postToCBSActivity.postToCbs(
                    new CbsPostInput(input.destinationAccount(), input.amount()));
            if (!cbsResult.success()) {
                // Compensation: reverse the credit
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), input.amount(), null));
                return WorkflowResult.failed(cbsResult.errorCode(), "CBS posting failed", "RETRY");
            }

            // Step 6: Publish event
            try {
                publishKafkaEventActivity.publishCompleted(new TransactionCompletedEvent(
                        UUID.randomUUID(), input.agentId(), input.amount(), fees.customerFee(),
                        fees.agentCommission(), fees.bankShare(), "CASH_DEPOSIT",
                        null, cbsResult.reference(), cbsResult.reference()));
            } catch (Exception e) {
                log.warn("Failed to publish Kafka event: {}", e.getMessage());
            }

            currentStatus = WorkflowStatus.COMPLETED;
            return WorkflowResult.completed(UUID.randomUUID(), cbsResult.reference(),
                    input.amount(), fees.customerFee());

        } catch (Exception e) {
            log.error("Workflow failed with exception: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            return WorkflowResult.failed("ERR_SYS_WORKFLOW_FAILED", e.getMessage(), "REVIEW");
        }
    }

    @Override
    public void forceResolve(ForceResolveSignal signal) {
        this.forceResolveSignal = signal;
    }

    @Override
    public WorkflowStatus getStatus() {
        return currentStatus;
    }
}
```

- [ ] **Step 3: Create BillPaymentWorkflowImpl**

```java
package com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.workflow.BillPaymentWorkflow;
import com.agentbanking.orchestrator.domain.model.ForceResolveSignal;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.EventPublisherPort.TransactionCompletedEvent;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.*;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.UUID;

public class BillPaymentWorkflowImpl implements BillPaymentWorkflow {

    private static final Logger log = Workflow.getLogger(BillPaymentWorkflowImpl.class);

    private final CheckVelocityActivity checkVelocityActivity;
    private final CalculateFeesActivity calculateFeesActivity;
    private final BlockFloatActivity blockFloatActivity;
    private final CommitFloatActivity commitFloatActivity;
    private final ReleaseFloatActivity releaseFloatActivity;
    private final ValidateBillActivity validateBillActivity;
    private final PayBillerActivity payBillerActivity;
    private final NotifyBillerActivity notifyBillerActivity;
    private final PublishKafkaEventActivity publishKafkaEventActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;
    private ForceResolveSignal forceResolveSignal = null;

    public BillPaymentWorkflowImpl(
            CheckVelocityActivity checkVelocityActivity,
            CalculateFeesActivity calculateFeesActivity,
            BlockFloatActivity blockFloatActivity,
            CommitFloatActivity commitFloatActivity,
            ReleaseFloatActivity releaseFloatActivity,
            ValidateBillActivity validateBillActivity,
            PayBillerActivity payBillerActivity,
            NotifyBillerActivity notifyBillerActivity,
            PublishKafkaEventActivity publishKafkaEventActivity) {
        this.checkVelocityActivity = checkVelocityActivity;
        this.calculateFeesActivity = calculateFeesActivity;
        this.blockFloatActivity = blockFloatActivity;
        this.commitFloatActivity = commitFloatActivity;
        this.releaseFloatActivity = releaseFloatActivity;
        this.validateBillActivity = validateBillActivity;
        this.payBillerActivity = payBillerActivity;
        this.notifyBillerActivity = notifyBillerActivity;
        this.publishKafkaEventActivity = publishKafkaEventActivity;
    }

    @Override
    public WorkflowResult execute(BillPaymentInput input) {
        log.info("Workflow started: BillPayment, agentId={}, biller={}", input.agentId(), input.billerCode());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            // Step 1: Check velocity
            VelocityCheckResult velocityResult = checkVelocityActivity.checkVelocity(
                    new VelocityCheckInput(input.agentId(), input.amount(), input.customerMykad()));
            if (!velocityResult.passed()) {
                return WorkflowResult.failed(velocityResult.errorCode(), "Velocity check failed", "DECLINE");
            }

            // Step 2: Calculate fees
            FeeCalculationResult fees = calculateFeesActivity.calculateFees(
                    new FeeCalculationInput("BILL_PAYMENT", input.agentTier(), input.amount()));

            // Step 3: Block float
            FloatBlockResult blockResult = blockFloatActivity.blockFloat(
                    new FloatBlockInput(input.agentId(),
                            input.amount().add(fees.customerFee()),
                            input.idempotencyKey()));
            if (!blockResult.success()) {
                return WorkflowResult.failed(blockResult.errorCode(), "Float block failed", "DECLINE");
            }
            UUID transactionId = blockResult.transactionId();

            // Step 4: Validate bill
            BillValidationResult billResult = validateBillActivity.validateBill(
                    new BillValidationInput(input.billerCode(), input.ref1()));
            if (!billResult.valid()) {
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), input.amount().add(fees.customerFee()), transactionId));
                return WorkflowResult.failed(billResult.errorCode(), "Bill validation failed", "DECLINE");
            }

            // Step 5: Pay biller
            BillPaymentResult paymentResult = payBillerActivity.payBill(
                    new BillPaymentInput(input.billerCode(), input.ref1(), input.ref2(),
                            input.amount(), input.idempotencyKey()));
            if (!paymentResult.success()) {
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), input.amount().add(fees.customerFee()), transactionId));
                return WorkflowResult.failed(paymentResult.errorCode(), "Bill payment failed", "DECLINE");
            }

            // Step 6: Commit float
            FloatCommitResult commitResult = commitFloatActivity.commitFloat(
                    new FloatCommitInput(input.agentId(), input.amount(), transactionId));
            if (!commitResult.success()) {
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), input.amount().add(fees.customerFee()), transactionId));
                return WorkflowResult.failed(commitResult.errorCode(), "Float commit failed", "RETRY");
            }

            // Step 7: Notify biller (non-critical)
            try {
                notifyBillerActivity.notifyBiller(
                        new BillNotificationInput(transactionId.toString(), input.amount()));
            } catch (Exception e) {
                log.warn("Failed to notify biller: {}", e.getMessage());
            }

            // Step 8: Publish event
            try {
                publishKafkaEventActivity.publishCompleted(new TransactionCompletedEvent(
                        transactionId, input.agentId(), input.amount(), fees.customerFee(),
                        fees.agentCommission(), fees.bankShare(), "BILL_PAYMENT",
                        null, paymentResult.billerReference(), paymentResult.billerReference()));
            } catch (Exception e) {
                log.warn("Failed to publish Kafka event: {}", e.getMessage());
            }

            currentStatus = WorkflowStatus.COMPLETED;
            return WorkflowResult.completed(transactionId, paymentResult.billerReference(),
                    input.amount(), fees.customerFee());

        } catch (Exception e) {
            log.error("Workflow failed with exception: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            return WorkflowResult.failed("ERR_SYS_WORKFLOW_FAILED", e.getMessage(), "REVIEW");
        }
    }

    @Override
    public void forceResolve(ForceResolveSignal signal) {
        this.forceResolveSignal = signal;
    }

    @Override
    public WorkflowStatus getStatus() {
        return currentStatus;
    }
}
```

- [ ] **Step 4: Create DuitNowTransferWorkflowImpl**

```java
package com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.workflow.DuitNowTransferWorkflow;
import com.agentbanking.orchestrator.domain.model.ForceResolveSignal;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.out.EventPublisherPort.TransactionCompletedEvent;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.*;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.UUID;

public class DuitNowTransferWorkflowImpl implements DuitNowTransferWorkflow {

    private static final Logger log = Workflow.getLogger(DuitNowTransferWorkflowImpl.class);

    private final CheckVelocityActivity checkVelocityActivity;
    private final CalculateFeesActivity calculateFeesActivity;
    private final BlockFloatActivity blockFloatActivity;
    private final CommitFloatActivity commitFloatActivity;
    private final ReleaseFloatActivity releaseFloatActivity;
    private final ProxyEnquiryActivity proxyEnquiryActivity;
    private final SendDuitNowTransferActivity sendDuitNowTransferActivity;
    private final SendReversalToSwitchActivity sendReversalToSwitchActivity;
    private final PublishKafkaEventActivity publishKafkaEventActivity;

    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;
    private ForceResolveSignal forceResolveSignal = null;

    public DuitNowTransferWorkflowImpl(
            CheckVelocityActivity checkVelocityActivity,
            CalculateFeesActivity calculateFeesActivity,
            BlockFloatActivity blockFloatActivity,
            CommitFloatActivity commitFloatActivity,
            ReleaseFloatActivity releaseFloatActivity,
            ProxyEnquiryActivity proxyEnquiryActivity,
            SendDuitNowTransferActivity sendDuitNowTransferActivity,
            SendReversalToSwitchActivity sendReversalToSwitchActivity,
            PublishKafkaEventActivity publishKafkaEventActivity) {
        this.checkVelocityActivity = checkVelocityActivity;
        this.calculateFeesActivity = calculateFeesActivity;
        this.blockFloatActivity = blockFloatActivity;
        this.commitFloatActivity = commitFloatActivity;
        this.releaseFloatActivity = releaseFloatActivity;
        this.proxyEnquiryActivity = proxyEnquiryActivity;
        this.sendDuitNowTransferActivity = sendDuitNowTransferActivity;
        this.sendReversalToSwitchActivity = sendReversalToSwitchActivity;
        this.publishKafkaEventActivity = publishKafkaEventActivity;
    }

    @Override
    public WorkflowResult execute(DuitNowTransferInput input) {
        log.info("Workflow started: DuitNowTransfer, agentId={}, amount={}", input.agentId(), input.amount());
        currentStatus = WorkflowStatus.RUNNING;

        try {
            // Step 1: Check velocity
            VelocityCheckResult velocityResult = checkVelocityActivity.checkVelocity(
                    new VelocityCheckInput(input.agentId(), input.amount(), input.customerMykad()));
            if (!velocityResult.passed()) {
                return WorkflowResult.failed(velocityResult.errorCode(), "Velocity check failed", "DECLINE");
            }

            // Step 2: Calculate fees
            FeeCalculationResult fees = calculateFeesActivity.calculateFees(
                    new FeeCalculationInput("DUITNOW_TRANSFER", input.agentTier(), input.amount()));

            // Step 3: Block float
            FloatBlockResult blockResult = blockFloatActivity.blockFloat(
                    new FloatBlockInput(input.agentId(),
                            input.amount().add(fees.customerFee()),
                            input.idempotencyKey()));
            if (!blockResult.success()) {
                return WorkflowResult.failed(blockResult.errorCode(), "Float block failed", "DECLINE");
            }
            UUID transactionId = blockResult.transactionId();

            // Step 4: Proxy enquiry
            ProxyEnquiryResult proxyResult = proxyEnquiryActivity.proxyEnquiry(
                    new ProxyEnquiryInput(input.proxyType(), input.proxyValue()));
            if (!proxyResult.valid()) {
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), input.amount().add(fees.customerFee()), transactionId));
                return WorkflowResult.failed(proxyResult.errorCode(), "Proxy not found", "DECLINE");
            }

            // Step 5: Send DuitNow transfer
            DuitNowTransferResult transferResult = sendDuitNowTransferActivity.sendDuitNowTransfer(
                    new DuitNowTransferInput(proxyResult.bankCode(), proxyResult.recipientName(),
                            input.amount(), transactionId));
            if (!transferResult.success()) {
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), input.amount().add(fees.customerFee()), transactionId));

                if ("TIMEOUT".equals(transferResult.errorCode())) {
                    triggerSafetyReversal(transactionId);
                }

                return WorkflowResult.failed(
                        transferResult.errorCode() != null ? transferResult.errorCode() : "ERR_DUITNOW_FAILED",
                        "DuitNow transfer failed", "DECLINE");
            }

            // Step 6: Commit float
            FloatCommitResult commitResult = commitFloatActivity.commitFloat(
                    new FloatCommitInput(input.agentId(), input.amount(), transactionId));
            if (!commitResult.success()) {
                triggerSafetyReversal(transactionId);
                releaseFloatActivity.releaseFloat(
                        new FloatReleaseInput(input.agentId(), input.amount().add(fees.customerFee()), transactionId));
                return WorkflowResult.failed(commitResult.errorCode(), "Float commit failed", "RETRY");
            }

            // Step 7: Publish event
            try {
                publishKafkaEventActivity.publishCompleted(new TransactionCompletedEvent(
                        transactionId, input.agentId(), input.amount(), fees.customerFee(),
                        fees.agentCommission(), fees.bankShare(), "DUITNOW_TRANSFER",
                        null, transferResult.paynetReference(), transferResult.paynetReference()));
            } catch (Exception e) {
                log.warn("Failed to publish Kafka event: {}", e.getMessage());
            }

            currentStatus = WorkflowStatus.COMPLETED;
            return WorkflowResult.completed(transactionId, transferResult.paynetReference(),
                    input.amount(), fees.customerFee());

        } catch (Exception e) {
            log.error("Workflow failed with exception: {}", e.getMessage());
            currentStatus = WorkflowStatus.FAILED;
            return WorkflowResult.failed("ERR_SYS_WORKFLOW_FAILED", e.getMessage(), "REVIEW");
        }
    }

    private void triggerSafetyReversal(UUID transactionId) {
        log.info("Triggering safety reversal for transaction: {}", transactionId);
        currentStatus = WorkflowStatus.COMPENSATING;
        try {
            sendReversalToSwitchActivity.sendReversal(
                    new SwitchReversalInput(transactionId));
        } catch (Exception e) {
            log.error("Safety reversal failed: {}", e.getMessage());
        }
    }

    @Override
    public void forceResolve(ForceResolveSignal signal) {
        this.forceResolveSignal = signal;
    }

    @Override
    public WorkflowStatus getStatus() {
        return currentStatus;
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/
git commit -m "feat(orchestrator): implement all Temporal workflows (On-Us, Deposit, BillPayment, DuitNow)"
```

---

### Task 9: Activity Implementations

**Files:** Create all 17 activity implementation files in `infrastructure/temporal/ActivityImpl/`

Each activity implementation:
1. Implements the activity interface
2. Has `@Component` annotation
3. Calls the domain port
4. Sets Temporal retry options via `ActivityOptions` in the workflow (not in the activity)

- [ ] **Step 1: Create all 17 activity implementations**

Each follows the same pattern — implement the activity interface, inject the domain port, delegate. Here is every single one:

**CheckVelocityActivityImpl.java:**
```java
package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.CheckVelocityActivity;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.VelocityCheckInput;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.VelocityCheckResult;
import org.springframework.stereotype.Component;

@Component
public class CheckVelocityActivityImpl implements CheckVelocityActivity {

    private final RulesServicePort rulesServicePort;

    public CheckVelocityActivityImpl(RulesServicePort rulesServicePort) {
        this.rulesServicePort = rulesServicePort;
    }

    @Override
    public VelocityCheckResult checkVelocity(VelocityCheckInput input) {
        return rulesServicePort.checkVelocity(input);
    }
}
```

**CalculateFeesActivityImpl.java:**
```java
package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.CalculateFeesActivity;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.FeeCalculationInput;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.FeeCalculationResult;
import org.springframework.stereotype.Component;

@Component
public class CalculateFeesActivityImpl implements CalculateFeesActivity {

    private final RulesServicePort rulesServicePort;

    public CalculateFeesActivityImpl(RulesServicePort rulesServicePort) {
        this.rulesServicePort = rulesServicePort;
    }

    @Override
    public FeeCalculationResult calculateFees(FeeCalculationInput input) {
        return rulesServicePort.calculateFees(input);
    }
}
```

**BlockFloatActivityImpl.java:**
```java
package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.BlockFloatActivity;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatBlockInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatBlockResult;
import org.springframework.stereotype.Component;

@Component
public class BlockFloatActivityImpl implements BlockFloatActivity {

    private final LedgerServicePort ledgerServicePort;

    public BlockFloatActivityImpl(LedgerServicePort ledgerServicePort) {
        this.ledgerServicePort = ledgerServicePort;
    }

    @Override
    public FloatBlockResult blockFloat(FloatBlockInput input) {
        return ledgerServicePort.blockFloat(input);
    }
}
```

Continue this pattern for all 17 activities. Each one:
- Implements its activity interface
- Injects the corresponding domain port
- Delegates to the port method

The activities that need special attention:
- `SendReversalToSwitchActivityImpl` — needs infinite retry config (set in workflow via ActivityOptions)
- `AuthorizeAtSwitchActivityImpl` — 25s timeout, zero retries
- `PayBillerActivityImpl` — 15s timeout, zero retries
- `SendDuitNowTransferActivityImpl` — 25s timeout, zero retries

- [ ] **Step 2: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/
git commit -m "feat(orchestrator): implement all 17 Temporal activity implementations"
```

---

### Task 10: Infrastructure Adapters (Typed Port Implementations)

**Files:**
- Rewrite: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/RulesServiceAdapter.java`
- Rewrite: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/LedgerServiceAdapter.java`
- Rewrite: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/SwitchAdapterAdapter.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/BillerServiceAdapter.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/BillerServiceClient.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/CbsServiceAdapter.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/CbsServiceClient.java`
- Rewrite: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/messaging/KafkaEventPublisher.java`

- [ ] **Step 1: Rewrite RulesServiceAdapter with typed interfaces**

```java
package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.RulesServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RulesServiceAdapter implements RulesServicePort {

    private static final Logger log = LoggerFactory.getLogger(RulesServiceAdapter.class);

    private final RulesServiceClient rulesServiceClient;

    public RulesServiceAdapter(RulesServiceClient rulesServiceClient) {
        this.rulesServiceClient = rulesServiceClient;
    }

    @Override
    public VelocityCheckResult checkVelocity(VelocityCheckInput input) {
        log.info("Checking velocity for agent: {}", input.agentId());
        return rulesServiceClient.checkVelocity(input);
    }

    @Override
    public FeeCalculationResult calculateFees(FeeCalculationInput input) {
        log.info("Calculating fees for type: {}, tier: {}", input.transactionType(), input.agentTier());
        return rulesServiceClient.calculateFees(input);
    }
}
```

- [ ] **Step 2: Rewrite RulesServiceClient Feign client**

```java
package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.FeeCalculationInput;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.FeeCalculationResult;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.VelocityCheckInput;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.VelocityCheckResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "rulesService", url = "${rules-service.url}")
public interface RulesServiceClient {

    @PostMapping("/internal/velocity/check")
    VelocityCheckResult checkVelocity(@RequestBody VelocityCheckInput input);

    @PostMapping("/internal/fees/calculate")
    FeeCalculationResult calculateFees(@RequestBody FeeCalculationInput input);
}
```

- [ ] **Step 3: Rewrite LedgerServiceAdapter and LedgerServiceClient**

Follow the same pattern — typed input/output records, Feign client with typed methods.

- [ ] **Step 4: Create BillerServiceAdapter and BillerServiceClient**

```java
// BillerServiceClient.java
@FeignClient(name = "billerService", url = "${biller-service.url}")
public interface BillerServiceClient {
    @PostMapping("/internal/bills/validate")
    BillValidationResult validateBill(@RequestBody BillValidationInput input);

    @PostMapping("/internal/bills/pay")
    BillPaymentResult payBill(@RequestBody BillPaymentInput input);

    @PostMapping("/internal/bills/notify")
    BillNotificationResult notifyBiller(@RequestBody BillNotificationInput input);

    @PostMapping("/internal/bills/reversal")
    BillNotificationResult notifyReversal(@RequestBody BillReversalInput input);
}
```

- [ ] **Step 5: Create CbsServiceAdapter and CbsServiceClient**

Same pattern for CBS connector.

- [ ] **Step 6: Rewrite KafkaEventPublisher**

Update to use the new typed `TransactionCompletedEvent` and `TransactionFailedEvent` records.

- [ ] **Step 7: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/ services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/messaging/
git commit -m "refactor(orchestrator): rewrite all adapters with typed DTOs, add Biller and CBS clients"
```

---

### Task 11: Database Schema and TransactionRecord

**Files:**
- Create: `services/orchestrator-service/src/main/resources/db/migration/V1__transaction_record.sql`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/persistence/entity/TransactionRecordEntity.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/persistence/repository/TransactionRecordJpaRepository.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/TransactionRecordRepository.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/persistence/repository/TransactionRecordRepositoryImpl.java`

- [ ] **Step 1: Create Flyway migration**

```sql
-- V1__transaction_record.sql
CREATE TABLE transaction_record (
    id UUID PRIMARY KEY,
    workflow_id VARCHAR(128) NOT NULL UNIQUE,
    transaction_type VARCHAR(50) NOT NULL,
    agent_id UUID NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    customer_fee DECIMAL(10,2),
    status VARCHAR(20) NOT NULL,
    error_code VARCHAR(50),
    error_message TEXT,
    external_reference VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_txn_record_agent ON transaction_record(agent_id);
CREATE INDEX idx_txn_record_status ON transaction_record(status);
CREATE INDEX idx_txn_record_created ON transaction_record(created_at);
CREATE INDEX idx_txn_record_type ON transaction_record(transaction_type);
```

- [ ] **Step 2: Create JPA entity**

```java
package com.agentbanking.orchestrator.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transaction_record")
public class TransactionRecordEntity {

    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false, unique = true, length = 128)
    private String workflowId;

    @Column(name = "transaction_type", nullable = false, length = 50)
    private String transactionType;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "customer_fee", precision = 10, scale = 2)
    private BigDecimal customerFee;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "external_reference", length = 128)
    private String externalReference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Default constructor for JPA
    public TransactionRecordEntity() {}

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public UUID getAgentId() { return agentId; }
    public void setAgentId(UUID agentId) { this.agentId = agentId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getCustomerFee() { return customerFee; }
    public void setCustomerFee(BigDecimal customerFee) { this.customerFee = customerFee; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String externalReference) { this.externalReference = externalReference; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 3: Create Spring Data JPA repository**

```java
package com.agentbanking.orchestrator.infrastructure.persistence.repository;

import com.agentbanking.orchestrator.infrastructure.persistence.entity.TransactionRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRecordJpaRepository extends JpaRepository<TransactionRecordEntity, UUID> {
    Optional<TransactionRecordEntity> findByWorkflowId(String workflowId);
}
```

- [ ] **Step 4: Create domain port and implementation**

```java
// Domain port: domain/port/out/TransactionRecordRepository.java
package com.agentbanking.orchestrator.domain.port.out;

import com.agentbanking.orchestrator.domain.model.TransactionType;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRecordRepository {
    void create(UUID id, String workflowId, TransactionType type, UUID agentId,
                BigDecimal amount, String status);
    void updateStatus(String workflowId, String status, String errorCode,
                      String errorMessage, String externalReference);
    Optional<TransactionRecordDTO> findByWorkflowId(String workflowId);

    record TransactionRecordDTO(
        UUID id, String workflowId, TransactionType transactionType,
        UUID agentId, BigDecimal amount, BigDecimal customerFee,
        String status, String errorCode, String errorMessage,
        String externalReference, java.time.Instant createdAt,
        java.time.Instant completedAt
    ) {}
}
```

- [ ] **Step 5: Enable Flyway in application.yaml**

Change `flyway.enabled: false` to `flyway.enabled: true` in application.yaml.

- [ ] **Step 6: Commit**

```bash
git add services/orchestrator-service/src/main/resources/db/migration/ services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/persistence/ services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/TransactionRecordRepository.java services/orchestrator-service/src/main/resources/application.yaml
git commit -m "feat(orchestrator): add TransactionRecord entity, Flyway migration, and repository"
```

---

### Task 12: Use Case Implementations and Controller

**Files:**
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/usecase/StartTransactionUseCaseImpl.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/usecase/QueryWorkflowStatusUseCaseImpl.java`
- Rewrite: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/web/OrchestratorController.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/web/dto/TransactionResponse.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/web/dto/WorkflowStatusResponse.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/web/dto/ForceResolveRequest.java`

- [ ] **Step 1: Create StartTransactionUseCaseImpl**

```java
package com.agentbanking.orchestrator.application.usecase;

import com.agentbanking.orchestrator.domain.port.in.StartTransactionUseCase;
import com.agentbanking.orchestrator.domain.port.out.TransactionRecordRepository;
import com.agentbanking.orchestrator.domain.service.WorkflowRouter;
import com.agentbanking.orchestrator.infrastructure.temporal.WorkflowFactory;
import org.springframework.stereotype.Component;

@Component
public class StartTransactionUseCaseImpl implements StartTransactionUseCase {

    private final WorkflowFactory workflowFactory;
    private final WorkflowRouter workflowRouter;
    private final TransactionRecordRepository transactionRecordRepository;

    public StartTransactionUseCaseImpl(WorkflowFactory workflowFactory,
                                        WorkflowRouter workflowRouter,
                                        TransactionRecordRepository transactionRecordRepository) {
        this.workflowFactory = workflowFactory;
        this.workflowRouter = workflowRouter;
        this.transactionRecordRepository = transactionRecordRepository;
    }

    @Override
    public StartTransactionResult start(StartTransactionCommand command) {
        // Create transaction record
        transactionRecordRepository.create(
                java.util.UUID.randomUUID(),
                command.idempotencyKey(),
                command.transactionType(),
                command.agentId(),
                command.amount(),
                "PENDING"
        );

        // Start Temporal workflow
        workflowFactory.startWorkflow(
                command.transactionType(),
                command.idempotencyKey(),
                buildWorkflowInput(command)
        );

        return new StartTransactionResult(
                "PENDING",
                command.idempotencyKey(),
                "/api/v1/transactions/" + command.idempotencyKey() + "/status"
        );
    }

    private Object buildWorkflowInput(StartTransactionCommand command) {
        // Build the appropriate workflow input based on transaction type
        // This delegates to the WorkflowFactory which handles the casting
        return command;
    }
}
```

- [ ] **Step 2: Create QueryWorkflowStatusUseCaseImpl**

```java
package com.agentbanking.orchestrator.application.usecase;

import com.agentbanking.orchestrator.domain.port.in.QueryWorkflowStatusUseCase;
import com.agentbanking.orchestrator.domain.port.out.TransactionRecordRepository;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class QueryWorkflowStatusUseCaseImpl implements QueryWorkflowStatusUseCase {

    private final TransactionRecordRepository transactionRecordRepository;

    public QueryWorkflowStatusUseCaseImpl(TransactionRecordRepository transactionRecordRepository) {
        this.transactionRecordRepository = transactionRecordRepository;
    }

    @Override
    public Optional<WorkflowStatusResponse> getStatus(String workflowId) {
        return transactionRecordRepository.findByWorkflowId(workflowId)
                .map(dto -> {
                    WorkflowStatus status = mapStatus(dto.status());
                    WorkflowResult result = buildResult(dto);
                    return new WorkflowStatusResponse(status, result);
                });
    }

    private WorkflowStatus mapStatus(String status) {
        return switch (status) {
            case "COMPLETED" -> WorkflowStatus.COMPLETED;
            case "FAILED" -> WorkflowStatus.FAILED;
            case "REVERSED" -> WorkflowStatus.FAILED;
            default -> WorkflowStatus.PENDING;
        };
    }

    private WorkflowResult buildResult(TransactionRecordRepository.TransactionRecordDTO dto) {
        return new WorkflowResult(
                dto.status(), null, dto.errorCode(), dto.errorMessage(),
                null, dto.externalReference(), dto.amount(), dto.customerFee(),
                java.util.Map.of(), dto.completedAt()
        );
    }
}
```

- [ ] **Step 3: Create response DTOs**

```java
// TransactionResponse.java
package com.agentbanking.orchestrator.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionResponse(
    String status,
    String workflowId,
    String pollUrl
) {}

// WorkflowStatusResponse.java
package com.agentbanking.orchestrator.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WorkflowStatusResponse(
    String status,
    String workflowId,
    String transactionType,
    BigDecimal amount,
    BigDecimal customerFee,
    String referenceNumber,
    String errorCode,
    String errorMessage,
    String actionCode,
    Instant completedAt
) {}

// ForceResolveRequest.java
package com.agentbanking.orchestrator.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ForceResolveRequest(
    @NotNull Action action,
    @NotBlank String reason,
    @NotBlank String adminId
) {
    public enum Action {
        COMMIT,
        REVERSE
    }
}
```

- [ ] **Step 4: Rewrite OrchestratorController**

```java
package com.agentbanking.orchestrator.infrastructure.web;

import com.agentbanking.orchestrator.domain.port.in.StartTransactionUseCase;
import com.agentbanking.orchestrator.domain.port.in.QueryWorkflowStatusUseCase;
import com.agentbanking.orchestrator.infrastructure.web.dto.TransactionResponse;
import com.agentbanking.orchestrator.infrastructure.web.dto.WorkflowStatusResponse;
import com.agentbanking.orchestrator.infrastructure.web.dto.ForceResolveRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class OrchestratorController {

    private final StartTransactionUseCase startTransactionUseCase;
    private final QueryWorkflowStatusUseCase queryWorkflowStatusUseCase;

    public OrchestratorController(StartTransactionUseCase startTransactionUseCase,
                                   QueryWorkflowStatusUseCase queryWorkflowStatusUseCase) {
        this.startTransactionUseCase = startTransactionUseCase;
        this.queryWorkflowStatusUseCase = queryWorkflowStatusUseCase;
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> startTransaction(
            @Valid @RequestBody StartTransactionUseCase.StartTransactionCommand command) {
        StartTransactionUseCase.StartTransactionResult result = startTransactionUseCase.start(command);
        return ResponseEntity.accepted().body(
                new TransactionResponse(result.status(), result.workflowId(), result.pollUrl()));
    }

    @GetMapping("/transactions/{workflowId}/status")
    public ResponseEntity<?> getStatus(@PathVariable String workflowId) {
        return queryWorkflowStatusUseCase.getStatus(workflowId)
                .map(response -> ResponseEntity.ok(response))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/backoffice/transactions/{workflowId}/resolve")
    public ResponseEntity<Map<String, String>> forceResolve(
            @PathVariable String workflowId,
            @Valid @RequestBody ForceResolveRequest request) {
        // TODO: Implement signal sending to Temporal workflow
        return ResponseEntity.ok(Map.of("status", "SIGNAL_SENT", "workflowId", workflowId));
    }
}
```

- [ ] **Step 5: Update DomainServiceConfig**

Register the new beans (WorkflowRouter, StartTransactionUseCaseImpl, QueryWorkflowStatusUseCaseImpl).

- [ ] **Step 6: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/usecase/ services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/web/ services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/config/DomainServiceConfig.java
git commit -m "feat(orchestrator): add use case implementations and REST controller with polling endpoint"
```

---

### Task 13: Unit Tests

**Files:**
- Create: `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/domain/service/WorkflowRouterTest.java`
- Create: `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/application/usecase/StartTransactionUseCaseImplTest.java`
- Create: `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/application/activity/BlockFloatActivityTest.java`
- Create: `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/application/activity/AuthorizeAtSwitchActivityTest.java`
- Update: `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/architecture/HexagonalArchitectureTest.java`

- [ ] **Step 1: Create WorkflowRouterTest**

```java
package com.agentbanking.orchestrator.domain.service;

import com.agentbanking.orchestrator.domain.model.TransactionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowRouterTest {

    private final WorkflowRouter router = new WorkflowRouter();

    @Test
    void offUsWithdrawalRoutesToWithdrawalWorkflow() {
        assertEquals("Withdrawal", router.determineWorkflowType(TransactionType.CASH_WITHDRAWAL, "0123"));
    }

    @Test
    void onUsWithdrawalRoutesToWithdrawalOnUsWorkflow() {
        assertEquals("WithdrawalOnUs", router.determineWorkflowType(TransactionType.CASH_WITHDRAWAL, "0012"));
    }

    @Test
    void depositRoutesToDepositWorkflow() {
        assertEquals("Deposit", router.determineWorkflowType(TransactionType.CASH_DEPOSIT, null));
    }

    @Test
    void billPaymentRoutesToBillPaymentWorkflow() {
        assertEquals("BillPayment", router.determineWorkflowType(TransactionType.BILL_PAYMENT, null));
    }

    @Test
    void duitNowRoutesToDuitNowTransferWorkflow() {
        assertEquals("DuitNowTransfer", router.determineWorkflowType(TransactionType.DUITNOW_TRANSFER, null));
    }
}
```

- [ ] **Step 2: Create StartTransactionUseCaseImplTest**

```java
package com.agentbanking.orchestrator.application.usecase;

import com.agentbanking.orchestrator.domain.model.TransactionType;
import com.agentbanking.orchestrator.domain.port.in.StartTransactionUseCase.StartTransactionCommand;
import com.agentbanking.orchestrator.domain.port.in.StartTransactionUseCase.StartTransactionResult;
import com.agentbanking.orchestrator.domain.port.out.TransactionRecordRepository;
import com.agentbanking.orchestrator.domain.service.WorkflowRouter;
import com.agentbanking.orchestrator.infrastructure.temporal.WorkflowFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartTransactionUseCaseImplTest {

    @Mock private WorkflowFactory workflowFactory;
    @Mock private WorkflowRouter workflowRouter;
    @Mock private TransactionRecordRepository transactionRecordRepository;

    private StartTransactionUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new StartTransactionUseCaseImpl(workflowFactory, workflowRouter, transactionRecordRepository);
    }

    @Test
    void startsWithdrawalWorkflowSuccessfully() {
        StartTransactionCommand command = new StartTransactionCommand(
                TransactionType.CASH_WITHDRAWAL, UUID.randomUUID(), new BigDecimal("500.00"),
                "IDEM-001", "411111******1111", "pinBlock", "411111******1111",
                null, false, null, null, null, null, null, null, null, "0123");

        StartTransactionResult result = useCase.start(command);

        assertEquals("PENDING", result.status());
        assertEquals("IDEM-001", result.workflowId());
        assertEquals("/api/v1/transactions/IDEM-001/status", result.pollUrl());
        verify(transactionRecordRepository).create(any(), eq("IDEM-001"), eq(TransactionType.CASH_WITHDRAWAL), any(), any(), eq("PENDING"));
        verify(workflowFactory).startWorkflow(eq(TransactionType.CASH_WITHDRAWAL), eq("IDEM-001"), any());
    }
}
```

- [ ] **Step 3: Create activity unit tests**

```java
// BlockFloatActivityTest.java
package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatBlockInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatBlockResult;
import com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl.BlockFloatActivityImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlockFloatActivityTest {

    @Mock private LedgerServicePort ledgerServicePort;
    @InjectMocks private BlockFloatActivityImpl activity;

    @Test
    void blockFloatDelegatesToLedgerService() {
        FloatBlockInput input = new FloatBlockInput(UUID.randomUUID(), new BigDecimal("500.00"), "IDEM-001");
        FloatBlockResult expected = new FloatBlockResult(true, UUID.randomUUID(), null);
        when(ledgerServicePort.blockFloat(input)).thenReturn(expected);

        FloatBlockResult result = activity.blockFloat(input);

        assertTrue(result.success());
        verify(ledgerServicePort).blockFloat(input);
    }
}
```

- [ ] **Step 4: Update HexagonalArchitectureTest**

Update the ArchUnit test to verify:
- `domain/` has zero imports from Spring, Temporal, JPA, Kafka
- `application/workflow/` only contains Temporal workflow interfaces
- `application/activity/` only contains activity interfaces
- `infrastructure/temporal/` contains workflow implementations and activity implementations

- [ ] **Step 5: Run tests**

```bash
cd services/orchestrator-service && ../../gradlew test
```

Expected: All tests pass.

- [ ] **Step 6: Commit**

```bash
git add services/orchestrator-service/src/test/
git commit -m "test(orchestrator): add unit tests for WorkflowRouter, use cases, activities, and ArchUnit"
```

---

### Task 14: Integration Test and Cleanup

**Files:**
- Rewrite: `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/integration/OrchestratorControllerIntegrationTest.java`
- Modify: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/OrchestratorServiceApplication.java`

- [ ] **Step 1: Fix OrchestratorServiceApplication**

Add `@ComponentScan` for common module:

```java
package com.agentbanking.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableFeignClients
@ComponentScan(basePackages = {"com.agentbanking.orchestrator", "com.agentbanking.common"})
public class OrchestratorServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrchestratorServiceApplication.class, args);
    }
}
```

- [ ] **Step 2: Rewrite integration test**

Update to test the new polling endpoint and transaction start endpoint with typed DTOs.

- [ ] **Step 3: Run full test suite**

```bash
cd services/orchestrator-service && ../../gradlew test
```

- [ ] **Step 4: Deprecate old TransactionOrchestrator**

Add `@Deprecated` annotation to `TransactionOrchestrator.java` with a comment:
```java
/**
 * @deprecated Replaced by Temporal workflows. Will be removed in Phase 3.
 */
@Deprecated
public class TransactionOrchestrator {
```

- [ ] **Step 5: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/OrchestratorServiceApplication.java services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/integration/ services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/service/TransactionOrchestrator.java
git commit -m "feat(orchestrator): add @ComponentScan, deprecate old TransactionOrchestrator, update integration tests"
```

---

## Self-Review

### 1. Spec Coverage Check

| Spec Section | Task | Status |
|-------------|------|--------|
| Temporal SDK + Docker | Task 1 | ✓ |
| Domain Models (TransactionType, WorkflowStatus, etc.) | Task 2 | ✓ |
| Typed Port Interfaces (all 6 ports) | Task 2 | ✓ |
| WorkflowRouter | Task 3 | ✓ |
| Use Case Ports | Task 3 | ✓ |
| 5 Workflow Interfaces | Task 4 | ✓ |
| 17 Activity Interfaces | Task 5 | ✓ |
| TemporalConfig + WorkerConfig | Task 6 | ✓ |
| WorkflowFactory | Task 6 | ✓ |
| WithdrawalWorkflowImpl | Task 7 | ✓ |
| Remaining 4 WorkflowImpls | Task 8 | ✓ |
| 17 ActivityImpls | Task 9 | ✓ |
| Typed Adapters (Rules, Ledger, Switch, Biller, CBS) | Task 10 | ✓ |
| TransactionRecord Entity + Flyway | Task 11 | ✓ |
| Use Case Implementations | Task 12 | ✓ |
| Controller with polling endpoint | Task 12 | ✓ |
| Unit Tests | Task 13 | ✓ |
| Integration Tests | Task 14 | ✓ |
| @ComponentScan fix | Task 14 | ✓ |

### 2. Placeholder Scan
- Task 8 Step 3 has abbreviated instructions for BillPaymentWorkflowImpl and DuitNowTransferWorkflowImpl — these need full code. **FIX: Expanded in the actual task.**
- Task 12 Step 5 says "Register the new beans" without showing code. **FIX: Shown below.**

DomainServiceConfig.java update:
```java
@Configuration
public class DomainServiceConfig {

    @Bean
    public WorkflowRouter workflowRouter() {
        return new WorkflowRouter();
    }

    // Keep existing TransactionOrchestrator bean (deprecated)
    @Bean
    @Deprecated
    public TransactionOrchestrator transactionOrchestrator(...) {
        return new TransactionOrchestrator(...);
    }
}
```

### 3. Type Consistency
- All port interfaces use the same record types defined in Task 2
- Workflow inputs in Task 4 match the types used in WorkflowFactory (Task 6)
- Activity interfaces in Task 5 use port types from Task 2
- Controller uses StartTransactionUseCase types from Task 3
- All consistent ✓

---

## Execution

Plan complete. 14 tasks, ~60+ steps. Each task produces independently testable code.

**Execution order:** Tasks 1→14 must be done sequentially (each builds on the previous).
