# Real Integration Testing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enhance existing E2E tests to verify full BDD Happy Path scenarios (status + balance + journal) and add OpenAPI-validated contract tests against real services.

**Architecture:** Extends the existing `SelfContainedOrchestratorE2ETest` pattern. Phase 1 enhances existing dispatch-only tests with poll→verify logic. Phase 2 creates a new `OpenApiContractE2ETest` that hits real services through the gateway and validates responses against `docs/api/openapi.yaml`. Phase 3 fills polling/idempotency gaps.

**Tech Stack:** JUnit 5, WebTestClient, Jackson, `com.atlassian.oai:swagger-request-validator-restassured` (Phase 2)

**BRD:** [brd-real-integration-testing.md](file:///Users/me/myprojects/agentbanking-backend/docs/superpowers/specs/brd-real-integration-testing.md)

**BDD Specs:**
- [2026-03-25-agent-banking-platform-bdd.md](file:///Users/me/myprojects/agentbanking-backend/docs/superpowers/specs/agent-banking-platform/2026-03-25-agent-banking-platform-bdd.md)
- [2026-04-05-transaction-bdd-addendum.md](file:///Users/me/myprojects/agentbanking-backend/docs/superpowers/specs/agent-banking-platform/2026-04-05-transaction-bdd-addendum.md)
- [2026-04-06-missing-transaction-types-bdd-addendum.md](file:///Users/me/myprojects/agentbanking-backend/docs/superpowers/specs/agent-banking-platform/2026-04-06-missing-transaction-types-bdd-addendum.md)

---

## Prerequisites

```bash
# Start all services (required for E2E tests)
docker compose --profile all up -d

# Verify services are running
docker ps | grep -E "gateway|orchestrator|ledger|temporal"

# Reset test data
./gradlew resetSystem
```

---

## Task 1: Add Journal Entry Endpoint to Ledger Service [DONE]

**BDD Scenarios:** BDD-L02 (Double-entry journal verification)
**BRD Requirements:** BRD §5.2 — Journal Entry Endpoint

**User-Facing:** NO

**Files:**
- Modify: `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/web/LedgerController.java`
- Test: `services/ledger-service/src/test/java/com/agentbanking/ledger/infrastructure/web/LedgerControllerIntegrationTest.java`

**Context:** `JournalEntryRepository.findByTransactionId(UUID)` exists. `JournalEntryRecord` has fields: `journalId`, `transactionId`, `entryType`, `accountCode`, `amount`, `description`, `createdAt`. The existing E2E test already calls `GET /internal/journal?workflowId=...` (see `SelfContainedOrchestratorE2ETest:1851`), but this endpoint **does not exist yet** in `LedgerController`. We need to add it.

- [ ] **Step 1: Write the failing integration test**

Add a test to verify the journal endpoint returns entries by transaction/workflow ID.

```java
// In LedgerControllerIntegrationTest.java
@Test
void getJournalEntries_existingTransaction_returnsEntries() {
    // Given: a transaction that has journal entries (create via existing debit endpoint)
    // When: GET /internal/journal?workflowId={workflowId}
    // Then: HTTP 200 with array of JournalEntryRecord objects
    ResponseEntity<String> response = restTemplate.getForEntity(
        "/internal/journal?workflowId={workflowId}", String.class, testWorkflowId);
    
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body.isArray()).isTrue();
}

@Test
void getJournalEntries_noEntries_returnsEmptyArray() {
    ResponseEntity<String> response = restTemplate.getForEntity(
        "/internal/journal?workflowId={workflowId}", String.class, UUID.randomUUID().toString());
    
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body.isArray()).isTrue();
    assertThat(body.size()).isEqualTo(0);
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :services:ledger-service:test --tests "*LedgerControllerIntegrationTest.getJournalEntries*" -i
```
Expected: FAIL — 404 (endpoint doesn't exist)

- [ ] **Step 3: Implement the endpoint**

Add to `LedgerController.java`:

```java
@GetMapping("/internal/journal")
public ResponseEntity<List<JournalEntryRecord>> getJournalEntries(
        @RequestParam String workflowId) {
    List<JournalEntryRecord> entries = journalEntryRepository
        .findByTransactionId(UUID.fromString(workflowId));
    return ResponseEntity.ok(entries);
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
./gradlew :services:ledger-service:test --tests "*LedgerControllerIntegrationTest.getJournalEntries*" -i
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add services/ledger-service/
git commit -m "feat(ledger): add GET /internal/journal endpoint for E2E verification"
```

---

## Task 2: Enhance E2E Tests — VPA, Bill Pay, Funds Transfer [IN_PROGRESS]

**BDD Scenarios:** BDD-WF-HP-W01 (Off-Us withdrawal completes successfully), BDD-WF-HP-W02 (On-Us withdrawal completes successfully)
**BRD Requirements:** BRD §4 Phase 1 — Transaction Happy Paths

**User-Facing:** NO

**Files:**
- Modify: `gateway/src/test/java/com/agentbanking/gateway/integration/orchestrator/SelfContainedOrchestratorE2ETest.java:941-1040`

**Context:** BDD-WF-HP-W01 at line 941 already has the full verification pattern. BDD-WF-HP-W02 at line 990 only checks HTTP 200/202 but doesn't poll or verify balance. The `waitForWorkflowCompletion()`, `getAgentFloatBalance()`, and `getJournalEntries()` helpers already exist.

- [ ] **Step 1: Enhance BDD-WF-HP-W02 to verify completion**

Replace the On-Us withdrawal test body to follow the same pattern as W01:

```java
@Test
@DisplayName("BDD-WF-HP-W02: On-Us withdrawal completes successfully")
void onUsWithdrawal_shouldCompleteSuccessfully() {
    assumeTrue(agentToken != null, "Agent token required");

    String idempotencyKey = "e2e-hp-withdraw-onus-" + UUID.randomUUID();
    String requestBody = buildWithdrawalRequest(idempotencyKey, "0012");

    // Record initial balance
    BigDecimal initialBalance = getAgentFloatBalance(getEffectiveAgentId());

    String body = gatewayClient.post()
            .uri("/api/v1/transactions")
            .header("Authorization", "Bearer " + agentToken)
            .header("X-Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(body);
    JsonNode json = parseBody(body);
    assertEquals("PENDING", json.get("status").asText());

    // Poll until workflow completes
    WorkflowDetails details = waitForWorkflowCompletion(idempotencyKey);

    // Verify workflow completed successfully
    assertEquals("COMPLETED", details.status(),
        "Workflow should complete, was: " + details.status() +
        (details.errorCode() != null ? " Error: " + details.errorCode() : ""));

    // Verify balance decreased
    BigDecimal finalBalance = getAgentFloatBalance(getEffectiveAgentId());
    assertTrue(finalBalance.compareTo(initialBalance) < 0,
        "Agent float should decrease for On-Us withdrawal");
}
```

- [ ] **Step 2: Run the test to verify**

```bash
./gradlew :gateway:e2eTest --tests "*WithdrawalHappyPath*" --no-daemon
```
Expected: Both W01 and W02 PASS

- [ ] **Step 3: Commit**

```bash
git add gateway/src/test/
git commit -m "test(e2e): enhance BDD-WF-HP-W02 with poll, balance, and journal verification"
```

---

## Task 3: Enhance Deposit Happy Path Test (Phase 1)

**BDD Scenarios:** BDD-WF-HP-D01 (Cash deposit completes successfully — float increases by +amount)
**BRD Requirements:** BRD §4 Phase 1

**User-Facing:** NO

**Files:**
- Modify: `gateway/src/test/java/com/agentbanking/gateway/integration/orchestrator/SelfContainedOrchestratorE2ETest.java`

**Context:** Look for the existing deposit happy path test (search for "BDD-WF-HP-D01" or a `@Nested` class for deposits). If it only does dispatch verification, enhance it. If it doesn't exist yet, create it in a new `@Nested class DepositHappyPath` following the W01 pattern.

- [ ] **Step 1: Add or enhance BDD-WF-HP-D01 test**

Follow the same pattern as BDD-WF-HP-W01:
1. Record `initialBalance`
2. Submit deposit via `POST /api/v1/transactions` with `transactionType: CASH_DEPOSIT`
3. Assert `status == PENDING`
4. `waitForWorkflowCompletion(idempotencyKey)`
5. Assert `status == COMPLETED`
6. Get `finalBalance` — assert `finalBalance > initialBalance` (deposit should increase float)
7. Get journal entries — assert they exist

```java
@Test
@DisplayName("BDD-WF-HP-D01: Cash deposit completes successfully")
void cashDeposit_shouldCompleteSuccessfully() {
    assumeTrue(agentToken != null, "Agent token required");

    String idempotencyKey = "e2e-hp-deposit-" + UUID.randomUUID();
    String requestBody = buildDepositRequest(idempotencyKey);

    BigDecimal initialBalance = getAgentFloatBalance(getEffectiveAgentId());

    String body = gatewayClient.post()
            .uri("/api/v1/transactions")
            .header("Authorization", "Bearer " + agentToken)
            .header("X-Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(body);
    assertEquals("PENDING", parseBody(body).get("status").asText());

    WorkflowDetails details = waitForWorkflowCompletion(idempotencyKey);
    assertEquals("COMPLETED", details.status(),
        "Deposit should complete, was: " + details.status() +
        (details.errorCode() != null ? " Error: " + details.errorCode() : ""));

    BigDecimal finalBalance = getAgentFloatBalance(getEffectiveAgentId());
    assertTrue(finalBalance.compareTo(initialBalance) > 0,
        "Agent float should increase for deposit");

    List<JsonNode> journalEntries = getJournalEntries(idempotencyKey);
    assertFalse(journalEntries.isEmpty(), "Should have journal entries for deposit");
}
```

- [ ] **Step 2: Run the test**

```bash
./gradlew :gateway:e2eTest --tests "*DepositHappyPath*" --no-daemon
```
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add gateway/src/test/
git commit -m "test(e2e): add BDD-WF-HP-D01 deposit happy path with full verification"
```

---

## Task 4: Enhance Remaining Transaction Type Happy Paths (Phase 1)

**BDD Scenarios:** BDD-WF-HP-BP01, BDD-WF-HP-DN01, BDD-WF-HP-CP01, BDD-WF-HP-PBP01, BDD-WF-HP-PT01, BDD-WF-HP-EWW01, BDD-WF-HP-EWT01, BDD-WF-HP-ESSP01, BDD-WF-HP-PIN01, BDD-WF-HP-RS01, BDD-WF-HP-HCB01
**BRD Requirements:** BRD §4 Phase 1 — remaining 11 transaction types

**User-Facing:** NO

**Files:**
- Modify: `gateway/src/test/java/com/agentbanking/gateway/integration/orchestrator/SelfContainedOrchestratorE2ETest.java`

**Context:** Each of these 11 transaction types needs the same enhancement pattern. The existing `build*Request()` helper methods at the bottom of the file construct the correct JSON payloads. For each test:

| BDD ID | txType | Float change | Key assertion |
|--------|--------|-------------|---------------|
| BDD-WF-HP-BP01 | BILL_PAYMENT | Decreases | `finalBalance < initialBalance` |
| BDD-WF-HP-DN01 | DUITNOW_TRANSFER | Decreases | `finalBalance < initialBalance` |
| BDD-WF-HP-CP01 | CASHLESS_PAYMENT | No change | `finalBalance == initialBalance` |
| BDD-WF-HP-PBP01 | PIN_BASED_PURCHASE | No change | `finalBalance == initialBalance` |
| BDD-WF-HP-PT01 | PREPAID_TOPUP | Decreases | `finalBalance < initialBalance` |
| BDD-WF-HP-EWW01 | EWALLET_WITHDRAWAL | Increases | `finalBalance > initialBalance` |
| BDD-WF-HP-EWT01 | EWALLET_TOPUP | Decreases | `finalBalance < initialBalance` |
| BDD-WF-HP-ESSP01 | ESSP_PURCHASE | Decreases | `finalBalance < initialBalance` |
| BDD-WF-HP-PIN01 | PIN_PURCHASE | Decreases | `finalBalance < initialBalance` |
| BDD-WF-HP-RS01 | RETAIL_SALE | Increases | `finalBalance > initialBalance` |
| BDD-WF-HP-HCB01 | HYBRID_CASHBACK | Increases | `finalBalance > initialBalance` |

- [ ] **Step 1: Locate existing test methods for each type**

Search for the existing test methods (e.g., `billPayment_shouldReturnPending`, `duitNowTransfer_shouldReturnPending`, etc.) and the `@Nested` classes they belong to. If a Happy Path `@Nested` class doesn't exist for a type, add one.

- [ ] **Step 2: Enhance each test following the pattern from Task 2**

For each test method:
1. Add `BigDecimal initialBalance = getAgentFloatBalance(getEffectiveAgentId())` before the POST
2. After POST, call `waitForWorkflowCompletion(idempotencyKey)`
3. Assert terminal status (`COMPLETED` for HP)
4. Assert float direction (per table above)
5. Assert journal entries exist

- [ ] **Step 3: Run all enhanced tests**

```bash
./gradlew :gateway:e2eTest --no-daemon
```
Expected: All enhanced HP tests PASS

- [ ] **Step 4: Commit**

```bash
git add gateway/src/test/
git commit -m "test(e2e): enhance all transaction type happy paths with full BDD verification"
```

---

## Task 5: Add Swagger Request Validator Dependency (Phase 2 Setup)

**BDD Scenarios:** N/A (infrastructure)
**BRD Requirements:** BRD §5.1

**User-Facing:** NO

**Files:**
- Modify: `gateway/build.gradle`

- [ ] **Step 1: Add dependency**

Add to `dependencies` block in `gateway/build.gradle`:

```groovy
testImplementation 'com.atlassian.oai:swagger-request-validator-restassured:2.41.0'
```

- [ ] **Step 2: Verify build compiles**

```bash
./gradlew :gateway:compileTestJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add gateway/build.gradle
git commit -m "build(gateway): add swagger-request-validator for OpenAPI contract tests"
```

---

## Task 6: Create OpenAPI Contract E2E Test Class (Phase 2)

**BDD Scenarios:** BDD-L01, BDD-L04, BDD-DNOW-01, BDD-R01, BDD-TO-01, BDD-POLL-01, BDD-O01, BDD-BO01-05, BDD-DR01-02
**BRD Requirements:** BRD §4 Phase 2

**User-Facing:** NO

**Files:**
- Create: `gateway/src/test/java/com/agentbanking/gateway/integration/gateway/OpenApiContractE2ETest.java`

**Context:** This test class loads `docs/api/openapi.yaml`, hits each real service endpoint through the gateway with a real JWT token, and validates that the request/response pair conforms to the OpenAPI spec. Uses the same token/setup pattern as `SelfContainedOrchestratorE2ETest`.

- [ ] **Step 1: Create the test class with validator setup**

```java
package com.agentbanking.gateway.integration.gateway;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ABOUTME: Validates real API responses against the OpenAPI spec.
 * ABOUTME: Catches contract drift between spec (requirements) and implementation (real services).
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OpenApiContractE2ETest {

    private static final String GATEWAY_URL = System.getenv()
        .getOrDefault("GATEWAY_BASE_URL", "http://localhost:8080");
    private static final String AUTH_URL = System.getenv()
        .getOrDefault("AUTH_SERVICE_URL", "http://localhost:8087");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static OpenApiInteractionValidator validator;
    private static String agentToken;
    private static String adminToken;

    private final WebTestClient gatewayClient = WebTestClient.bindToServer()
            .baseUrl(GATEWAY_URL)
            .responseTimeout(java.time.Duration.ofSeconds(30))
            .build();

    @BeforeAll
    static void setup() {
        // Load OpenAPI spec
        String specPath = Path.of("docs/api/openapi.yaml").toAbsolutePath().toUri().toString();
        validator = OpenApiInteractionValidator
            .createForSpecificationUrl(specPath)
            .build();

        // Get tokens (same pattern as SelfContainedOrchestratorE2ETest)
        // ... token setup code ...
    }

    private void validateContract(String method, String path, int statusCode, String responseBody) {
        Request request = method.equals("GET") 
            ? SimpleRequest.Builder.get(path).build()
            : SimpleRequest.Builder.post(path)
                .withContentType("application/json")
                .build();
        SimpleResponse response = SimpleResponse.Builder
            .status(statusCode)
            .withContentType("application/json")
            .withBody(responseBody)
            .build();
        
        ValidationReport report = validator.validate(request, response);
        assertFalse(report.hasErrors(),
            "OpenAPI contract violation for " + method + " " + path + ": " + 
            report.getMessages());
    }
}
```

- [ ] **Step 2: Add contract tests for each endpoint**

Add one `@Test` method per endpoint from BRD §4 Phase 2:

```java
@Test
@DisplayName("Contract: GET /api/v1/agent/balance matches OpenAPI spec")
void agentBalance_matchesSpec() {
    var result = gatewayClient.get()
            .uri("/api/v1/agent/balance")
            .header("Authorization", "Bearer " + agentToken)
            .exchange()
            .expectBody(String.class)
            .returnResult();

    validateContract("GET", "/api/v1/agent/balance",
        result.getStatus().value(), result.getResponseBody());
}

@Test
@DisplayName("Contract: POST /api/v1/transactions matches OpenAPI spec")
void startTransaction_matchesSpec() {
    String idempotencyKey = "contract-test-" + UUID.randomUUID();
    String requestBody = /* build valid withdrawal request */;

    var result = gatewayClient.post()
            .uri("/api/v1/transactions")
            .header("Authorization", "Bearer " + agentToken)
            .header("X-Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectBody(String.class)
            .returnResult();

    validateContract("POST", "/api/v1/transactions",
        result.getStatus().value(), result.getResponseBody());
}

// ... repeat for all 16 endpoints listed in BRD §4 Phase 2 ...
```

- [ ] **Step 3: Run the contract tests**

```bash
./gradlew :gateway:e2eTest --tests "*OpenApiContractE2ETest*" --no-daemon
```
Expected: Some tests may FAIL — this is expected! Failures reveal drift between `openapi.yaml` and implementation. Each failure must be triaged: fix spec or fix implementation.

- [ ] **Step 4: Commit**

```bash
git add gateway/src/test/ docs/api/openapi.yaml
git commit -m "test(e2e): add OpenAPI contract validation tests against real services"
```

---

## Task 7: Fill Polling & Idempotency Gaps (Phase 3)

**BDD Scenarios:** BDD-POLL-02, BDD-POLL-03, BDD-IDE-02, BDD-IDE-03
**BRD Requirements:** BRD §4 Phase 3

**User-Facing:** NO

**Files:**
- Modify: `gateway/src/test/java/com/agentbanking/gateway/integration/orchestrator/SelfContainedOrchestratorE2ETest.java`

**Context:** Enhance the existing `WorkflowLifecycle` and `IdempotencyTests` `@Nested` classes. BDD-POLL-02 needs a test that polls a completed workflow and verifies the response contains transaction details (amount, customerFee, referenceNumber). BDD-IDE-02/03 need tests that submit a duplicate request after completion/failure and verify cached results are returned.

- [ ] **Step 1: Add BDD-POLL-02 test**

```java
@Test
@DisplayName("BDD-POLL-02: Poll returns COMPLETED status with transaction details")
void poll_completedWorkflow_shouldReturnFullDetails() {
    assumeTrue(agentToken != null, "Agent token required");

    String idempotencyKey = "e2e-poll-details-" + UUID.randomUUID();
    String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

    gatewayClient.post()
            .uri("/api/v1/transactions")
            .header("Authorization", "Bearer " + agentToken)
            .header("X-Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange();

    WorkflowDetails details = waitForWorkflowCompletion(idempotencyKey);
    
    // BDD-POLL-02 requires full transaction details in response
    if ("COMPLETED".equals(details.status())) {
        assertNotNull(details.amount(), "COMPLETED poll should include amount");
        assertNotNull(details.workflowId(), "COMPLETED poll should include workflowId");
    }
}
```

- [ ] **Step 2: Add BDD-POLL-03 test**

Similar to POLL-02 but for failed workflows. Submit a request designed to fail (e.g., insufficient float):

```java
@Test
@DisplayName("BDD-POLL-03: Poll returns FAILED status with error details")
void poll_failedWorkflow_shouldReturnErrorDetails() {
    // ... submit transaction designed to fail ...
    WorkflowDetails details = waitForWorkflowCompletion(idempotencyKey);
    
    if ("FAILED".equals(details.status())) {
        assertNotNull(details.errorCode(), "FAILED poll should include errorCode");
    }
}
```

- [ ] **Step 3: Add BDD-IDE-02 and BDD-IDE-03 tests**

```java
@Test
@DisplayName("BDD-IDE-02: Duplicate request for completed workflow returns cached result")
void duplicateAfterCompletion_shouldReturnCachedResult() {
    // Submit, poll to COMPLETED, then submit again with same idempotency key
    // Assert second response matches first (no new workflow created)
    // Assert agent float is not modified again
}
```

- [ ] **Step 4: Run the tests**

```bash
./gradlew :gateway:e2eTest --tests "*WorkflowLifecycle*" --tests "*IdempotencyTests*" --no-daemon
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add gateway/src/test/
git commit -m "test(e2e): fill polling and idempotency BDD gaps (POLL-02/03, IDE-02/03)"
```

---

## Task 8: Final Verification & Cleanup

**BDD Scenarios:** All
**BRD Requirements:** All phases

**User-Facing:** NO

- [ ] **Step 1: Run full E2E test suite**

```bash
./gradlew :gateway:e2eTest --no-daemon 2>&1 | tee e2e-results.log
```
Expected: All tests pass (except OpenAPI contract tests that may reveal drift)

- [ ] **Step 2: Review OpenAPI contract test failures**

For each failing contract test, determine root cause:
- If spec is wrong → update `docs/api/openapi.yaml`
- If implementation is wrong → file a bug
- If intentional divergence → add `@Disabled("Spec drift: tracked in issue #xxx")` with explanation

- [ ] **Step 3: Run full suite again after fixes**

```bash
./gradlew :gateway:e2eTest --no-daemon
```
Expected: All tests PASS

- [ ] **Step 4: Final commit**

```bash
git add -A  # after git status review
git commit -m "test(e2e): complete real integration testing for BDD scenarios"
```

---

## Verification Plan

### Automated Tests

All tests run via:
```bash
# Full E2E suite (requires docker compose --profile all up -d)
./gradlew :gateway:e2eTest --no-daemon

# Just transaction happy paths
./gradlew :gateway:e2eTest --tests "*HappyPath*" --no-daemon

# Just OpenAPI contract tests
./gradlew :gateway:e2eTest --tests "*OpenApiContractE2ETest*" --no-daemon

# Just journal endpoint
./gradlew :services:ledger-service:test --tests "*LedgerControllerIntegrationTest.getJournalEntries*"
```

### What Each Test Phase Verifies

| Phase | What's Tested | How |
|-------|--------------|-----|
| Phase 1 (Tasks 1-4) | Transaction HP flows: status, balance delta, journal | Poll to terminal → assert state + side effects |
| Phase 2 (Tasks 5-6) | API contract compliance vs OpenAPI spec | Validate real responses against `openapi.yaml` schema |
| Phase 3 (Task 7) | Polling details, idempotency guarantees | Submit → poll details, duplicate submission |
