# BDD Test Complete Verification Gap Analysis

**Date:** 2026-04-14  
**Status:** Analysis Complete - Ready for Implementation

---

## Executive Summary

### The Problem

Tests verify **SOME** BDD "Then/And" clauses but not **ALL**.

**Example - BDD-WF-HP-W01:**
```
Then the WorkflowRouter should start WithdrawalWorkflow     # We verify this ✅
And the CheckVelocityActivity should pass                  # NOT verified ❌
And the CalculateFeesActivity should return...               # NOT verified ❌
And the BlockFloatActivity should reserve...               # NOT verified ❌
And the AuthorizeAtSwitchActivity should return APPROVED   # NOT verified ❌
And the CommitFloatActivity should commit               # NOT verified ❌
And the PublishKafkaEventActivity should publish...      # NOT verified ❌
And the TransactionRecord should have...                 # NOT verified ❌
And AgentFloat.balance should be "9499.00"               # NOT verified ❌
```

**Current state:** Only checks "202 + workflow started" - gives FALSE confidence.

---

## Detailed Gap Analysis

### BDD Scenario vs Test Coverage

| Category | BDD Scenarios | Typical "And" Clauses | Verified By Tests |
|----------|--------------|---------------------|------------------|
| **BDD-TO** (Router) | 14 | 2-3 | ✅ Only workflow start |
| **BDD-WF-HP** (Happy Path) | 10 | 5-9 | ❌ Only HTTP 202 |
| **BDD-WF-EC** (Edge Cases) | 25 | 3-6 | ❌ Only HTTP status |
| **BDD-POLL** | 5 | 1-2 | ❌ Only status check |
| **BDD-IDE** | 3 | 1-2 | ❌ Only status check |
| **BDD-HITL** | 4 | 2-3 | ❌ Only HTTP response |
| **BDD-STP** | 8 | 2-4 | ❌ Only final status |

### What's Currently Verified (vs What's Required)

#### Integration Tests (OrchestratorControllerIntegrationTest.java)
| Test | What Verifies | What's Missing |
|------|-------------|-----------------|
| BDD-TO-01 | ✅ workflowFactory.startWorkflow() called | Not verifying actual activity execution |
| BDD-TO-02 | ✅ workflowFactory.startWorkflow() called | Not verifying CheckVelocity, fees, float, switch |
| BDD-TO-03 | ✅ workflowFactory.startWorkflow() called | Not verifying validation, float credit, CBS posting |
| BDD-WF-HP-01 | ❌ Only HTTP 202 | All 9 And clauses not verified |
| BDD-WF-HP-W01 | ❌ Only returns 202 | Not verifying Velocity→Fees→Float→Switch→Commit→Kafka |
| BDD-WF-EC-W01 | ❌ Only checks failure status | Not verifying compensation/reversal |

#### E2E Tests (SelfContainedOrchestratorE2ETest.java)
| Test | What Verifies | What's Missing |
|------|-------------|-----------------|
| BDD-TO-01 | ✅ verifyWorkflowField(targetBIN) | Not verifying activity chain |
| BDD-WF-HP-W01 | ❌ Only returns 202, pending status | Not verifying CheckVelocity, BlockFloat, Authorize, etc. |
| BDD-WF-EC-W01 | ❌ Only status check | Not verifying ReleaseFloat, float restoration |

---

## Root Cause Analysis

### 1. Test Design Issue
Tests were designed for **request/response** verification, not **workflow execution path** verification.

### 2. No Activity Verification
- No verification that specific Temporal Activities were executed
- No verification of activity input/output parameters
- No verification of side effects (float, Kafka, DB)

### 3. No Compensations Verified
- Edge case tests don't verify Safety Reversal logic
- No verification of Float release on failure
- No verification of compensation triggering

---

## Proposed Fix Plan

### Phase 1: Identify All Missing Verifications Per BDD Scenario

Create a mapping of:
- Each BDD scenario
- All "And" clauses in scenario
- Required verification for each clause

### Phase 2: Implement Verification Strategies

#### Strategy A: Mock Activity Verification (Integration Tests)
For integration tests that mock Temporal:

```java
// Current (incomplete)
verify(workflowFactory).startWorkflow(...);

// Required (complete)
verify(workflowFactory).startWorkflow(...);
verify(rulesServicePort).checkVelocity(...);           // Activity #2
verify(ledgerServicePort).calculateFees(...);       // Activity #3
verify(ledgerServicePort).blockFloat(...);           // Activity #4
verify(switchAdapter).authorizeAtSwitch(...);        // Activity #5
verify(ledgerServicePort).commitFloat(...);         // Activity #6
verify(kafkaTemplate).send(...);                    // Activity #7
```

#### Strategy B: Poll + Field Verification (E2E Tests)
For E2E tests that use real infrastructure:

```java
// Current (incomplete)
assertEquals(202, statusCode);

// Required (complete) - poll and verify workflow data
var pollResponse = poll(idempotencyKey);
// Verify TransactionRecord data (activity outputs)
assertEquals("COMPLETED", pollResponse.status);
assertEquals("PAYNET-REF-789", pollResponse.externalReference);
// Verify AgentFloat (via separate API or DB)
assertEquals(expectedBalance, agentFloatService.getBalance());
```

#### Strategy C: Activity Parameter Verification
Verify inputs passed to activities:

```java
// BlockFloat input should be amount + fees
verify(ledgerServicePort).blockFloat(
    argThat(req -> 
        req.amount().equals(new BigDecimal("501.00")))); // 500 + 1
```

### Phase 3: Implementation Priority

| Priority | BDD Category | Baseline Test Coverage | Fix Complexity |
|----------|--------------|-------------------------|----------------|
| P1 | BDD-TO (Happy Path) | Already has workflow verification ✅ | MINOR - OK |
| P2 | BDD-WF-HP (Happy Path) | Only HTTP 202 | HIGH - needs activity verify |
| P3 | BDD-WF-EC (Edge Cases) | Only failure status | HIGH - needs compensation verify |
| P4 | BDD-STP (Straight Through) | Only final status | MEDIUM - needs intermediate states |
| P5 | BDD-HITL | Only response | MEDIUM - needs signal verify |

### Phase 4: Test Structure Standard

Standard template for each BDD scenario:

```java
@Test
@DisplayName("BDD-WF-HP-W01: Off-Us withdrawal completes successfully")
void withdrawOffUs_completesSuccessfully() {
    // Given - setup with mock/real data
    
    // When - call endpoint
    var response = postTransaction(request);
    
    // Then #1: HTTP response (always verify)
    assertEquals(202, response.statusCode);
    
    // Then #2: Workflow started (verify workflowFactory)
    verify(workflowFactory).startWorkflow(...);
    
    // Then #3-5: Activity execution (verify mock calls)
    verify(rulesServicePort).checkVelocity(...);
    verify(ledgerServicePort).calculateFees(...);
    verify(ledgerServicePort).blockFloat(...);
    verify(switchAdapter).authorizeAtSwitch(...);
    
    // Then #6: TransactionRecord (for E2E: poll response)
    var details = poll(workflowId);
    assertEquals("COMPLETED", details.status());
    assertEquals("PAYNET-REF-789", details.externalReference());
    
    // Then #7: Side effects
    assertEquals(expectedBalance, agentFloat.getBalance());
}
```

---

## Implementation Work Estimate

| Test File | Current Tests | Updates Required | Estimated Changes |
|----------|---------------|----------------|-----------------|
| OrchestratorControllerIntegrationTest | 39 | 15 Happy Path + 10 STP tests | Add activity verifications (50 per test) |
| BDDAlignedTransactionIntegrationTest | 6 | Add activity verifications | 5 per test |
| BDDWorkflowLifecycleIntegrationTest | 8 | Add compensation verifications | 8 per test |
| SelfContainedOrchestratorE2ETest (E2E) | 14 | All TO + HP tests | Poll + field verifications (20 per test) |

**Total test code changes:** ~800-1000 lines additional assertions

---

## Implementation Phases

### Phase 1: BDD-TO Tests (COMPLETED ✅)
- Already have workflowFactory.startWorkflow() verification
- Already have verifyWorkflowField() for E2E

### Phase 2: BDD-WF-HP Happy Path Tests (NEXT)
- BDD-WF-HP-W01, W02: Withdrawal Happy Path
- BDD-WF-HP-D01: Deposit Happy Path
- BDD-WF-HP-BP01: Bill Payment Happy Path
- BDD-WF-HP-DN01, DN02, DN03: DuitNow Happy Path

### Phase 3: BDD-WF-EC Edge Case Tests
- BDD-WF-EC-W01: Withdrawal declined - compensation
- BDD-WF-EC-W04: Insufficient float
- BDD-WF-EC-W05: Velocity check failed
- Other edge cases

### Phase 4: BDD-STP Straight Through Processing
- BDD-STP-01 through BDD-STP-04
- Verify intermediate states

### Phase 5: BDD-HITL Human in the Loop
- Verify signal handling
- Verify admin force resolve

---

## Summary

This analysis documents the gap between BDD specification (full scenario execution) and test implementation (only HTTP 202 verification). The fix plan outlines implementation in 5 phases, starting with Happy Path scenarios which are most critical.

**Key achievement after implementation:** Tests will verify ACTUAL BDD behavior including:
- Activity execution chain
- TransactionRecord data
- Side effects (AgentFloat balance, Kafka events)
- Compensation logic

This eliminates false confidence from current test suite.
