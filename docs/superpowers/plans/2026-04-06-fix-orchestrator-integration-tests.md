# Fix Orchestrator Integration Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all 30 failing integration tests by adding Temporal test container infrastructure and add integration tests for 9 new transaction types.

**Architecture:** Create orchestrator-specific test base class (`AbstractOrchestratorIntegrationTest`) that extends the common `AbstractIntegrationTest`, adds a Temporal Docker container, and mocks all 12 external Feign clients. The integration tests will test real controller → use case → Temporal workflow lifecycle while mocking downstream services.

**Tech Stack:** Java 21, Spring Boot 3.2.5, Testcontainers (Temporal), JUnit 5, Mockito, MockMvc

---

## Root Cause Analysis

**Problem:** All 30 integration tests fail with `io.grpc.StatusRuntimeException → java.net.ConnectException`

**Why:** The orchestrator uses Temporal SDK for workflow orchestration. When Spring Boot starts the test context, the Temporal Spring Boot Starter tries to connect to a Temporal server at `localhost:7233`. The shared `AbstractIntegrationTest` only provisions PostgreSQL, Redis, and Kafka containers — no Temporal server.

**Why other services pass:** Auth, ledger, biller, rules, onboarding, switch-adapter services don't depend on Temporal. The orchestrator is unique.

**Why not add Temporal to `AbstractIntegrationTest`:** Would affect ALL 8 services unnecessarily, slowing down all integration tests.

---

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `services/orchestrator-service/build.gradle` | Modify | Add Temporal testcontainer dependency |
| `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/integration/AbstractOrchestratorIntegrationTest.java` | Create | Test base class with Temporal container + Feign client mocks |
| `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/integration/OrchestratorControllerIntegrationTest.java` | Modify | Change to extend new base, add 9 new transaction type tests |

---

### Task 1: Add Temporal TestContainer Dependency

**Files:**
- Modify: `services/orchestrator-service/build.gradle`

- [ ] **Step 1: Add Temporal testcontainer dependency**

Add to the `dependencies` block in `build.gradle` (after line 40):

```groovy
    testImplementation 'org.testcontainers:temporal:1.20.1'
```

The full test dependencies section should look like:

```groovy
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'
    testImplementation 'org.testcontainers:testcontainers:1.20.1'
    testImplementation 'org.testcontainers:junit-jupiter:1.20.1'
    testImplementation 'org.testcontainers:postgresql:1.20.1'
    testImplementation 'org.testcontainers:kafka:1.20.1'
    testImplementation 'org.testcontainers:temporal:1.20.1'
    testImplementation 'io.temporal:temporal-testing:1.25.1'
    testImplementation project(':common')
    testImplementation testFixtures(project(':common'))
```

- [ ] **Step 2: Verify dependency resolves**

Run: `./gradlew :services:orchestrator-service:dependencies --configuration testRuntimeClasspath | grep temporal`
Expected: Should show `io.temporal:temporal-sdk:1.25.1` and `org.testcontainers:temporal:1.20.1`

- [ ] **Step 3: Commit**

```bash
git add services/orchestrator-service/build.gradle
git commit -m "test: add Temporal testcontainer dependency for integration tests"
```

---

### Task 2: Create AbstractOrchestratorIntegrationTest

**Files:**
- Create: `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/integration/AbstractOrchestratorIntegrationTest.java`

This class:
1. Extends `AbstractIntegrationTest` (inherits PostgreSQL, Redis, Kafka)
2. Adds Temporal test container
3. Registers Temporal connection properties via `@DynamicPropertySource`
4. Mocks all 12 external Feign clients with `@MockBean`

- [ ] **Step 1: Create the test base class**

Create file: `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/integration/AbstractOrchestratorIntegrationTest.java`

