# BDD Test Coverage Analysis Report

**Date:** 2026-04-14  
**Analyzed By:** AI Agent  
**Scope:** All BDD scenarios vs. existing test implementation  

---

## Executive Summary

Analysis of **~150 BDD scenarios** across 3 specification files against **~99 test files** reveals critical gaps in test coverage and correctness:

- **73% of BDD scenarios have NO tests** (~110 scenarios)
- **Only 7% of tests have correct BDD alignment** (~10 scenarios)
- **20% have partial/superficial coverage** (~30 scenarios)
- Multiple tests assert wrong HTTP status codes, missing business logic validation
- Error code drift between BDD specs and implementation

**Critical untested areas:** Safety Reversal (BDD-SR), Workflow lifecycle (BDD-WF-02/03/04/05/06), Withdrawal failures (BDD-WF-EC-W*), Reversals & Disputes (BDD-V*)

---

## BDD Specification Files Analyzed

1. `docs/superpowers/specs/agent-banking-platform/2026-03-25-agent-banking-platform-bdd.md` (~100 scenarios)
2. `docs/superpowers/specs/agent-banking-platform/2026-04-05-transaction-bdd-addendum.md` (55 scenarios)
3. `docs/superpowers/specs/agent-banking-platform/2026-04-06-missing-transaction-types-bdd-addendum.md` (48 scenarios)

---

## Category 1: Tests with WRONG Assertions (Superficial Testing)

### Example: BDD-TO-01 (Workflow Router Dispatch)

**BDD Specification:**
```gherkin
Scenario: Router dispatches Off-Us withdrawal to WithdrawalWorkflow
  Given a transaction request with:
    | field           | value             |
    | transactionType | CASH_WITHDRAWAL   |
    | targetBIN       | 0123              |
    | pan             | 411111******1111  |
  When the WorkflowRouter processes the request
  Then it should select WithdrawalWorkflow
  And start a Temporal workflow with workflowId = idempotencyKey
  And return 202 Accepted with workflowId and pollUrl
```

**What Tests Actually Assert:**
- ✗ Check HTTP 200 (should be **202 Accepted**)
- ✗ Only verify `status=PENDING` in response body
- ✗ Check `workflowId` exists
- ✗ **DO NOT** verify that `WithdrawalWorkflow` was actually selected
- ✗ **DO NOT** verify Temporal workflow was started
- ✗ **DO NOT** verify `pollUrl` format (`/api/v1/transactions/{idempotencyKey}/status`)
- ✗ **DO NOT** verify targetBIN routing logic (off-Us vs on-Us)

**Affected Test Files:**
- `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/integration/OrchestratorControllerIntegrationTest.java`
- `gateway/src/test/java/com/agentbanking/gateway/integration/orchestrator/SelfContainedOrchestratorE2ETest.java`

**Same Pattern Across ALL Orchestrator Tests:**
Tests only validate API returns 200 with PENDING status, but don't verify:
- The correct workflow was selected
- Temporal integration actually worked
- The specific BDD business rules were enforced

---

## Category 2: Missing BDD Scenario Coverage

### BDD-TO Series (Workflow Router) - 6 scenarios

| Scenario | Status | Issue |
|----------|--------|-------|
| BDD-TO-01 | ⚠️ Partial | Wrong HTTP status, missing workflow verification |
| BDD-TO-02 | ⚠️ Partial | Wrong HTTP status, missing workflow verification |
| BDD-TO-03 | ⚠️ Partial | Wrong HTTP status, missing workflow verification |
| BDD-TO-04 | ⚠️ Partial | Wrong HTTP status, missing workflow verification |
| BDD-TO-05 | ⚠️ Partial | Wrong HTTP status, missing workflow verification |
| BDD-TO-06 | ⚠️ Partial | Checks 4xx but not specific error code |
| BDD-TO-07 to BDD-TO-15 | ❌ Missing | New transaction types from 2026-04-06 addendum |

**Note:** Tests exist labeled `BDD-TO-NEW-01` through `BDD-TO-NEW-09` but don't align with BDD scenario numbering and only check `status=PENDING`.

---

### BDD-WF Series (Workflow Lifecycle) - 6 scenarios

