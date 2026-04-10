# Code Review Issues Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all critical and important code review issues from latest commit

**Architecture:** Fix database migration, align enums, add transactionality, remove build artifacts, refactor duplicate code, add null checks, add gateway tests

**Tech Stack:** Java 21, Spring Boot, Flyway, PostgreSQL, WireMock

---

## Current State Analysis

- V1 migration incorrectly modified to add `reference_number` column
- AgentTier enum mismatch: Onboarding has 5 values, Ledger has 3 (correct)
- Missing @Transactional on financial methods in LedgerService
- Build artifacts committed to git
- Code duplication in LedgerService status update methods
- Geofence threshold changed from 100m to 1m (should be 10m)
- Switch response unchecked cast risk
- Gateway integration tests missing coverage for many external APIs

---

## Task 1: Fix V1 Migration - Create V8 Instead

**Files:**
- Create: `services/ledger-service/src/main/resources/db/migration/ledger/V8__add_reference_number.sql`

- [ ] **Step 1: Create V8 migration to add reference_number column**

```sql
-- Add reference_number column to ledger_transaction table
ALTER TABLE ledger_transaction ADD COLUMN IF NOT EXISTS reference_number VARCHAR(50);
```

- [ ] **Step 2: Commit**

```bash
git add services/ledger-service/src/main/resources/db/migration/ledger/V8__add_reference_number.sql
git commit -m "fix: add V8 migration for reference_number instead of modifying V1"
```

---

## Task 2: Fix AgentTier Enum Inconsistency

**Files:**
- Modify: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/model/AgentTier.java`

**Analysis:** Ledger has 3 values (MICRO, STANDARD, PREMIER) which is correct. Onboarding has 5 values - need to remove PREMIUM and BASIC to match.

- [ ] **Step 1: Update Onboarding AgentTier to match Ledger (remove PREMIUM, BASIC)**

```java
package com.agentbanking.onboarding.domain.model;

public enum AgentTier {
    MICRO,
    STANDARD,
    PREMIER
}
```

- [ ] **Step 2: Search for any usage of PREMIUM or BASIC in codebase**

```bash
grep -r "PREMIUM\|BASIC" services/ --include="*.java" | grep -v "test"
```

- [ ] **Step 3: Update any code that uses the removed values**

If found, update to use nearest valid tier (e.g., PREMIUM → PREMIER, BASIC → MICRO).

- [ ] **Step 4: Commit**

```bash
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/model/AgentTier.java
git commit -m "fix: align AgentTier enum with ledger (remove PREMIUM, BASIC)"
```

---

## Task 3: Add @Transactional to Financial Methods

**Files:**
- Modify: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/LedgerService.java:58-250`

- [ ] **Step 1: Add @Transactional import**

Check imports at top of LedgerService.java - add:
```java
import org.springframework.transaction.annotation.Transactional;
```

- [ ] **Step 2: Add @Transactional to processWithdrawal method**

Find method signature at line 58:
```java
@SuppressWarnings("unchecked")
public Map<String, Object> processWithdrawal(UUID agentId, BigDecimal amount, 
                                               BigDecimal customerFee, BigDecimal agentCommission,
                                               BigDecimal bankShare, String idempotencyKey,
                                               String customerCardMasked,
                                               BigDecimal geofenceLat, BigDecimal geofenceLng,
                                               String agentTier, String targetBin) {
```

Change to:
```java
@SuppressWarnings("unchecked")
@Transactional(propagation = Propagation.REQUIRES_NEW)
public Map<String, Object> processWithdrawal(UUID agentId, BigDecimal amount, 
                                               BigDecimal customerFee, BigDecimal agentCommission,
                                               BigDecimal bankShare, String idempotencyKey,
                                               String customerCardMasked,
                                               BigDecimal geofenceLat, BigDecimal geofenceLng,
                                               String agentTier, String targetBin) {
```

- [ ] **Step 3: Add @Transactional to processDeposit method**

Find method at line 251 - add same annotation.

- [ ] **Step 4: Commit**

```bash
git add services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/LedgerService.java
git commit -m "fix: add @Transactional to financial methods (processWithdrawal, processDeposit)"
```

---

## Task 4: Remove Build Artifacts from Git

**Files:**
- Delete: `build.log`, `dependencies.log`, `classmap.log`

- [ ] **Step 1: Remove build artifacts from git**

