# BDD Test Mismatch Analysis - Comprehensive Report

**Date:** 2026-04-14  
**Analysis:** Tests vs. BDD Actual Behavior Requirements  
**Status:** CRITICAL - Tests verify response format, NOT BDD behavior

---

## Executive Summary

**CRITICAL FINDING:** Most BDD-aligned tests verify HTTP response structure but **DO NOT verify the actual behavior** specified in BDD "Then" clauses.

**Example:** BDD-TO-01 requires "select WithdrawalWorkflow" but test only checks HTTP 202 + JSON structure. The workflow selection logic is NEVER verified.

---

## BDD-TO Series: Workflow Router Dispatch (CRITICAL GAPS)

### BDD-TO-01: Router dispatches Off-Us withdrawal to WithdrawalWorkflow

**BDD Requires:**
```gherkin
Then it should select WithdrawalWorkflow
And start a Temporal workflow with workflowId = idempotencyKey
And return 202 Accepted with workflowId and pollUrl
```

**Test Actually Checks:**
- ✅ HTTP 202 Accepted
- ✅ workflowId = idempotencyKey  
- ✅ pollUrl format correct
- ❌ **MISSING:** WithdrawalWorkflow was selected (not WithdrawalOnUsWorkflow)
- ❌ **MISSING:** Temporal workflow was actually started

**Gap Severity:** MEDIUM (core routing behavior untested)

---

### BDD-TO-02: Router dispatches On-Us withdrawal to WithdrawalOnUsWorkflow

**BDD Requires:**
```gherkin
Then it should select WithdrawalOnUsWorkflow
And start a Temporal workflow with workflowId = idempotencyKey
```

**Test Actually Checks:**
- ✅ HTTP 202 Accepted
- ✅ workflowId = idempotencyKey
- ❌ **MISSING:** WithdrawalOnUsWorkflow was selected (not WithdrawalWorkflow)
- ❌ **MISSING:** targetBIN=0012 triggered on-us routing

**Gap Severity:** MEDIUM (on-us vs off-us routing untested)

---

### BDD-TO-03 through BDD-TO-05: Same Pattern

All have **SAME GAP**: No assertion that specific workflow was selected:
- BDD-TO-03: Should select **DepositWorkflow** ❌
- BDD-TO-04: Should select **BillPaymentWorkflow** ❌
- BDD-TO-05: Should select **DuitNowTransferWorkflow** ❌

---

### BDD-TO-06: Router rejects unsupported transaction type

**BDD Requires:**
```gherkin
Then it should return error code "ERR_UNSUPPORTED_TRANSACTION_TYPE"
And the response status should be 400 Bad Request
```

**Test Actually Checks:**
- ❌ **MISSING:** Error code = "ERR_UNSUPPORTED_TRANSACTION_TYPE"
- ⚠️ PARTIAL: Status is 4xx (could be 401, 403, 404 - not specifically 400)

**Gap Severity:** HIGH (error code not verified)

---

## Root Cause

**Problem:** Tests are written as **API contract tests** (verify response format) instead of **behavior tests** (verify actual business logic).

**Why This Matters:**
- Tests pass even if routing logic is broken
- Tests pass even if wrong workflow is selected
- Tests pass even if Temporal workflow never starts
- **Tests give FALSE confidence**

---

## Recommended Fixes

### Option 1: Add WorkflowType to Response (EASIEST)

**Change:** Include `workflowType` in transaction response
```java
record TransactionResponse(
    String status,
    String workflowId,
    String workflowType,  // ADD THIS
    String pollUrl
) {}
```

**Test Then Verifies:**
```java
.andExpect(jsonPath("$.workflowType").value("Withdrawal"))
```

**Pros:** Simple, visible to clients, easy to test
**Cons:** Changes API contract

---

### Option 2: Verify WorkflowFactory Calls (BETTER)

**Change:** Mock WorkflowFactory and verify calls
```java
@MockBean
private WorkflowFactory workflowFactory;

@Test
void BDD_TO_01_routerSelectsWithdrawalWorkflow() {
    // ... setup ...
    
    verify(workflowFactory).startWorkflow(
        eq(idempotencyKey),
        eq("Withdrawal"),  // Verify correct workflow type
        any(WithdrawalInput.class)
    );
}
```

**Pros:** Tests actual behavior, no API change
**Cons:** Requires refactoring test to use mocks properly

---

### Option 3: Integration Test with Real Temporal (BEST but HARDEST)

**Change:** Start real Temporal workflow and verify it exists
```java
@Test
void BDD_TO_01_verifyTemporalWorkflowStarted() {
    // ... start transaction ...
    
    WorkflowStub stub = workflowClient.getWorkflowStub(idempotencyKey);
    assertNotNull(stub);
    assertEquals("Withdrawal", stub.getWorkflowType());
}
```

**Pros:** Most realistic, tests full integration
**Cons:** Requires Temporal server in test environment

---

## Priority for Fixes

| Priority | BDD Scenario | Gap | Recommended Fix |
|----------|--------------|-----|-----------------|
| P0 | BDD-TO-01 | Workflow selection not verified | Option 2 (verify WorkflowFactory) |
| P0 | BDD-TO-02 | On-us vs off-us routing not verified | Option 2 |
| P0 | BDD-TO-06 | Error code not verified | Add error code assertion |
| P1 | BDD-TO-03/04/05 | Workflow selection not verified | Option 2 |
| P2 | BDD-WF-* | Workflow completion not verified | Option 3 (Temporal integration) |

---

## Next Steps

1. **Immediate:** Fix BDD-TO-06 to verify error code (5 min)
2. **Short-term:** Refactor BDD-TO-01/02 tests to verify workflow selection (30 min)
3. **Medium-term:** Add workflowType to response for visibility (1 hour)
4. **Long-term:** Implement full Temporal integration tests (1-2 days)

---

**Conclusion:** Tests currently verify response format but NOT actual BDD behavior. This gives false confidence and allows routing bugs to slip through. Fixing this is CRITICAL for test reliability.