| Scenario | Status | Issue |
|----------|--------|-------|
| BDD-WF-01 | ⚠️ Partial | Checks PENDING response only |
| BDD-WF-02 | ❌ **MISSING** | Workflow completes successfully, updates TransactionRecord |
| BDD-WF-03 | ❌ **MISSING** | Workflow fails, records error in TransactionRecord |
| BDD-WF-04 | ❌ **MISSING** | Workflow survives JVM crash and resumes |
| BDD-WF-05 | ❌ **MISSING** | Workflow survives network partition (25s timeout) |
| BDD-WF-06 | ❌ **MISSING** | Workflow stays in COMPENSATING state when compensation fails |

---

### BDD-WF-HP-W Series (Withdrawal Happy Path) - 2 scenarios

**BDD-WF-HP-W01:** Test exists but doesn't verify:
- ✗ CheckVelocityActivity passed
- ✗ CalculateFeesActivity returned correct fees (customerFee=1.00, agentCommission=0.20, bankShare=0.80)
- ✗ BlockFloatActivity reserved RM 501.00
- ✗ AuthorizeAtSwitchActivity returned APPROVED with referenceCode "PAYNET-REF-789"
- ✗ CommitFloatActivity committed RM 501.00
- ✗ AgentFloat.balance = 9499.00
- ✗ TransactionRecord has all required fields

**BDD-WF-HP-W02:** Similar issues

---

### BDD-WF-EC-W Series (Withdrawal Failures) - 8 scenarios

| Scenario | Status | Issue |
|----------|--------|-------|
| BDD-WF-EC-W01 | ❌ **MISSING** | Withdrawal declined by switch — compensation releases float |
| BDD-WF-EC-W02 | ❌ **MISSING** | Switch timeout — Safety Reversal triggered with Store & Forward |
| BDD-WF-EC-W03 | ❌ **MISSING** | CommitFloat fails after switch approval — Safety Reversal |
| BDD-WF-EC-W04 | ⚠️ Superficial | Only checks `isOk()`, no error code or balance verification |
| BDD-WF-EC-W05 | ⚠️ Superficial | Only checks `isOk()`, no error code verification |
| BDD-WF-EC-W06 | ❌ **MISSING** | Fee config not found — workflow fails before float block |
| BDD-WF-EC-W07 | ❌ **MISSING** | On-Us withdrawal — CBS authorization fails, float released |
| BDD-WF-EC-W08 | ❌ **MISSING** | PublishKafkaEvent fails — workflow still completes |

---

### BDD-SR Series (Safety Reversal) - 4 scenarios

| Scenario | Status | Issue |
|----------|--------|-------|
| BDD-SR-01 | ❌ **MISSING** | Safety Reversal succeeds on first attempt |
| BDD-SR-02 | ❌ **MISSING** | Safety Reversal retries until PayNet acknowledges (5 fails + 1 success) |
| BDD-SR-03 | ❌ **MISSING** | Safety Reversal persists across JVM restarts |
| BDD-SR-04 | ❌ **MISSING** | Safety Reversal flagged for manual investigation after 24h failure |

**CRITICAL:** These are Store & Forward scenarios with retry logic essential for financial safety.

---

### BDD-HITL Series (Human-in-the-Loop) - 3 scenarios

| Scenario | Status | Issue |
|----------|--------|-------|
| BDD-HITL-01 | ⚠️ Partial | Doesn't verify COMPENSATING state, 4-hour elapsed, AuditLog |
| BDD-HITL-02 | ⚠️ Partial | Doesn't verify COMPENSATING state, 4-hour elapsed, AuditLog |
| BDD-HITL-03 | ❌ **MISSING** | Unauthorized user cannot send force-resolve signal |

---

### BDD-POLL Series (Polling) - 5 scenarios

| Scenario | Status | Issue |
|----------|--------|-------|
| BDD-POLL-01 | ⚠️ Partial | Polls but doesn't verify full response structure |
| BDD-POLL-02 | ⚠️ Partial | Polls but doesn't verify error details |
| BDD-POLL-03 | ⚠️ Partial | Partial test |
| BDD-POLL-04 | ✅ Tested | Returns 404 for non-existent workflow |
| BDD-POLL-05 | ❌ **MISSING** | Redis caching to reduce Temporal load |

---

### BDD-IDE Series (Idempotency) - 3 scenarios

