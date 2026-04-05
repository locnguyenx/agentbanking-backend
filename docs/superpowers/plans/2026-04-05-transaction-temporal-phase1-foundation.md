# Phase 1: Foundation — Temporal Infrastructure and Domain Layer

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Set up Temporal SDK, Docker, domain models, typed port interfaces, workflow/activity interfaces, and Temporal configuration. No implementation logic yet — just interfaces and infrastructure.

**Architecture:** Temporal SDK added to build. All domain ports rewritten from Map-based to typed Records. Workflow and activity interfaces defined. Temporal client and worker configured.

**Tech Stack:** Java 21, Spring Boot 3.2.5, Temporal SDK 1.25.x, PostgreSQL, Redis, JUnit 5, Mockito, ArchUnit.

**Spec References:**
- Design: `docs/superpowers/specs/agent-banking-platform/2026-04-05-transaction-orchestrator-temporal-design.md`
- BRD Addendum: `docs/superpowers/specs/agent-banking-platform/2026-04-05-transaction-brd-addendum.md`
- BDD Addendum: `docs/superpowers/specs/agent-banking-platform/2026-04-05-transaction-bdd-addendum.md`

**Dependencies:** None — this is the first phase.

---

### Task 1.1: Temporal SDK and Docker Compose

**Files:**
- Modify: `services/orchestrator-service/build.gradle`
- Modify: `services/orchestrator-service/src/main/resources/application.yaml`
- Modify: `docker-compose.yml`

- [ ] **Step 1: Add Temporal SDK to build.gradle**

Read `services/orchestrator-service/build.gradle`. Replace the entire file with:

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

- [ ] **Step 2: Verify dependency resolves**

```bash
cd services/orchestrator-service && ../../gradlew dependencies --configuration implementation 2>&1 | grep temporal
```

Expected: `io.temporal:temporal-sdk:1.25.1` appears.

- [ ] **Step 3: Add Temporal config to application.yaml**

Read `services/orchestrator-service/src/main/resources/application.yaml`. Append at the end:

```yaml

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

Read `docker-compose.yml`. Add these services before the `volumes:` section:

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

Add `temporal-postgres-data:` to the `volumes:` section at the bottom.

- [ ] **Step 5: Commit**

```bash
git add services/orchestrator-service/build.gradle services/orchestrator-service/src/main/resources/application.yaml docker-compose.yml
git commit -m "feat(orchestrator): add Temporal SDK dependency and docker-compose services"
```

---

### Task 1.2: Domain Models

**Files:**
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/TransactionType.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/WorkflowStatus.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/WorkflowResult.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/ForceResolveSignal.java`

**CRITICAL:** All files in `domain/` must have ZERO imports from Spring, Temporal, JPA, Kafka. Pure Java only.

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

- [ ] **Step 5: Verify domain/ has zero framework imports**

```bash
find services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain -name "*.java" -exec grep -l "import org.springframework\|import io.temporal\|import jakarta.persistence" {} \;
```

Expected: Empty output.

- [ ] **Step 6: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/
git commit -m "feat(orchestrator): add domain models (TransactionType, WorkflowStatus, WorkflowResult, ForceResolveSignal)"
```

---

### Task 1.3: Typed Port Interfaces (Rewrite from Map-based)

**Files:**
- Rewrite: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/RulesServicePort.java`
- Rewrite: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/LedgerServicePort.java`
- Rewrite: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/SwitchAdapterPort.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/BillerServicePort.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/CbsServicePort.java`
- Rewrite: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/EventPublisherPort.java`

- [ ] **Step 1: Rewrite RulesServicePort**

Delete existing file and replace with:

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

- [ ] **Step 2: Rewrite LedgerServicePort**

Delete existing file and replace with:

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

- [ ] **Step 3: Rewrite SwitchAdapterPort**

Delete existing file and replace with:

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

- [ ] **Step 4: Create BillerServicePort**

```java
package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;

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

- [ ] **Step 5: Create CbsServicePort**

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

- [ ] **Step 6: Rewrite EventPublisherPort**

Delete existing file and replace with:

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

