# Business Requirements Document: Real Integration Testing for BDD Scenarios

**Version:** 2.0  
**Date:** 2026-04-18  
**Status:** Draft — Pending Review  
**Module:** Testing Infrastructure

---

## 1. Business Goals & Success Criteria

### Business Goals

1. Ensure all transaction and related APIs are tested with **real backend services** (no mocks)
2. Close the BDD coverage gap: most BDD categories have 0% side-effect verification
3. Eliminate "false confidence" from tests that pass but verify only HTTP 202/PENDING
4. Verify `ExternalApiIntegrationTest` tests against real backends, not fake WireMock stubs
5. Provide full traceability from BDD scenario → E2E test method

### Success Criteria

- **100% of Happy Path BDD scenarios** have E2E tests verifying: HTTP response + TransactionRecord state + AgentFloat delta + JournalEntries
- All gateway API contracts tested against real backends (not WireMock)
- Tests run against docker compose services via `./gradlew :gateway:e2eTest`
- No `@MockBean` or WireMock used in E2E tests — only real service calls

---

## 2. Current State Audit

### 2.1 Existing E2E Tests: `SelfContainedOrchestratorE2ETest`

Tests exist for the following BDD scenarios, **but only verify HTTP response (202/PENDING)**. None poll to completion or verify side effects:

| BDD ID | Test Method | Verifies | Gaps |
|--------|-------------|----------|------|
| BDD-TO-01 | `withdraw_offUs_shouldReturnPending()` | HTTP 202 + status=PENDING | No poll, no balance check, no journal |
| BDD-TO-02 | `withdraw_onUs_shouldReturnPending()` | HTTP 202 + status=PENDING | Same |
| BDD-TO-03 | `deposit_shouldReturnPending()` | HTTP 202 + status=PENDING | Same |
| BDD-TO-04 | `billPayment_shouldReturnPending()` | HTTP 202 + status=PENDING | Same |
| BDD-TO-05 | `duitNowTransfer_shouldReturnPending()` | HTTP 202 + status=PENDING | Same |
| BDD-TO-06 | `startTransaction_unsupportedType_shouldReturnError()` | HTTP 400 | ✅ Adequate |
| BDD-TO-07→15 | `cashlessPayment_shouldReturnPending()` ... `hybridCashback_shouldReturnPending()` | HTTP 202 | Same — dispatch only |
| BDD-POLL-01 | `poll_pendingWorkflow_shouldReturnPending()` | HTTP 200 + PENDING | Doesn't test COMPLETED |
| BDD-POLL-04 | `poll_nonexistentWorkflow_shouldReturn404()` | HTTP 404 | ✅ Adequate |
| BDD-IDE-01 | `duplicateStart_shouldReturnExistingStatus()` | Returns existing | No float verification |
| BDD-WF-01 | `startWorkflow_validRequest_shouldReturnPending()` | Status=PENDING | Same |
| BDD-WF-02 | `poll_completedWorkflow_shouldReturnCompleted()` | Status=COMPLETED | No balance/journal verify |
| BDD-WF-03 | `poll_failedWorkflow_shouldReturnFailed()` | Status=FAILED | No balance verify |

### 2.2 Existing Contract Tests: `ExternalApiIntegrationTest`

> [!CAUTION]
> This test uses **WireMock** exclusively — it stubs all backend service responses and only verifies that the **gateway routes requests correctly** and returns the stubbed response. It does NOT verify:
> - Actual API contract shape from real services
> - Real response codes
> - Real error handling
> - Actual backend behavior

**Endpoints tested (all with WireMock stubs):**
- `GET /api/v1/transfer/proxy/enquiry` → stubs orchestrator
- `POST /api/v1/balance-inquiry` → stubs ledger  
- `GET /api/v1/transactions/quote` → stubs rules
- `POST /api/v1/onboarding/application` → stubs onboarding
- `GET /api/v1/agent/balance` → stubs ledger
- `POST /api/v1/ekyc/verify-mykad` → stubs onboarding
- `POST /api/v1/ekyc/biometric` → stubs onboarding
- `GET /api/v1/compliance/status` → stubs onboarding
- `GET /api/v1/backoffice/*` (dashboard, agents, settlement, transactions, audit-logs) → stubs services
- `POST /api/v1/backoffice/discrepancy/*` (maker-propose, checker-approve, checker-reject) → stubs services
- `GET /api/v1/kyc/review-queue` → stubs onboarding