```bash
git rm build.log dependencies.log classmap.log
```

- [ ] **Step 2: Check .gitignore for build artifacts**

```bash
grep -E "build\.log|dependencies\.log|classmap\.log" .gitignore || echo "Not found"
```

- [ ] **Step 3: Add to .gitignore if not present**

```bash
echo -e "\n# Build artifacts\nbuild.log\ndependencies.log\nclassmap.log" >> .gitignore
```

- [ ] **Step 4: Commit**

```bash
git add .gitignore
git commit -m "fix: remove build artifacts from git, add to .gitignore"
```

---

## Task 5: Extract Duplicate Code in LedgerService

**Files:**
- Modify: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/LedgerService.java`

- [ ] **Step 1: Identify duplicate code pattern**

The duplicate pattern is in processWithdrawal and processDeposit where status is updated from PENDING to COMPLETED/FAILED - creating new TransactionRecord with copied fields.

- [ ] **Step 2: Add helper method after processDeposit**

Add this method after processDeposit (around line 400):

```java
private Map<String, Object> updateTransactionStatus(
            TransactionRecord transaction, String newStatus, 
            Map<String, Object> switchResponse, BigDecimal customerFee, 
            BigDecimal agentCommission, BigDecimal bankShare) {
        
        String switchRef = switchResponse != null ? (String) switchResponse.get("responseMessage") : null;
        String errorCode = "FAILED".equals(newStatus) ? (String) switchResponse.get("responseCode") : null;
        
        TransactionRecord updated = new TransactionRecord(
            transaction.transactionId(),
            transaction.idempotencyKey(),
            transaction.agentId(),
            transaction.transactionType(),
            transaction.amount(),
            customerFee,
            agentCommission,
            bankShare,
            newStatus,
            errorCode,
            transaction.customerMykad(),
            transaction.customerCardMasked(),
            switchRef,
            transaction.referenceNumber(),
            transaction.geofenceLat(),
            transaction.geofenceLng(),
            transaction.createdAt(),
            LocalDateTime.now()
        );
        
        transactionRepository.save(updated);
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", newStatus);
        result.put("switchReference", switchRef);
        result.put("errorCode", errorCode);
        result.put("transactionId", transaction.transactionId());
        result.put("completedAt", LocalDateTime.now().toString());
        
        return result;
    }
```

- [ ] **Step 3: Refactor processWithdrawal to use helper**

Replace the duplicate code blocks with calls to updateTransactionStatus().

- [ ] **Step 4: Commit**

```bash
git add services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/LedgerService.java
git commit -m "refactor: extract duplicate status update code to helper method"
```

---

## Task 6: Fix Switch Response Null Check

**Files:**
- Modify: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/LedgerService.java`

- [ ] **Step 1: Find the unchecked cast around line 127**

```bash
sed -n '120,140p' services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/LedgerService.java
```

- [ ] **Step 2: Add null-safe retrieval**

Find the line:
```java
String responseCode = (String) switchResponse.get("responseCode");
```

Change to:
```java
String responseCode = switchResponse.get("responseCode") != null 
    ? switchResponse.get("responseCode").toString() 
    : "UNKNOWN";
```

- [ ] **Step 3: Commit**

```bash
git add services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/LedgerService.java
git commit -m "fix: add null-safe switch response retrieval to prevent NPE"
```

---

## Task 7: Fix Geofence Threshold to 10 meters

**Files:**
- Modify: `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/LedgerService.java:96`

- [ ] **Step 1: Find and update geofence threshold**

```bash
grep -n "isWithinGeofence" services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/LedgerService.java
```

- [ ] **Step 2: Change threshold from 1.0 to 10.0 meters**

Find the line with `1.0` and change to `10.0`:
```java
if (!GeofenceChecker.isWithinGeofence(
        agentFloat.merchantGpsLat(), agentFloat.merchantGpsLng(),
        geofenceLat, geofenceLng, 10.0)) {  // Changed from 1.0 to 10.0
```

- [ ] **Step 3: Commit**

```bash
git add services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/LedgerService.java
git commit -m "fix: set geofence threshold to 10 meters for practical use"
```

---

## Task 8: Document Deleted Tests (Skip - Intentional)

**Note:** User confirmed E2E test scripts were intentionally deleted as they were useless. No action needed.

- [ ] **Step 1: Mark as complete (no changes needed)**

