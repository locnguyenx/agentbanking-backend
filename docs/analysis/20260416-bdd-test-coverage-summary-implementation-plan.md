# BDD Test Coverage Analysis - 2026-04-16 Implementation Plan

**Date:** 2026-04-16
**Analysis Scope:** Current test implementation vs BDD specifications
**Status:** Analysis Complete - Implementation Plan Ready

---

## Executive Summary

### Critical Discovery

**Happy Path tests provide FALSE CONFIDENCE.** Tests exist and pass, but verify **0% of actual business logic**.

### Current State (2026-04-16)

✅ **Completed Areas:**
- BDD-SR (Safety Reversal): 6/6 tests complete with activity verification
- BDD-V (Reversals): 11/11 tests complete with Store & Forward verification
- BDD-WF-EC (Edge Cases): 11/11 tests complete with compensation logic
- HTTP Status Codes: Fixed to 202 Accepted per BDD spec
- Error Codes: 15 new error codes added

❌ **Critical Gap - Happy Path Tests:**
- BDD-WF-HP tests exist but are **SUPERFICIAL**
- Verify only HTTP 202 + workflow start
- **0% verification** of activity execution, TransactionRecord state, or side effects

### Impact

Tests pass but provide **ZERO assurance** that workflows execute correctly. This is the most dangerous form of false confidence - tests that give security without providing safety.

---

## Detailed Findings

### Test Coverage by Category

| Category | Test Status | Coverage | What's Verified | Critical Gaps |
|----------|-------------|----------|-----------------|---------------|
| **BDD-TO (Router)** | ✅ Tests Exist | 100% | HTTP 202, workflow selection | Activity execution chain missing |
| **BDD-WF-HP (Happy Path)** | ❌ **FALSE POSITIVES** | 0% | HTTP 202, workflow start | **ALL business logic** |
| **BDD-WF-EC (Edge Cases)** | ✅ Complete | 100% | Compensation, activity ordering | - |
| **BDD-SR (Safety Reversal)** | ✅ Complete | 100% | Retry logic, audit logging | - |
| **BDD-V (Reversals)** | ✅ Complete | 100% | ISO 8583, Store & Forward | - |
| **E2E Tests** | ❌ Minimal | 10% | HTTP 202, status=PENDING | Business logic verification |

### Happy Path Test Analysis

**Location:** `OrchestratorControllerIntegrationTest.java`

**Current Test (BDD-WF-HP-W01):**
```java
@Test
void withdrawOffUs_validRequest_shouldStartWorkflow() throws Exception {
    mockMvc.perform(post("/api/v1/transactions").content(requestBody))
        .andExpect(status().isAccepted())           // ✅ HTTP 202
        .andExpect(jsonPath("$.status").value("PENDING")); // ✅ Status

    verify(workflowFactory).startWorkflow(...);     // ✅ Workflow start
    // ❌ MISSING: Everything else
}
```

**BDD Specification Requires (9 "And" clauses):**
1. ✅ WorkflowRouter should start WithdrawalWorkflow
2. ❌ CheckVelocityActivity should pass
3. ❌ CalculateFeesActivity should return...
4. ❌ BlockFloatActivity should reserve...
5. ❌ AuthorizeAtSwitchActivity should return APPROVED
6. ❌ CommitFloatActivity should commit
7. ❌ PublishKafkaEventActivity should publish...
8. ❌ TransactionRecord should have...
9. ❌ AgentFloat.balance should be "9499.00"

**Result:** Test verifies 11% of required functionality but claims to test the entire workflow.

---

## Implementation Plan

### Priority 1: BDD-WF-HP Integration Tests (HIGHEST)

**Files:** `OrchestratorControllerIntegrationTest.java`, `BDDAlignedTransactionIntegrationTest.java`

**Goal:** Transform superficial tests into comprehensive business logic verification