**Verdict:** `ExternalApiIntegrationTest` tests gateway routing logic in isolation — useful but does NOT verify real API contracts. Must be supplemented with real-backend E2E tests for the same endpoints.

### 2.3 Total BDD Scenario Counts

| Source | HP | EC | Total |
|--------|----|----|-------|
| Original BDD (2026-03-25) | 58 | 62 | 120 |
| Transaction Orchestrator Addendum (2026-04-05) | 23 | 32 | 55 |
| Missing Transaction Types Addendum (2026-04-06) | 26 | 22 | 48 |
| **Grand Total** | **107** | **116** | **223** |

---

## 3. Coverage Gap Analysis — BDD Categories NOT Covered by E2E Tests

### What IS covered (partially)
- BDD-TO (dispatch) — 15 scenarios, HTTP 202 only
- BDD-POLL — 2 of 5 scenarios
- BDD-IDE — 1 of 3 scenarios  
- BDD-WF (lifecycle) — 3 of 6 scenarios

### What is NOT covered at all (0% E2E coverage)

| Category | Scenarios | Description |
|----------|-----------|-------------|
| **BDD-R** (Rules/Fees) | 14 | Fee calc, limits, velocity |
| **BDD-L** (Ledger/Float) | 14 | Balance, journal, settlement |
| **BDD-W** (Cash Withdrawal) | 12 | Full withdrawal flows |
| **BDD-D** (Cash Deposit) | 9 | Full deposit flows |
| **BDD-O** (eKYC/Onboarding) | 14 | KYC verification flows |
| **BDD-B** (Bill Payments) | 8 | Bill payment flows |
| **BDD-T** (Prepaid Top-up) | 5 | Telco top-up flows |
| **BDD-DNOW** (DuitNow) | 8 | DuitNow transfer flows |
| **BDD-WAL/ESSP** (eWallet/eSSP) | 8 | eWallet/eSSP flows |
| **BDD-A** (Agent Onboarding) | 6 | Agent STP/non-STP flows |
| **BDD-V** (Reversals) | 10 | Automatic/manual reversals |
| **BDD-M** (Merchant) | 10 | Retail/PIN/Cashback |
| **BDD-EFM** (AML/Fraud) | 7 | Velocity engine, smurfing |
| **BDD-SM** (Settlement) | 12 | EOD settlement, reconciliation |
| **BDD-DR** (Discrepancy) | 8 | Ghost/orphan/mismatch resolution |
| **BDD-G** (Gateway) | 6 | Auth, routing, error handling |
| **BDD-BO** (Backoffice) | 11 | CRUD, reports, dashboard |
| **BDD-S** (STP) | 6 | STP processing flows |
| **BDD-SR** (Safety Reversal) | 4 | Store & Forward |
| **BDD-HITL** (Human-in-Loop) | 3 | Force-resolve signals |
| **BDD-XWF** (Cross-Workflow) | 6 | Race conditions, compensation |
| **BDD-WF-HP** (Workflow HP) | 2 | Full happy path verification |
| **BDD-WF-EC** (Workflow EC) | 8 | Failure path verification |
| **BDD-WF-D/BP/DN** | 15 | Deposit/Bill/DuitNow workflow |
| **BDD-WF-CP/PBP/PT/EWW/EWT/ESSP/PIN/RS/HCB** | 36 | New transaction type workflows |

---

## 4. Scope — What To Test

### Phase 1: Transaction Happy Paths (Priority — Real Backend E2E)

> [!IMPORTANT]
> These are the tests Loc specifically needs. Full activity chain verification with real docker-compose services.

**Enhance existing `SelfContainedOrchestratorE2ETest` tests to verify completion:**

For each transaction type's Happy Path scenario, extend current tests to:
1. Record initial balance → `GET /internal/balance/{agentId}`
2. Submit transaction → `POST /api/v1/transactions` (already done — HTTP 202)
3. **NEW: Poll until terminal** → `GET /api/v1/transactions/{id}/status` (loop with timeout)
4. **NEW: Assert final status** = COMPLETED
5. **NEW: Verify TransactionRecord** via poll response (status, amount, fees, type)
6. **NEW: Verify AgentFloat delta** → `GET /internal/balance/{agentId}` (compare before/after)
7. **NEW: Verify JournalEntries** → `GET /internal/journal/{transactionId}` (new endpoint)