The 18 shell scripts were removed intentionally. Skip this task.

---

## Task 9: Add Missing Gateway External API Integration Tests

**Files:**
- Modify: `gateway/src/test/java/com/agentbanking/gateway/integration/gateway/ExternalApiIntegrationTest.java`

**Coverage Required:** All external APIs except orchestrator (covered in E2E)

- [ ] **Step 1: Add tests for Onboarding verify-mykad**

```java
@Test
@DisplayName("Onboarding: Verify MyKad - /api/v1/onboarding/verify-mykad")
void verifyOnboardingVerifyMyKad() {
    wireMockServer.stubFor(post(urlPathEqualTo("/internal/verify-mykad"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"verificationId\":\"VER-123\",\"status\":\"VERIFIED\"}")));

    String token = generateToken("agent001", "ROLE_AGENT");
    
    webClient.post()
            .uri("/api/v1/onboarding/verify-mykad")
            .header("Authorization", "Bearer " + token)
            .bodyValue("{\"icNumber\":\"123456789012\",\"name\":\"Test User\"}")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.verificationId").isEqualTo("VER-123");
}
```

- [ ] **Step 2: Add tests for Onboarding kyc/biometric**

```java
@Test
@DisplayName("Onboarding: KYC Biometric - /api/v1/kyc/biometric")
void verifyOnboardingKycBiometric() {
    wireMockServer.stubFor(post(urlPathEqualTo("/internal/biometric"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"biometricId\":\"BIO-123\",\"status\":\"MATCHED\"}")));

    String token = generateToken("agent001", "ROLE_AGENT");
    
    webClient.post()
            .uri("/api/v1/kyc/biometric")
            .header("Authorization", "Bearer " + token)
            .bodyValue("{\"fingerprintData\":\"encodedData\"}")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.biometricId").isEqualTo("BIO-123");
}
```

- [ ] **Step 3: Add tests for Compliance status**

```java
@Test
@DisplayName("Onboarding: Compliance Status - /api/v1/compliance/status")
void verifyComplianceStatus() {
    wireMockServer.stubFor(get(urlPathEqualTo("/internal/compliance/status"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"compliant\":true,\"riskLevel\":\"LOW\"}")));

    String token = generateToken("agent001", "ROLE_AGENT");
    
    webClient.get()
            .uri("/api/v1/compliance/status?agentId=123")
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.compliant").isEqualTo(true);
}
```

- [ ] **Step 4: Add tests for Transaction Quote**

```java
@Test
@DisplayName("Rules: Transaction Quote - /api/v1/transactions/quote")
void verifyTransactionQuote() {
    wireMockServer.stubFor(post(urlPathEqualTo("/internal/transactions/quote"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"quoteId\":\"Q-123\",\"totalAmount\":105.00}")));

    String token = generateToken("agent001", "ROLE_AGENT");
    
    webClient.post()
            .uri("/api/v1/transactions/quote")
            .header("Authorization", "Bearer " + token)
            .bodyValue("{\"transactionType\":\"CASH_WITHDRAWAL\",\"amount\":100.00}")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.quoteId").isEqualTo("Q-123");
}
```

- [ ] **Step 5: Add tests for Backoffice Ledger Transactions**

```java
@Test
@DisplayName("Backoffice: Ledger Transactions - /api/v1/backoffice/ledger-transactions")
void verifyBackofficeLedgerTransactions() {
    wireMockServer.stubFor(post(urlPathEqualTo("/internal/backoffice/ledger-transactions"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"transactions\":[],\"total\":0}")));

    String token = generateToken("admin001", "ROLE_ADMIN");
    
    webClient.post()
            .uri("/api/v1/backoffice/ledger-transactions")
            .header("Authorization", "Bearer " + token)
            .bodyValue("{\"page\":0,\"size\":10}")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.total").isEqualTo(0);
}
```

- [ ] **Step 6: Add tests for Backoffice Dashboard**

```java
@Test
@DisplayName("Backoffice: Dashboard - /api/v1/backoffice/dashboard")
void verifyBackofficeDashboard() {
    wireMockServer.stubFor(get(urlPathEqualTo("/internal/backoffice/dashboard"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"totalAgents\":100,\"totalTransactions\":5000}")));

    String token = generateToken("admin001", "ROLE_ADMIN");
    
    webClient.get()
            .uri("/api/v1/backoffice/dashboard")
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.totalAgents").isEqualTo(100);
}
```