**Changes Required:**
- Add mock setup for successful workflow execution
- Add Mockito verification for all 6 activities
- Add TransactionRecord state verification
- Add AgentFloat balance verification
- Add JournalEntry double-entry verification

**Example Implementation:**
```java
@Test
void BDD_WF_HP_W01_withdrawalCompletesSuccessfully() throws Exception {
    // Setup mocks for complete workflow execution
    when(rulesServicePort.checkVelocity(any())).thenReturn(success());
    when(ledgerServicePort.calculateFees(any())).thenReturn(fees(1.00, 0.20, 0.80));
    when(ledgerServicePort.blockFloat(any())).thenReturn(success());
    when(switchAdapter.authorizeAtSwitch(any())).thenReturn(approved());
    when(ledgerServicePort.commitFloat(any())).thenReturn(success());
    when(kafkaTemplate.send(any(), any())).thenReturn(null);

    // Execute and verify ALL aspects
    var response = postTransaction(withdrawalRequest);
    assertEquals(202, response.getStatusCode());

    verify(workflowFactory).startWorkflow(...);

    // Activity execution chain
    verify(rulesServicePort).checkVelocity(...);
    verify(ledgerServicePort).calculateFees(...);
    verify(ledgerServicePort).blockFloat(argThat(req -> req.amount().equals(new BigDecimal("501.00"))));
    verify(switchAdapter).authorizeAtSwitch(...);
    verify(ledgerServicePort).commitFloat(...);
    verify(kafkaTemplate).send(...);

    // Database state verification
    TransactionRecord record = transactionRecordRepository.findByWorkflowId(workflowId);
    assertEquals(TransactionStatus.COMPLETED, record.getStatus());
    assertEquals(new BigDecimal("9499.00"), agentFloatRepository.findByAgentId(agentId).getBalance());

    // Double-entry verification
    List<JournalEntry> entries = journalEntryRepository.findByTransactionId(record.getId());
    assertEquals(2, entries.size());
    // Verify DEBIT AGT_FLOAT and CREDIT SETTLEMENT entries
}
```

**Tests to Update:** 8 Happy Path tests
**Estimated LOC:** 400 lines

### Priority 2: BDD-WF-HP E2E Tests (HIGH)

**Files:** `SelfContainedOrchestratorE2ETest.java`

**Goal:** Add real infrastructure verification to E2E tests

**Changes Required:**
- Poll until workflow completion
- Verify TransactionRecord via poll response
- Verify AgentFloat via Ledger API calls
- Verify JournalEntry via Ledger API calls

**Example Implementation:**
```java
@Test
void offUsWithdrawal_completesSuccessfully() {
    BigDecimal initialBalance = getAgentFloatBalance(agentId);

    String responseBody = submitTransaction(withdrawalRequest);
    String workflowId = extractWorkflowId(responseBody);
    assertEquals("PENDING", extractStatus(responseBody));

    // Poll to completion
    TransactionPollResponse pollResponse = pollUntilComplete(workflowId, 60);

    // Verify final state
    assertEquals("COMPLETED", pollResponse.status());
    assertEquals(new BigDecimal("500.00"), pollResponse.amount());

    // Verify side effects
    BigDecimal finalBalance = getAgentFloatBalance(agentId);
    assertEquals(initialBalance.subtract(new BigDecimal("501.00")), finalBalance);

    List<JournalEntry> entries = getJournalEntries(pollResponse.transactionId());
    assertEquals(2, entries.size());
    // Verify double-entry bookkeeping
}
```

**Tests to Update:** 8 Happy Path tests
**Infrastructure:** 100 LOC helper methods
**Estimated LOC:** 580 lines

### Priority 3: Infrastructure (MEDIUM)

**Files:** Test base classes, E2E test class

**Changes:** Add repository injections and API helper methods

**Estimated LOC:** 150 lines

---

## Success Criteria

### Definition of Done