```java
package com.agentbanking.orchestrator.integration;

import com.agentbanking.common.test.AbstractIntegrationTest;
import com.agentbanking.orchestrator.infrastructure.external.*;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Base class for orchestrator integration tests.
 * 
 * Extends AbstractIntegrationTest (PostgreSQL, Redis, Kafka) and adds:
 * - Temporal server container for real workflow testing
 * - Mocked Feign clients for all external services
 * 
 * The orchestrator has a unique dependency on Temporal that other services don't have,
 * so we isolate this test infrastructure here rather than polluting the shared base.
 */
public abstract class AbstractOrchestratorIntegrationTest extends AbstractIntegrationTest {

    // Temporal test container
    static final GenericContainer<?> temporal = new GenericContainer<>("temporalio/auto-setup:1.25.1")
            .withExposedPorts(7233)
            .withEnv("DB", "postgresql")
            .withEnv("DB_PORT", "5432")
            .withEnv("POSTGRES_USER", "postgres")
            .withEnv("POSTGRES_PWD", "postgres")
            .withEnv("POSTGRES_SEEDS", postgres.getHost())
            .withEnv("DYNAMIC_CONFIG_FILE_PATH", "/etc/temporal/dynamicconfig/development-sql.yaml")
            .dependsOn(postgres)
            .waitingFor(Wait.forLogMessage(".*Started workflow dispatcher.*", 1));

    static {
        temporal.start();
    }

    @DynamicPropertySource
    static void registerTemporalProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.temporal.connection.target", 
                () -> temporal.getHost() + ":" + temporal.getMappedPort(7233));
        registry.add("temporal.task-queue", () -> "test-agent-banking-tasks");
    }

    // Mock all external Feign clients - these are tested separately in unit tests
    // The integration test focuses on: controller → use case → workflow router → Temporal workflow start → status polling

    @MockBean
    protected SwitchAdapterClient switchAdapterClient;

    @MockBean
    protected LedgerServiceClient ledgerServiceClient;

    @MockBean
    protected RulesServiceClient rulesServiceClient;

    @MockBean
    protected BillerServiceClient billerServiceClient;

    @MockBean
    protected CbsServiceClient cbsServiceClient;

    @MockBean
    protected TelcoAggregatorClient telcoAggregatorClient;

    @MockBean
    protected EWalletProviderClient ewalletProviderClient;

    @MockBean
    protected ESSPServiceClient esspServiceClient;

    @MockBean
    protected PINInventoryClient pinInventoryClient;

    @MockBean
    protected QRPaymentClient qrPaymentClient;

    @MockBean
    protected RequestToPayClient requestToPayClient;

    @MockBean
    protected MerchantTransactionClient merchantTransactionClient;
}
```

- [ ] **Step 2: Verify file compiles**

Run: `./gradlew :services:orchestrator-service:compileTestJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/integration/AbstractOrchestratorIntegrationTest.java
git commit -m "test: create AbstractOrchestratorIntegrationTest with Temporal container and mocked Feign clients"
```

---

### Task 3: Update OrchestratorControllerIntegrationTest to Use New Base

**Files:**
- Modify: `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/integration/OrchestratorControllerIntegrationTest.java`

- [ ] **Step 1: Change base class**

Change line 24 from:
```java
class OrchestratorControllerIntegrationTest extends AbstractIntegrationTest {
```
to:
```java
class OrchestratorControllerIntegrationTest extends AbstractOrchestratorIntegrationTest {
```

And remove the import:
```java
import com.agentbanking.common.test.AbstractIntegrationTest;
```

- [ ] **Step 2: Run existing integration tests to verify they pass**

Run: `./gradlew :services:orchestrator-service:test --tests "*OrchestratorControllerIntegrationTest*" -i`
Expected: All 30 tests PASS

Note: The first run will download the Temporal Docker image (~200MB). Subsequent runs will be faster.

- [ ] **Step 3: Commit**

```bash
git add services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/integration/OrchestratorControllerIntegrationTest.java
git commit -m "test: update OrchestratorControllerIntegrationTest to use Temporal-aware base class"
```

---

### Task 4: Add Integration Tests for 9 New Transaction Types

**Files:**
- Modify: `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/integration/OrchestratorControllerIntegrationTest.java`

Add a new nested test class for the 9 new transaction types. Each test verifies:
1. The controller accepts the request
2. The workflow is started (returns PENDING status)
3. The workflowId matches the idempotencyKey

- [ ] **Step 1: Add new transaction type test class**

Add this nested class to `OrchestratorControllerIntegrationTest.java` (after the `CrossWorkflowTests` class, before the helper methods):