- [ ] **Step 7: Add tests for Backoffice Agents List**

```java
@Test
@DisplayName("Backoffice: Agents List - /api/v1/backoffice/agents")
void verifyBackofficeAgentsList() {
    wireMockServer.stubFor(get(urlPathEqualTo("/backoffice/agents"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"agents\":[],\"total\":0}")));

    String token = generateToken("admin001", "ROLE_ADMIN");
    
    webClient.get()
            .uri("/api/v1/backoffice/agents")
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.total").isEqualTo(0);
}
```

- [ ] **Step 8: Add tests for Backoffice Settlement**

```java
@Test
@DisplayName("Backoffice: Settlement - /api/v1/backoffice/settlement")
void verifyBackofficeSettlement() {
    wireMockServer.stubFor(get(urlPathEqualTo("/internal/backoffice/settlement"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"settlements\":[],\"total\":0}")));

    String token = generateToken("admin001", "ROLE_ADMIN");
    
    webClient.get()
            .uri("/api/v1/backoffice/settlement")
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.total").isEqualTo(0);
}
```

- [ ] **Step 9: Add tests for Backoffice KYC Review Queue**

```java
@Test
@DisplayName("Backoffice: KYC Review Queue - /api/v1/backoffice/kyc/review-queue")
void verifyBackofficeKycReviewQueue() {
    wireMockServer.stubFor(get(urlPathEqualTo("/internal/kyc/review-queue"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"applications\":[],\"total\":0}")));

    String token = generateToken("admin001", "ROLE_ADMIN");
    
    webClient.get()
            .uri("/api/v1/backoffice/kyc/review-queue")
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.total").isEqualTo(0);
}
```

- [ ] **Step 10: Add tests for Backoffice Discrepancy Maker Action**

```java
@Test
@DisplayName("Backoffice: Discrepancy Maker Action - /api/v1/backoffice/discrepancy/*/maker-action")
void verifyBackofficeDiscrepancyMaker() {
    wireMockServer.stubFor(post(urlPathEqualTo("/internal/reconciliation/discrepancy/maker-propose"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"caseId\":\"DISC-123\",\"status\":\"PROPOSED\"}")));

    String token = generateToken("maker001", "ROLE_MAKER");
    
    webClient.post()
            .uri("/api/v1/backoffice/discrepancy/DISC-123/maker-action")
            .header("Authorization", "Bearer " + token)
            .bodyValue("{\"action\":\"PROPOSE\",\"amount\":100.00}")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("PROPOSED");
}
```

- [ ] **Step 11: Add tests for Backoffice Discrepancy Checker Approve**

```java
@Test
@DisplayName("Backoffice: Discrepancy Checker Approve - /api/v1/backoffice/discrepancy/*/checker-approve")
void verifyBackofficeDiscrepancyCheckerApprove() {
    wireMockServer.stubFor(post(urlPathEqualTo("/internal/reconciliation/discrepancy/checker-approve"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"caseId\":\"DISC-123\",\"status\":\"APPROVED\"}")));

    String token = generateToken("checker001", "ROLE_CHECKER");
    
    webClient.post()
            .uri("/api/v1/backoffice/discrepancy/DISC-123/checker-approve")
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("APPROVED");
}
```

- [ ] **Step 12: Add tests for Backoffice Discrepancy Checker Reject**

```java
@Test
@DisplayName("Backoffice: Discrepancy Checker Reject - /api/v1/backoffice/discrepancy/*/checker-reject")
void verifyBackofficeDiscrepancyCheckerReject() {
    wireMockServer.stubFor(post(urlPathEqualTo("/internal/reconciliation/discrepancy/checker-reject"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"caseId\":\"DISC-123\",\"status\":\"REJECTED\"}")));

    String token = generateToken("checker001", "ROLE_CHECKER");
    
    webClient.post()
            .uri("/api/v1/backoffice/discrepancy/DISC-123/checker-reject")
            .header("Authorization", "Bearer " + token)
            .bodyValue("{\"reason\":\"Invalid documentation\"}")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("REJECTED");
}
```

- [ ] **Step 13: Add tests for Backoffice Audit Logs**