| Scenario | Status | Issue |
|----------|--------|-------|
| BDD-IDE-01 | ⚠️ Partial | Tests duplicate POST but doesn't verify Temporal rejection |
| BDD-IDE-02 | ⚠️ Partial | Tests but doesn't verify Redis cache with 24h TTL |
| BDD-IDE-03 | ⚠️ Partial | Tests but doesn't verify cached error response |

---

### BDD-XWF Series (Cross-Workflow) - 3 scenarios

| Scenario | Status | Issue |
|----------|--------|-------|
| BDD-XWF-01 | ⚠️ Partial | Sends 2 requests but doesn't verify PESSIMISTIC_WRITE lock |
| BDD-XWF-02 | ❌ **MISSING** | Compensation executes in reverse order |
| BDD-XWF-03 | ❌ **MISSING** | Compensation after switch approval includes Safety Reversal |

---

## Category 3: Entire BDD Files Untested

### From `2026-03-25-agent-banking-platform-bdd.md` (~100 scenarios)

#### BDD-R Series (Rules & Fee Engine) - 14 scenarios

| Scenario | Status | Issue |
|----------|--------|-------|
| BDD-R01 | ❌ **MISSING** | Configure fee structure for Micro agent cash withdrawal |
| BDD-R01-PCT | ❌ **MISSING** | Percentage-based fee for Premier agent |
| BDD-R01-EC-01 | ❌ **MISSING** | No fee configuration exists |
| BDD-R01-EC-02 | ❌ **MISSING** | Fee configuration is expired |
| BDD-R01-EC-03 | ❌ **MISSING** | Fee component values do not sum correctly |
| BDD-R02 | ❌ **MISSING** | Daily transaction limit check passes |
| BDD-R02-EC-01 | ❌ **MISSING** | Daily transaction limit exceeded — amount boundary |
| BDD-R02-EC-02 | ❌ **MISSING** | Daily transaction count limit exceeded |
| BDD-R02-EC-03 | ❌ **MISSING** | Transaction amount is zero |
| BDD-R02-EC-04 | ❌ **MISSING** | Transaction amount is negative |
| BDD-R03 | ❌ **MISSING** | Velocity check passes |
| BDD-R03-EC-01 | ⚠️ Wrong Error Code | Uses `ERR_BIZ_VELOCITY_COUNT_EXCEEDED`, BDD expects `ERR_VELOCITY_COUNT_EXCEEDED` |
| BDD-R03-EC-02 | ⚠️ Wrong Error Code | Uses `ERR_BIZ_VELOCITY_AMOUNT_EXCEEDED`, BDD expects `ERR_VELOCITY_AMOUNT_EXCEEDED` |
| BDD-R03-EC-03 | ❌ **MISSING** | Velocity rule is inactive |
| BDD-R03-EC-04 | ❌ **MISSING** | No velocity rule configured |
| BDD-R04 | ❌ **MISSING** | Percentage-based fee calculation with rounding |

**Unit tests exist in:**
- `services/rules-service/src/test/java/com/agentbanking/rules/domain/service/FeeCalculationServiceTest.java`
- `services/rules-service/src/test/java/com/agentbanking/rules/domain/service/VelocityCheckServiceTest.java`

**But NO integration tests matching BDD scenarios!**

---

#### BDD-L Series (Ledger & Float) - 13 scenarios

| Scenario | Status | Issue |
|----------|--------|-------|
| BDD-L01 | ❌ **MISSING** | Agent checks wallet balance (only unit test exists) |
| BDD-L01-EC-01 | ❌ **MISSING** | Agent float not found |
| BDD-L01-EC-02 | ❌ **MISSING** | Agent is deactivated |
| BDD-L02 | ❌ **MISSING** | Transaction creates double-entry journal (DEBIT + CREDIT) |
| BDD-L03 | ❌ **MISSING** | Real-time settlement updates agent float |
| BDD-L03-EC-01 | ❌ **MISSING** | Insufficient agent float |
| BDD-L03-EC-02 | ❌ **MISSING** | Concurrent withdrawal race condition (PESSIMISTIC_WRITE lock) |
| BDD-L04 | ❌ **MISSING** | Customer balance inquiry via card + PIN |
| BDD-L04-EC-01 | ❌ **MISSING** | Balance inquiry with invalid PIN |
| BDD-L04-EC-02 | ❌ **MISSING** | Duplicate balance inquiry with same idempotency key |
| BDD-L02-DC | ❌ **MISSING** | Cash deposit creates correct double-entry journal |
| BDD-L02-MER | ❌ **MISSING** | Retail sale creates correct double-entry journal with MDR |
| BDD-L02-PIN | ❌ **MISSING** | PIN purchase creates correct double-entry journal |