```java
    @Nested
    @DisplayName("BDD-TO-NEW: New Transaction Types")
    class NewTransactionTypeTests {

        @Test
        @DisplayName("BDD-TO-NEW-01: Router dispatches cashless payment to CashlessPaymentWorkflow")
        void cashlessPayment_validData_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-cashless-" + UUID.randomUUID();
            String requestBody = """
                {
                    "transactionType": "CASHLESS_PAYMENT",
                    "agentId": "%s",
                    "amount": 150.00,
                    "idempotencyKey": "%s",
                    "agentTier": "TIER_1",
                    "customerMykad": "encrypted-mykad",
                    "geofenceLat": 3.1390,
                    "geofenceLng": 101.6869
                }
                """.formatted(AGENT_ID, idempotencyKey);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey))
                    .andExpect(jsonPath("$.pollUrl").exists());
        }

        @Test
        @DisplayName("BDD-TO-NEW-02: Router dispatches pin-based purchase to PinBasedPurchaseWorkflow")
        void pinBasedPurchase_validData_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-pin-purchase-" + UUID.randomUUID();
            String requestBody = """
                {
                    "transactionType": "PIN_BASED_PURCHASE",
                    "agentId": "%s",
                    "amount": 250.00,
                    "idempotencyKey": "%s",
                    "agentTier": "TIER_1",
                    "pan": "4111111111111111",
                    "pinBlock": "encrypted-pin-block",
                    "customerCardMasked": "411111******1111",
                    "geofenceLat": 3.1390,
                    "geofenceLng": 101.6869
                }
                """.formatted(AGENT_ID, idempotencyKey);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-TO-NEW-03: Router dispatches prepaid topup to PrepaidTopupWorkflow")
        void prepaidTopup_validData_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-prepaid-topup-" + UUID.randomUUID();
            String requestBody = """
                {
                    "transactionType": "PREPAID_TOPUP",
                    "agentId": "%s",
                    "amount": 30.00,
                    "idempotencyKey": "%s",
                    "agentTier": "TIER_1",
                    "customerMykad": "encrypted-mykad",
                    "destinationAccount": "0123456789",
                    "geofenceLat": 3.1390,
                    "geofenceLng": 101.6869
                }
                """.formatted(AGENT_ID, idempotencyKey);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-TO-NEW-04: Router dispatches ewallet withdrawal to EWalletWithdrawalWorkflow")
        void ewalletWithdrawal_validData_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-ewallet-withdraw-" + UUID.randomUUID();
            String requestBody = """
                {
                    "transactionType": "EWALLET_WITHDRAWAL",
                    "agentId": "%s",
                    "amount": 100.00,
                    "idempotencyKey": "%s",
                    "agentTier": "TIER_1",
                    "customerMykad": "encrypted-mykad",
                    "destinationAccount": "ewallet-account-123",
                    "geofenceLat": 3.1390,
                    "geofenceLng": 101.6869
                }
                """.formatted(AGENT_ID, idempotencyKey);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-TO-NEW-05: Router dispatches ewallet topup to EWalletTopupWorkflow")
        void ewalletTopup_validData_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-ewallet-topup-" + UUID.randomUUID();
            String requestBody = """
                {
                    "transactionType": "EWALLET_TOPUP",
                    "agentId": "%s",
                    "amount": 50.00,
                    "idempotencyKey": "%s",
                    "agentTier": "TIER_1",
                    "customerMykad": "encrypted-mykad",
                    "destinationAccount": "ewallet-account-456",
                    "geofenceLat": 3.1390,
                    "geofenceLng": 101.6869
                }
                """.formatted(AGENT_ID, idempotencyKey);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-TO-NEW-06: Router dispatches ESSP purchase to ESSPPurchaseWorkflow")
        void esspPurchase_validData_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-essp-purchase-" + UUID.randomUUID();
            String requestBody = """
                {
                    "transactionType": "ESSP_PURCHASE",
                    "agentId": "%s",
                    "amount": 20.00,
                    "idempotencyKey": "%s",
                    "agentTier": "TIER_1",
                    "customerMykad": "encrypted-mykad",
                    "geofenceLat": 3.1390,
                    "geofenceLng": 101.6869
                }
                """.formatted(AGENT_ID, idempotencyKey);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-TO-NEW-07: Router dispatches PIN purchase to PINPurchaseWorkflow")
        void pinPurchase_validData_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-pin-purchase-" + UUID.randomUUID();
            String requestBody = """
                {
                    "transactionType": "PIN_PURCHASE",
                    "agentId": "%s",
                    "amount": 10.00,
                    "idempotencyKey": "%s",
                    "agentTier": "TIER_1",
                    "customerMykad": "encrypted-mykad",
                    "destinationAccount": "telco-number-789",
                    "geofenceLat": 3.1390,
                    "geofenceLng": 101.6869
                }
                """.formatted(AGENT_ID, idempotencyKey);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-TO-NEW-08: Router dispatches retail sale to RetailSaleWorkflow")
        void retailSale_validData_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-retail-sale-" + UUID.randomUUID();
            String requestBody = """
                {
                    "transactionType": "RETAIL_SALE",
                    "agentId": "%s",
                    "amount": 75.00,
                    "idempotencyKey": "%s",
                    "agentTier": "TIER_1",
                    "customerMykad": "encrypted-mykad",
                    "geofenceLat": 3.1390,
                    "geofenceLng": 101.6869
                }
                """.formatted(AGENT_ID, idempotencyKey);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-TO-NEW-09: Router dispatches hybrid cashback to HybridCashbackWorkflow")
        void hybridCashback_validData_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-hybrid-cashback-" + UUID.randomUUID();
            String requestBody = """
                {
                    "transactionType": "HYBRID_CASHBACK",
                    "agentId": "%s",
                    "amount": 200.00,
                    "idempotencyKey": "%s",
                    "agentTier": "TIER_1",
                    "pan": "4111111111111111",
                    "customerCardMasked": "411111******1111",
                    "geofenceLat": 3.1390,
                    "geofenceLng": 101.6869
                }
                """.formatted(AGENT_ID, idempotencyKey);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }
    }
```