| BDD ID | Transaction Type | Float Direction | Float Delta |
|--------|-----------------|----------------|-------------|
| BDD-WF-HP-W01 | CASH_WITHDRAWAL (Off-Us) | Decreases | -amount - fees |
| BDD-WF-HP-W02 | CASH_WITHDRAWAL (On-Us) | Decreases | -amount |
| BDD-WF-HP-D01 | CASH_DEPOSIT | Increases | +amount |
| BDD-WF-HP-BP01 | BILL_PAYMENT | Decreases | -amount |
| BDD-WF-HP-DN01 | DUITNOW_TRANSFER | Decreases | -amount |
| BDD-WF-HP-CP01 | CASHLESS_PAYMENT | No change | 0 |
| BDD-WF-HP-PBP01 | PIN_BASED_PURCHASE | No change | 0 |
| BDD-WF-HP-PT01 | PREPAID_TOPUP | Decreases | -amount - fees |
| BDD-WF-HP-EWW01 | EWALLET_WITHDRAWAL | Increases | +amount + fees |
| BDD-WF-HP-EWT01 | EWALLET_TOPUP | Decreases | -amount - fees |
| BDD-WF-HP-ESSP01 | ESSP_PURCHASE | Decreases | -amount |
| BDD-WF-HP-PIN01 | PIN_PURCHASE | Decreases | -face value |
| BDD-WF-HP-RS01 | RETAIL_SALE | Increases | +net credit (after MDR) |
| BDD-WF-HP-HCB01 | HYBRID_CASHBACK | Increases | +sale net + cashback |

### Phase 2: OpenAPI-Validated API Contract E2E Tests

**Goal:** Verify that real backend responses match the OpenAPI spec at `docs/api/openapi.yaml`.

> [!IMPORTANT]
> The OpenAPI spec was composed from requirements and has **never been verified** against the actual implementation. This test catches drift in both directions:
> - Spec says field X exists → implementation doesn't return it → **spec is wrong or implementation is missing a field**
> - Implementation returns field Y → spec doesn't define it → **spec is incomplete**