---

#### BDD-W Series (Cash Withdrawal) - 9 scenarios

| Scenario | Status | Issue |
|----------|--------|-------|
| BDD-W01 | ❌ **MISSING** | Successful ATM card withdrawal (EMV + PIN) with ISO 8583 |
| BDD-W01-EC-01 | ❌ **MISSING** | Withdrawal with invalid card PIN |
| BDD-W01-EC-02 | ❌ **MISSING** | Terminal printer failure after switch approval — reversal |
| BDD-W01-EC-03 | ❌ **MISSING** | Network drop after switch approval — Store & Forward |
| BDD-W01-EC-04 | ❌ **MISSING** | Withdrawal amount exceeds daily limit |
| BDD-W01-EC-05 | ❌ **MISSING** | Withdrawal outside geofence |
| BDD-W01-EC-06 | ❌ **MISSING** | GPS unavailable on POS terminal |
| BDD-W01-EC-07 | ❌ **MISSING** | Duplicate withdrawal with same idempotency key |
| BDD-W01-EC-08 | ❌ **MISSING** | Reversal fails after multiple retries |
| BDD-W01-SMS | ❌ **MISSING** | Withdrawal triggers SMS notification |
| BDD-W02 | ❌ **MISSING** | Successful MyKad-based withdrawal |
| BDD-W02-EC-01 | ❌ **MISSING** | MyKad withdrawal with biometric mismatch |

---

#### BDD-D Series (Cash Deposit) - 8 scenarios

| Scenario | Status | Issue |
|----------|--------|-------|
| BDD-D01 | ⚠️ Partial | Test exists but doesn't verify ProxyEnquiry |
| BDD-D01-EC-01 | ❌ **MISSING** | Deposit to invalid account |
| BDD-D01-EC-02 | ❌ **MISSING** | Deposit amount is zero |
| BDD-D01-EC-03 | ❌ **MISSING** | Deposit amount is negative |
| BDD-D01-EC-04 | ❌ **MISSING** | Agent float cap exceeded after deposit |
| BDD-D01-NIC | ❌ **MISSING** | Deposit with name inquiry confirmation (low value) |
| BDD-D01-BIO | ❌ **MISSING** | High-value deposit requires MyKad biometric match |
| BDD-D02 | ❌ **MISSING** | Successful card-based deposit |
| BDD-D02-EC-01 | ❌ **MISSING** | Card deposit with invalid PIN |

---

#### BDD-B Series (Bill Payments) - 8 scenarios

| Scenario | Status | Issue |
|----------|--------|-------|
| BDD-B01 | ❌ **MISSING** | Successful JomPAY bill payment via cash |
| BDD-B01-EC-01 | ❌ **MISSING** | Bill payment — Ref-1 validation fails |
| BDD-B01-EC-02 | ❌ **MISSING** | Bill payment — biller system timeout |
| BDD-B01-EC-03 | ❌ **MISSING** | Bill payment — insufficient agent float |
| BDD-B02 | ❌ **MISSING** | Successful ASTRO RPN bill payment via cash |
| BDD-B02-CARD | ❌ **MISSING** | Successful ASTRO RPN bill payment via card |
| BDD-B03 | ❌ **MISSING** | Successful TM RPN payment |
| BDD-B04 | ❌ **MISSING** | Successful EPF i-SARAAN payment |
| BDD-B04-EC-01 | ❌ **MISSING** | EPF payment — invalid member number |

---

#### BDD-T Series (Prepaid Top-up) - 6 scenarios

| Scenario | Status | Issue |
|----------|--------|-------|
| BDD-T01 | ❌ **MISSING** | Successful CELCOM prepaid top-up via cash |
| BDD-T01-EC-01 | ❌ **MISSING** | CELCOM top-up — invalid phone number |
| BDD-T01-EC-02 | ❌ **MISSING** | CELCOM top-up — aggregator timeout |
| BDD-T01-CARD | ❌ **MISSING** | Successful CELCOM prepaid top-up via card |
| BDD-T02 | ❌ **MISSING** | Successful M1 prepaid top-up via cash |
| BDD-T02-EC-01 | ❌ **MISSING** | M1 top-up — invalid phone number |