- [ ] **Step 7: Verify zero framework imports in domain/**

```bash
find services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain -name "*.java" -exec grep -l "import org.springframework\|import io.temporal\|import jakarta.persistence" {} \;
```

Expected: Empty output.

- [ ] **Step 8: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/
git commit -m "refactor(orchestrator): replace Map-based ports with typed interfaces (Rules, Ledger, Switch, Biller, CBS, Events)"
```

---

### Task 1.4: Use Case Ports and WorkflowRouter

**Files:**
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/in/StartTransactionUseCase.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/in/QueryWorkflowStatusUseCase.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/service/WorkflowRouter.java`

- [ ] **Step 1: Create StartTransactionUseCase**

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
        String pan,
        String pinBlock,
        String customerCardMasked,
        String destinationAccount,
        boolean requiresBiometric,
        String billerCode,
        String ref1,
        String ref2,
        String proxyType,
        String proxyValue,
        String customerMykad,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng,
        String targetBIN,
        String agentTier
    ) {}

    record StartTransactionResult(
        String status,
        String workflowId,
        String pollUrl
    ) {}
}
```

- [ ] **Step 2: Create QueryWorkflowStatusUseCase**

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

- [ ] **Step 3: Create WorkflowRouter**

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

### Task 1.5: Temporal Workflow Interfaces

**Files:**
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/WithdrawalWorkflow.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/WithdrawalOnUsWorkflow.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/DepositWorkflow.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/BillPaymentWorkflow.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/DuitNowTransferWorkflow.java`

- [ ] **Step 1: Create all 5 workflow interfaces**

**WithdrawalWorkflow.java:**
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

**WithdrawalOnUsWorkflow.java:**
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

**DepositWorkflow.java:**
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

**BillPaymentWorkflow.java:**
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

**DuitNowTransferWorkflow.java:**
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

- [ ] **Step 2: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/
git commit -m "feat(orchestrator): add 5 Temporal workflow interfaces (Withdrawal, On-Us, Deposit, BillPayment, DuitNow)"
```

---

### Task 1.6: Temporal Activity Interfaces

**Files:** Create all 17 files in `application/activity/`

- [ ] **Step 1: Create all 17 activity interfaces**

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
    BillValidationResult validateBill(BillValidationInput input);
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

### Task 1.7: Temporal Configuration and Worker Setup

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

    public String startWithdrawalWorkflow(String idempotencyKey,
                                           WithdrawalWorkflow.WithdrawalInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(idempotencyKey)
                .setTaskQueue(taskQueue)
                .setWorkflowIdReusePolicy(
                        io.temporal.common.WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        WithdrawalWorkflow workflow = workflowClient.newWorkflowStub(
                WithdrawalWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return idempotencyKey;
    }

    public String startDepositWorkflow(String idempotencyKey,
                                        DepositWorkflow.DepositInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(idempotencyKey)
                .setTaskQueue(taskQueue)
                .setWorkflowIdReusePolicy(
                        io.temporal.common.WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        DepositWorkflow workflow = workflowClient.newWorkflowStub(
                DepositWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return idempotencyKey;
    }

    public String startBillPaymentWorkflow(String idempotencyKey,
                                            BillPaymentWorkflow.BillPaymentInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(idempotencyKey)
                .setTaskQueue(taskQueue)
                .setWorkflowIdReusePolicy(
                        io.temporal.common.WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        BillPaymentWorkflow workflow = workflowClient.newWorkflowStub(
                BillPaymentWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return idempotencyKey;
    }

    public String startDuitNowTransferWorkflow(String idempotencyKey,
                                                DuitNowTransferWorkflow.DuitNowTransferInput input) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(idempotencyKey)
                .setTaskQueue(taskQueue)
                .setWorkflowIdReusePolicy(
                        io.temporal.common.WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        DuitNowTransferWorkflow workflow = workflowClient.newWorkflowStub(
                DuitNowTransferWorkflow.class, options);
        WorkflowClient.start(workflow::execute, input);
        return idempotencyKey;
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

### Task 1.8: Fix Application Class and Run Tests

**Files:**
- Modify: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/OrchestratorServiceApplication.java`

- [ ] **Step 1: Add @ComponentScan for common module**

Read the file and replace it with:

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

- [ ] **Step 2: Run tests**

```bash
cd services/orchestrator-service && ../../gradlew test
```

Expected: Existing tests pass. New code compiles. Some tests may fail because the old adapters still use Map-based ports — that's expected, they'll be fixed in Phase 3.

- [ ] **Step 3: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/OrchestratorServiceApplication.java
git commit -m "fix(orchestrator): add @ComponentScan for common module (Law IX compliance)"
```

---

## Self-Review

### Spec Coverage
| Spec Section | Task | Status |
|-------------|------|--------|
| Temporal SDK + Docker | 1.1 | ✓ |
| Domain Models | 1.2 | ✓ |
| Typed Ports (6 interfaces) | 1.3 | ✓ |
| Use Case Ports | 1.4 | ✓ |
| WorkflowRouter | 1.4 | ✓ |
| 5 Workflow Interfaces | 1.5 | ✓ |
| 17 Activity Interfaces | 1.6 | ✓ |
| TemporalConfig + Factory + Worker | 1.7 | ✓ |
| @ComponentScan fix | 1.8 | ✓ |

### Placeholder Scan
No TBD, TODO, "follow the same pattern", or abbreviated steps found.

### Type Consistency
- All port interfaces use records defined within themselves (no cross-file type references)
- Workflow inputs match WorkflowFactory method signatures
- Activity interfaces use port types from Task 1.3
- All consistent ✓