```java
@Test
@DisplayName("Backoffice: Audit Logs - /api/v1/backoffice/audit-logs")
void verifyBackofficeAuditLogs() {
    wireMockServer.stubFor(get(urlPathEqualTo("/auth/audit/logs"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"logs\":[],\"total\":0}")));

    String token = generateToken("admin001", "ROLE_ADMIN");
    
    webClient.get()
            .uri("/api/v1/backoffice/audit-logs?page=0&size=10")
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.total").isEqualTo(0);
}
```

- [ ] **Step 14: Add tests for Backoffice Admin Users**

```java
@Test
@DisplayName("Backoffice: Admin Users - /api/v1/backoffice/admin/users")
void verifyBackofficeAdminUsers() {
    wireMockServer.stubFor(get(urlPathEqualTo("/auth/users"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"users\":[],\"total\":0}")));

    String token = generateToken("admin001", "ROLE_ADMIN");
    
    webClient.get()
            .uri("/api/v1/backoffice/admin/users")
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.total").isEqualTo(0);
}
```

- [ ] **Step 15: Add tests for Backoffice Admin Roles**

```java
@Test
@DisplayName("Backoffice: Admin Roles - /api/v1/backoffice/admin/roles")
void verifyBackofficeAdminRoles() {
    wireMockServer.stubFor(get(urlPathEqualTo("/auth/roles"))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"roles\":[]}")));

    String token = generateToken("admin001", "ROLE_ADMIN");
    
    webClient.get()
            .uri("/api/v1/backoffice/admin/roles")
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.roles").isArray();
}
```

- [ ] **Step 16: Add tests for Admin Health All**

```java
@Test
@DisplayName("Admin: Health All - /api/v1/admin/health/all")
void verifyAdminHealthAll() {
    webClient.get()
            .uri("/api/v1/admin/health/all")
            .header("Authorization", "Bearer " + generateToken("admin001", "ROLE_ADMIN"))
            .exchange()
            .expectStatus().isOk();
}
```

- [ ] **Step 17: Add tests for Admin Health Service**

```java
@Test
@DisplayName("Admin: Health Service - /api/v1/admin/health/{service}")
void verifyAdminHealthService() {
    webClient.get()
            .uri("/api/v1/admin/health/ledger")
            .header("Authorization", "Bearer " + generateToken("admin001", "ROLE_ADMIN"))
            .exchange()
            .expectStatus().isOk();
}
```

- [ ] **Step 18: Add tests for Admin Metrics**

```java
@Test
@DisplayName("Admin: Metrics - /api/v1/admin/metrics/{service}")
void verifyAdminMetrics() {
    webClient.get()
            .uri("/api/v1/admin/metrics/ledger")
            .header("Authorization", "Bearer " + generateToken("admin001", "ROLE_ADMIN"))
            .exchange()
            .expectStatus().isOk();
}
```

- [ ] **Step 19: Commit**

```bash
git add gateway/src/test/java/com/agentbanking/gateway/integration/gateway/ExternalApiIntegrationTest.java
git commit -m "test: add 18 new gateway integration tests for external APIs"
```

---

## Task 10: Run Tests and Verify Build

**Files:**
- Run: `./gradlew test` in ledger-service and gateway

- [ ] **Step 1: Run ledger-service tests**

```bash
cd services/ledger-service && ../gradlew test
```

- [ ] **Step 2: Run gateway integration tests**

```bash
cd gateway && ../gradlew test
```

- [ ] **Step 3: Verify build passes**

```bash
cd services/ledger-service && ../gradlew build
cd gateway && ../gradlew build
```

- [ ] **Step 4: Commit final verification**

```bash
git commit --allow-empty -m "chore: verify all tests pass after fixes"
```

---

## Plan Summary

| Task | Description | Severity |
|------|-------------|----------|
| 1 | Fix V1 migration - create V8 instead | Critical |
| 2 | Fix AgentTier enum (remove PREMIUM, BASIC from onboarding) | Critical |
| 3 | Add @Transactional to financial methods | Critical |
| 4 | Remove build artifacts from git | Critical |
| 5 | Extract duplicate code in LedgerService | Important |
| 6 | Fix switch response null check | Important |
| 7 | Fix geofence threshold to 10 meters | Important |
| 8 | Document deleted tests (skipped - intentional) | - |
| 9 | Add 18 new gateway integration tests | Important |
| 10 | Run tests and verify build | Verification |

**Plan complete and saved to `docs/superpowers/plans/2026-04-10-code-review-fixes.md`.**

Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?