---

#### BDD-DNOW Series (DuitNow & JomPAY) - 7 scenarios

| Scenario | Status | Issue |
|----------|--------|-------|
| BDD-DNOW-01 | ❌ **MISSING** | Successful DuitNow transfer via mobile number (ISO 20022) |
| BDD-DNOW-01-EC-01 | ❌ **MISSING** | DuitNow transfer — recipient account closed (AC04) |
| BDD-DNOW-01-EC-02 | ❌ **MISSING** | DuitNow transfer — network timeout at PayNet |
| BDD-DNOW-01-EC-03 | ❌ **MISSING** | DuitNow transfer — invalid proxy |
| BDD-DNOW-01-NRIC | ❌ **MISSING** | DuitNow transfer via MyKad number |
| BDD-DNOW-01-BRN | ❌ **MISSING** | DuitNow transfer via Business Registration Number |
| BDD-DNOW-02 | ❌ **MISSING** | Successful JomPAY ON-US payment (same bank) |
| BDD-DNOW-03 | ❌ **MISSING** | Successful JomPAY OFF-US payment (cross-bank) |

---

#### BDD-WAL Series (e-Wallet) - 5 scenarios

| Scenario | Status | Issue |
|----------|--------|-------|
| BDD-WAL-01 | ❌ **MISSING** | Successful Sarawak Pay e-Wallet withdrawal via cash |
| BDD-WAL-01-EC-01 | ❌ **MISSING** | Sarawak Pay withdrawal — insufficient wallet balance |
| BDD-WAL-01-CARD | ❌ **MISSING** | Successful Sarawak Pay e-Wallet withdrawal via card |
| BDD-WAL-02 | ❌ **MISSING** | Successful Sarawak Pay e-Wallet top-up via cash |
| BDD-WAL-02-CARD | ❌ **MISSING** | Successful Sarawak Pay e-Wallet top-up via card |

---

#### BDD-ESSP Series (eSSP) - 3 scenarios

| Scenario | Status | Issue |
|----------|--------|-------|
| BDD-ESSP-01 | ❌ **MISSING** | Successful eSSP certificate purchase via cash |
| BDD-ESSP-01-EC-01 | ❌ **MISSING** | eSSP purchase — BSN system unavailable |
| BDD-ESSP-01-CARD | ❌ **MISSING** | Successful eSSP certificate purchase via card |

---

#### BDD-V Series (Reversals & Disputes) - 5 scenarios

| Scenario | Status | Issue |
|----------|--------|-------|
| BDD-V01 | ❌ **MISSING** | Network timeout triggers automatic reversal (Store & Forward) |
| BDD-V01-EC-01 | ❌ **MISSING** | Reversal message fails — Store & Forward retries |
| BDD-V01-EC-02 | ❌ **MISSING** | Reversal fails after maximum retries |
| BDD-V01-EC-03 | ❌ **MISSING** | Financial authorization uses ZERO retries on timeout |
| BDD-V01-ECHO | ❌ **MISSING** | Non-financial echo uses exponential backoff retry |
| BDD-V02 | ❌ **MISSING** | (truncated in BDD spec) |

---

## Category 4: Tests with WRONG Error Codes

| BDD Expected Error Code | Test Uses | File Location |
|-------------------------|-----------|---------------|
| `ERR_VELOCITY_COUNT_EXCEEDED` | `ERR_BIZ_VELOCITY_COUNT_EXCEEDED` | `VelocityCheckServiceTest.java` |
| `ERR_VELOCITY_AMOUNT_EXCEEDED` | `ERR_BIZ_VELOCITY_AMOUNT_EXCEEDED` | `VelocityCheckServiceTest.java` |
| `ERR_INSUFFICIENT_FUNDS` | Not tested | Multiple tests missing |
| `ERR_NETWORK_TIMEOUT` | Not tested | Multiple tests missing |
| `ERR_FEE_CONFIG_NOT_FOUND` | Not tested | Multiple tests missing |

---

## Category 5: Missing Critical Assertions in Existing Tests

### Gateway E2E Tests (`SelfContainedOrchestratorE2ETest.java`)