**Approach:** Use [Atlassian swagger-request-validator](https://bitbucket.org/atlassian/swagger-request-validator) library:
1. Load `docs/api/openapi.yaml` as the contract source of truth
2. Hit each real service endpoint through the gateway with real JWT
3. Validate request + response pair against the spec schema automatically
4. Failures = contract drift that must be resolved (fix spec OR fix implementation)

**New dependency:** `com.atlassian.oai:swagger-request-validator-restassured` (test scope only)

**New test class:** `OpenApiContractE2ETest` extends `BaseIntegrationTest`

```java
// Pseudocode — loads spec once, validates every request/response
OpenApiInteractionValidator validator = OpenApiInteractionValidator
    .createForSpecificationUrl("docs/api/openapi.yaml")
    .build();

// Each test: hit real gateway → validate response against spec
Response response = gatewayGet("/api/v1/agent/balance", token);
ValidationReport report = validator.validate(request, response);
assertThat(report.hasErrors()).isFalse();
```

**Endpoints to test (same set as ExternalApiIntegrationTest but real):**

| Endpoint | OpenAPI operationId | BDD Reference |
|----------|-------------------|---------------|
| `GET /api/v1/agent/balance` | getAgentBalance | BDD-L01 |
| `POST /api/v1/balance-inquiry` | balanceInquiry | BDD-L04 |
| `GET /api/v1/transfer/proxy/enquiry` | proxyEnquiry | BDD-DNOW-01 |
| `POST /api/v1/transactions/quote` | getTransactionQuote | BDD-R01 |
| `POST /api/v1/transactions` | startTransaction | BDD-TO-01 |
| `GET /api/v1/transactions/{id}/status` | getTransactionStatus | BDD-POLL-01 |
| `POST /api/v1/onboarding/verify-mykad` | verifyMyKad | BDD-O01 |
| `POST /api/v1/onboarding/submit-application` | submitApplication | BDD-O01 |
| `GET /api/v1/compliance/status` | getComplianceStatus | BDD-EFM |
| `GET /api/v1/backoffice/dashboard` | getDashboard | BDD-BO02 |
| `GET /api/v1/backoffice/agents` | getAgents | BDD-BO01 |
| `GET /api/v1/backoffice/settlement` | getSettlement | BDD-BO03 |
| `GET /api/v1/backoffice/transactions` | getTransactions | BDD-BO02 |
| `GET /api/v1/backoffice/audit-logs` | getAuditLogs | BDD-BO05 |
| `POST /api/v1/backoffice/discrepancy/{id}/maker-action` | makerPropose | BDD-DR01 |
| `POST /api/v1/backoffice/discrepancy/{id}/checker-approve` | checkerApprove | BDD-DR02 |

**Outcome per test:**
- ✅ Pass = real response matches OpenAPI spec schema (field names, types, required fields, enums)
- ❌ Fail = drift detected → either fix `openapi.yaml` or fix the service implementation

**ExternalApiIntegrationTest disposition:** Keep as-is for fast gateway routing tests. `OpenApiContractE2ETest` supplements it with real-backend contract verification.

### Phase 3: Polling & Idempotency (Enhance Existing)

| BDD ID | Test | Enhancement |
|--------|------|-------------|
| BDD-POLL-01 | `poll_pendingWorkflow_shouldReturnPending()` | Already adequate |
| BDD-POLL-02 | NEW | Poll returns COMPLETED with transaction details |
| BDD-POLL-03 | NEW | Poll returns FAILED with error details |
| BDD-POLL-04 | Exists | ✅ Adequate |
| BDD-IDE-01 | Exists | Add float non-modification assertion |
| BDD-IDE-02 | NEW | Completed workflow returns cached result |
| BDD-IDE-03 | NEW | Failed workflow returns cached error |

### Out of Scope (for this iteration)

| Category | Reason |
|----------|--------|
| Per-service `BDD*IntegrationTest` stub files | Deferred to cleanup task |
| BDD-EFM (smurfing, geofence) | Requires simulated fraud patterns |
| BDD-SM (EOD settlement, reconciliation) | Requires EOD batch job execution |
| BDD-DR (discrepancy resolution) | Requires maker-checker workflow |
| BDD-V (manual reversals, disputes) | Requires backoffice UI interaction |
| BDD-HITL (human-in-the-loop signals) | Requires Temporal admin API |
| BDD-WF-04 (JVM crash recovery) | Infrastructure test, not functional |
| BDD-XWF (concurrent workflows) | Requires parallel test execution |
| BDD-A (agent onboarding STP) | Requires external API mocks (JPN, SSM) |
| BDD-O (eKYC flows) | Requires external API mocks (JPN, biometric device) |

---

## 5. Infrastructure Changes Required

### 5.1 Swagger Request Validator Dependency (Gateway Module)

**Change:** Add to `gateway/build.gradle`:
```groovy
testImplementation 'com.atlassian.oai:swagger-request-validator-restassured:2.41.0'
```
This library loads `openapi.yaml` and validates HTTP request/response pairs against the spec schema.

### 5.2 Journal Entry Endpoint (Ledger Service)

**Status:** `JournalEntryRepository.findByTransactionId(UUID)` exists in domain; `JournalEntryRecord` has fields: `journalId`, `transactionId`, `entryType`, `accountCode`, `amount`, `description`, `createdAt`. No REST endpoint exists.

**Change:** Add `GET /internal/journal/{transactionId}` to `LedgerController`.

### 5.2 Poll-Until-Terminal Utility

**Change:** Add `pollUntilTerminal(String workflowId, String token, Duration timeout)` to `BaseIntegrationTest`.

### 5.3 Balance Snapshot Utility

**Change:** Add `getBalance(UUID agentId)` helper to `BaseIntegrationTest` (may already exist — verify).

---

## 6. Expected Test Count

| Phase | New Tests | Enhanced Tests | Total |
|-------|-----------|---------------|-------|
| Phase 1: Transaction HP | 0 | 14 (enhance existing dispatch tests) | 14 |
| Phase 2: OpenAPI Contract | 16 (new class) | 0 | 16 |
| Phase 3: Poll & Idempotency | 4 | 2 | 6 |
| **Total** | **20** | **16** | **36** |

---

## 7. Constraints & Assumptions

- Tests require all docker compose services running
- Temporal must be running on port 7233
- Tests are tagged with `@Tag("e2e")` and run via `./gradlew :gateway:e2eTest`
- Test data cleanup via `./gradlew resetSystem`
- The existing poll endpoint returns terminal states (COMPLETED/FAILED)
- PayNet mock service is running for off-us transaction flows
- `ExternalApiIntegrationTest` (WireMock) is **NOT deleted** — it remains for gateway routing tests. The new E2E tests supplement it with real-backend verification.