1. **Every BDD "Then" clause has corresponding assertions**
2. **Activity execution chain fully verified** (CheckVelocity → CalculateFees → BlockFloat → Authorize → Commit → Kafka)
3. **TransactionRecord final state verified** (status, amounts, fees, commissions, card masked)
4. **AgentFloat balance changes verified**
5. **JournalEntry double-entry bookkeeping verified**
6. **E2E tests verify through real infrastructure**
7. **Test execution passes with comprehensive coverage**

### Expected Test Output

```
$ ./gradlew :services:orchestrator-service:test --tests "BDD-WF-HP*"

BDD-WF-HP-W01: Off-Us withdrawal completes successfully PASSED
  ✓ HTTP 202 Accepted returned
  ✓ Workflow started with WithdrawalWorkflow
  ✓ CheckVelocityActivity called with correct MyKad
  ✓ CalculateFeesActivity returned correct fees (1.00, 0.20, 0.80)
  ✓ BlockFloatActivity blocked 501.00 (500 + 1 fee)
  ✓ AuthorizeAtSwitchActivity sent ISO 8583 authorization
  ✓ CommitFloatActivity committed the float
  ✓ PublishKafkaEventActivity published completion event
  ✓ TransactionRecord: status=COMPLETED, amount=500.00, fees verified
  ✓ AgentFloat balance: 10000.00 → 9499.00 (debit 501.00)
  ✓ JournalEntry: DEBIT AGT_FLOAT 501, CREDIT SETTLEMENT 501

All Happy Path tests now verify ACTUAL business logic execution!
```

---

## Implementation Timeline

### Week 1: Phase 1 (Integration Tests)
- **Day 1:** BDD-WF-HP-W01, W02 (withdrawals) - 120 LOC
- **Day 2:** BDD-WF-HP-D01, BP01, DN01 (other types) - 150 LOC
- **Day 3:** BDD-TO tests enhancement - 100 LOC
- **Day 4:** Testing and fixes - ensure all pass

### Week 2: Phase 2 (E2E Tests) + Phase 3 (Infrastructure)
- **Day 5:** E2E enhancements - 200 LOC
- **Day 6:** Infrastructure and helpers - 150 LOC
- **Day 7:** Final testing and documentation

**Total:** 950 LOC, 2 weeks, comprehensive Happy Path verification

---

## Risk Mitigation

### Risk 1: Test Brittleness
**Mitigation:** Use `argThat()` for flexible parameter matching, not exact equality

### Risk 2: E2E Performance
**Mitigation:** Keep polling intervals reasonable (5-10s), run E2E tests selectively

### Risk 3: Repository Access
**Mitigation:** Confirm `AbstractOrchestratorRealInfraIntegrationTest` provides real DB access

---

## Files to Modify Summary

| File | Changes | Priority | LOC |
|------|---------|----------|-----|
| `OrchestratorControllerIntegrationTest.java` | Enhance 8 BDD-WF-HP tests | P1 | 320 |
| `BDDAlignedTransactionIntegrationTest.java` | Add repository verifications | P1 | 80 |
| `SelfContainedOrchestratorE2ETest.java` | Add polling + API verification | P2 | 480 |
| Test base classes | Add repository injections | P3 | 50 |
| E2E helpers | Add API call methods | P3 | 100 |

**Total:** 5 files, 1,030 LOC

---

## Conclusion

**Problem:** Happy Path tests exist but verify nothing meaningful, creating dangerous false confidence.

**Solution:** Implement comprehensive verification of activity execution, database state, and side effects for all BDD-WF-HP scenarios.

**Impact:** Tests will actually verify the business logic they claim to test, eliminating false confidence and ensuring workflow correctness.

**Timeline:** 2 weeks implementation, focused on the highest business impact area.

---

**Analysis Date:** 2026-04-16
**Status:** Ready for implementation
**Priority:** Implement Happy Path verification immediately - this affects all transaction workflows