Tests labeled with BDD IDs but only check:
- HTTP status code
- Response is not null
- `status=PENDING`

**Missing assertions for:**
- ✗ Temporal workflow actually started
- ✗ Correct workflow type selected
- ✗ Activities executed in correct order
- ✗ Float balance changes
- ✗ TransactionRecord created with correct fields
- ✗ Kafka events published
- ✗ Journal entries created

### Orchestrator Integration Tests (`OrchestratorControllerIntegrationTest.java`)

Tests labeled with BDD IDs but only check:
- `status().isOk()` (200 instead of 202!)
- `status=PENDING`
- `workflowId` exists

**Missing assertions for:**
- ✗ Correct workflow selected by router
- ✗ Temporal workflow lifecycle
- ✗ Activity execution order
- ✗ Business logic validation
- ✗ Error code verification
- ✗ Float balance verification

---

## Summary Statistics

| Metric | Count | Percentage |
|--------|-------|------------|
| **Total BDD scenarios identified** | ~150+ | 100% |
| **Tests with correct BDD alignment** | ~10 | **7%** |
| **Tests with partial/superficial coverage** | ~30 | **20%** |
| **BDD scenarios with NO tests** | ~110 | **73%** |
| **Tests with WRONG error codes** | 2+ | - |
| **Critical BDD scenarios untested** | 30+ | - |

### By Category

| BDD Category | Total Scenarios | Tested | Partial | Missing | Coverage % |
|--------------|----------------|--------|---------|---------|------------|
| BDD-TO (Router) | 15 | 6 | 9 | 0 | 40% (superficial) |
| BDD-WF (Lifecycle) | 6 | 1 | 0 | 5 | 17% |
| BDD-WF-HP-W (Withdrawal HP) | 2 | 0 | 2 | 0 | 0% (superficial) |
| BDD-WF-EC-W (Withdrawal EC) | 8 | 0 | 2 | 6 | 0% |
| BDD-SR (Safety Reversal) | 4 | 0 | 0 | 4 | **0%** |
| BDD-HITL (Human-in-Loop) | 3 | 0 | 2 | 1 | 0% |
| BDD-POLL (Polling) | 5 | 1 | 3 | 1 | 20% |
| BDD-IDE (Idempotency) | 3 | 0 | 3 | 0 | 0% (superficial) |
| BDD-XWF (Cross-Workflow) | 6 | 0 | 1 | 5 | 0% |
| BDD-R (Rules) | 14 | 0 | 4 | 10 | 0% |
| BDD-L (Ledger) | 13 | 0 | 0 | 13 | **0%** |
| BDD-W (Withdrawal) | 12 | 0 | 0 | 12 | **0%** |
| BDD-D (Deposit) | 9 | 0 | 1 | 8 | 0% |
| BDD-B (Bill Payment) | 9 | 0 | 0 | 9 | **0%** |
| BDD-T (Top-up) | 6 | 0 | 0 | 6 | **0%** |
| BDD-DNOW (DuitNow) | 8 | 0 | 0 | 8 | **0%** |
| BDD-WAL (e-Wallet) | 5 | 0 | 0 | 5 | **0%** |
| BDD-ESSP (eSSP) | 3 | 0 | 0 | 3 | **0%** |
| BDD-V (Reversals) | 6 | 0 | 0 | 6 | **0%** |

---

## Root Causes

1. **Tests validate API response format, NOT business logic**
   - Tests check HTTP status and response JSON structure
   - Don't verify actual workflow execution, float changes, or database state

2. **No Temporal workflow testing**
   - Crash recovery scenarios untested
   - Compensation logic untested
   - Safety reversal untested
   - Activity execution order untested

3. **No integration tests with real float balance verification**
   - Tests don't verify AgentFloat.balance changes
   - Tests don't verify journal entry creation
   - Tests don't verify TransactionRecord completeness

4. **Error code drift between BDD specs and implementation**
   - `ERR_BIZ_VELOCITY_*` vs `ERR_VELOCITY_*`
   - Missing error codes in tests for common failure scenarios

5. **Test organization by technical layer instead of BDD scenarios**
   - Tests organized as unit/integration/architecture
   - No mapping back to BDD scenario IDs in most tests
   - Impossible to trace which BDD scenarios are covered