- [ ] **Step 2: Run all integration tests including new ones**

Run: `./gradlew :services:orchestrator-service:test --tests "*OrchestratorControllerIntegrationTest*" -i`
Expected: All 39 tests PASS (30 existing + 9 new)

- [ ] **Step 3: Run ALL orchestrator tests to verify nothing is broken**

Run: `./gradlew :services:orchestrator-service:test`
Expected: All tests PASS (92 unit tests + 39 integration tests = 131 total)

- [ ] **Step 4: Commit**

```bash
git add services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/integration/OrchestratorControllerIntegrationTest.java
git commit -m "test: add integration tests for 9 new transaction types"
```

---

## Self-Review Checklist

### Spec Coverage
- ✅ Root cause identified: Missing Temporal container in test infrastructure
- ✅ Solution: Orchestrator-specific test base with Temporal container
- ✅ All 30 failing tests addressed by changing base class
- ✅ 9 new transaction type integration tests added
- ✅ Feign clients mocked (12 total) to avoid needing real downstream services

### Placeholder Scan
- ✅ No TBD, TODO, or "implement later" placeholders
- ✅ All code blocks contain complete, copy-pasteable content
- ✅ All file paths are exact
- ✅ All commands include expected output

### Type Consistency
- ✅ TransactionType enum values match exactly: CASHLESS_PAYMENT, PIN_BASED_PURCHASE, PREPAID_TOPUP, EWALLET_WITHDRAWAL, EWALLET_TOPUP, ESSP_PURCHASE, PIN_PURCHASE, RETAIL_SALE, HYBRID_CASHBACK
- ✅ Feign client class names match existing infrastructure
- ✅ Test class naming follows existing convention

### Architecture Compliance
- ✅ Hexagonal architecture preserved (test infrastructure doesn't affect domain layer)
- ✅ MockBean used for external services (proper isolation)
- ✅ Temporal container uses same version as production (1.25.1)
- ✅ Test base class isolated to orchestrator service only

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Temporal container startup slow | Medium | Low | First run downloads image; subsequent runs cached |
| Temporal container conflicts with local dev | Low | Medium | Uses dynamic port mapping, no fixed ports |
| MockBean configuration wrong | Low | High | All 12 Feign clients listed explicitly |
| Test flakiness due to container timing | Low | Medium | Wait strategy with log message pattern |

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-04-06-fix-orchestrator-integration-tests.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