6. **Missing assertion on downstream effects**
   - Database state changes unverified
   - Kafka event publishing unverified
   - Temporal workflow state unverified

---

## Recommendations

### Phase 1: Fix Existing Tests (Immediate)
1. Update orchestrator integration tests to verify:
   - Correct HTTP status codes (202 vs 200)
   - Actual workflow selection (not just response format)
   - pollUrl format verification
   - Error codes match BDD spec
2. Fix error code mismatches in `VelocityCheckServiceTest`
3. Add comprehensive assertions to gateway E2E tests

### Phase 2: Implement Missing Critical Tests (High Priority)
Priority order based on financial safety importance:
1. **BDD-SR series** (Safety Reversal) - 4 scenarios - CRITICAL for financial safety
2. **BDD-WF-02/03** (Workflow completion/failure) - 2 scenarios
3. **BDD-WF-EC-W series** (Withdrawal failures) - 8 scenarios
4. **BDD-HITL-03** (Unauthorized HITL) - 1 scenario
5. **BDD-POLL-05** (Redis caching) - 1 scenario
6. **BDD-XWF-02/03** (Compensation order) - 2 scenarios
7. **BDD-V series** (Reversals) - 5+ scenarios

### Phase 3: Add Missing Domain Tests (Medium Priority)
From original BDD file (~100 scenarios):
- BDD-R series (Rules & Fees) - integration tests
- BDD-L series (Ledger & Float) - with balance verification
- BDD-W/D/B/T/DNOW/WAL/ESSP series - one test per scenario

### Phase 4: Improve Test Architecture (Ongoing)
1. Create test classes organized by BDD scenario ID
2. Use Temporal testing library for workflow lifecycle tests
3. Use Testcontainers for real database/Kafka/Redis testing
4. Each test must verify ALL BDD "Then" clauses
5. Add BDD scenario ID to @DisplayName for traceability
6. Implement BDD scenario coverage reporting in CI/CD

---

## Implementation Approach

### Test Structure Template
```java
@DisplayName("BDD-TO-01: Router dispatches Off-Us withdrawal to WithdrawalWorkflow [HP]")
@Test
void BDD_TO_01_routerDispatchesOffUsWithdrawal() {
    // Given - setup per BDD spec
    // When - execute per BDD spec
    // Then - verify ALL BDD assertions:
    //   1. HTTP 202 Accepted
    //   2. workflowId = idempotencyKey
    //   3. pollUrl format correct
    //   4. WithdrawalWorkflow selected
    //   5. Temporal workflow started
}
```

### Tools Required
- Temporal testing library (`io.temporal:temporal-testing`)
- Testcontainers (already configured)
- WireMock for external service mocking
- ArchUnit for architecture verification (already in use)

---

## Files Analyzed

### BDD Specification Files
1. `docs/superpowers/specs/agent-banking-platform/2026-03-25-agent-banking-platform-bdd.md`
2. `docs/superpowers/specs/agent-banking-platform/2026-04-05-transaction-bdd-addendum.md`
3. `docs/superpowers/specs/agent-banking-platform/2026-04-06-missing-transaction-types-bdd-addendum.md`

### Test Files (Sample)
- `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/integration/OrchestratorControllerIntegrationTest.java`
- `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/domain/service/WorkflowRouterTest.java`
- `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/application/usecase/StartTransactionUseCaseImplTest.java`
- `gateway/src/test/java/com/agentbanking/gateway/integration/orchestrator/SelfContainedOrchestratorE2ETest.java`
- `services/rules-service/src/test/java/com/agentbanking/rules/domain/service/FeeCalculationServiceTest.java`
- `services/rules-service/src/test/java/com/agentbanking/rules/domain/service/VelocityCheckServiceTest.java`
- `services/ledger-service/src/test/java/com/agentbanking/ledger/domain/service/LedgerServiceTest.java`
- And 90+ additional test files across all services

---

## Conclusion

The current test suite provides **false confidence** - tests pass but don't verify the actual business logic specified in BDD scenarios. Critical financial safety features (Safety Reversal, Store & Forward, Compensation) are completely untested.

**Immediate action required** to:
1. Fix existing superficial tests
2. Implement missing critical safety tests
3. Establish BDD-to-test traceability
4. Add comprehensive assertions verifying business logic

This analysis should be used as a roadmap for implementing proper BDD-aligned test coverage.
