# Fix STP Evaluation - Implementation Plan

> **Date:** 2026-04-10
> **Issue:** All transactions from channel app are in PENDING status - STP evaluation not working

## Root Cause Summary

| # | Issue | Why Tests Don't Catch |
|---|-------|----------------------|
| 1 | **Feign client uses `@RequestParam` but controller expects `@RequestBody`** | Tests mock `RulesServiceClient` - no real HTTP call |
| 2 | **Incomplete STP data** - missing agentTier, transactionCountToday, etc. | Tests mock the adapter, not the actual call |
| 3 | **Exception swallowed** - workflow catches all exceptions and returns PENDING_REVIEW | Error handling masks the actual failure |

The call **SHOULD** throw an exception (400 Bad Request from rules-service), but the workflow's catch block (line 207) converts it to a failed `WorkflowResult` with status `PENDING_REVIEW`, masking the real error.

---

## Implementation Tasks

### Phase 0: Add Contract Test (Prevents Future Regression)

**File:** `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/integration/RulesServiceContractTest.java`

Add test that verifies the Feign client contract matches the controller.

---

### Phase 1: Fix STP Evaluation Data Flow

#### Task 1: Fix RulesServiceClient

**File:** `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/RulesServiceClient.java`

- Change `@RequestParam` to `@RequestBody`
- Use proper request DTO instead of individual parameters

#### Task 2: Update RulesServicePort and Adapter

**Files:**
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/RulesServicePort.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/RulesServiceAdapter.java`

Update `evaluateStp` signature to accept complete request object.

#### Task 3: Update EvaluateStpActivity

**Files:**
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/EvaluateStpActivity.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/EvaluateStpActivityImpl.java`

Add more parameters to Input and pass all required data to adapter.

#### Task 4: Update All 4 Workflow Implementations

**Files:**
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/WithdrawalWorkflowImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/DepositWorkflowImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/BillPaymentWorkflowImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/DuitNowTransferWorkflowImpl.java`

Update the `evaluateStpActivity.evaluateStp()` call to pass:
- transactionType
- agentId
- amount
- customerProfile
- **agentTier** (from input)
- **transactionCountToday** (use 0 for now)
- **amountToday** (use "0" for now)
- **todayTotalAmount** (use "0" for now)

---

### Phase 2: Fix pendingReason Persistence

#### Task 5: Add pendingReason to TransactionRecordRepository

**Files:**
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/TransactionRecordRepository.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/persistence/repository/TransactionRecordJpaRepository.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/persistence/repository/TransactionRecordRepositoryImpl.java`

Add `pendingReason` parameter to `updateStatus()` method.

#### Task 6: Add pendingReason to PersistWorkflowResultActivity

**Files:**
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/PersistWorkflowResultActivity.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/PersistWorkflowResultActivityImpl.java`

Add `pendingReason` to Input record and pass to repository.

#### Task 7: Update Workflows to Pass pendingReason

In each workflow's STP failure block, pass `stpDecision.reason()` as pendingReason to `persistWorkflowResultActivity`.

---

### Phase 3: Improve Error Handling

#### Task 8: Don't Mask Exceptions

In workflows, distinguish between:
- **Expected failures** (STP review, velocity check) → return PENDING_REVIEW
- **Unexpected failures** (Feign error, timeout) → throw or return FAILED with clear error

---

### Phase 4: Build and Test

#### Task 9: Compile and Build

```bash
cd services/orchestrator-service && ./gradlew compileJava -q
./gradlew :services:orchestrator-service:bootJar -q
docker compose build --no-cache orchestrator-service
docker compose up -d orchestrator-service rules-service
```

#### Task 10: Verify STP Works End-to-End

Test STP endpoint directly and verify transaction flow.

---

## Summary

| Phase | Tasks | Description |
|-------|-------|-------------|
| 0 | Test | Add contract test to catch mismatch |
| 1 | 1-4 | Fix Feign client, pass complete STP data |
| 2 | 5-7 | Fix pendingReason persistence |
| 3 | 8 | Improve error handling |
| 4 | 9-10 | Build and verify |

---

## Test Coverage Summary

### Unit Tests (orchestrator-service)
- `ResolutionServiceTest.java` - Tests resolution case state transitions
- `TransactionResolutionCaseTest.java` - Tests domain model behavior

### Integration Tests (orchestrator-service)
- `OrchestratorControllerIntegrationTest.java` - Tests controller endpoints
- `RulesServiceContractTest.java` - NEW: Verifies Feign client contract matches controller

### Integration Tests (rules-service)
- `RulesControllerIntegrationTest.java` - Tests STP, velocity, fee calculation endpoints
- `StpDecisionServiceTest.java` - Tests STP decision logic

### E2E Tests (gateway)
- `SelfContainedOrchestratorE2ETest.java` - Full transaction flow tests with real backend
- Tests cover: workflow dispatch, polling, idempotency, workflow lifecycle, happy paths, edge cases
- **NEW: BDD-STP** - 7 new tests for STP evaluation end-to-end:
  - BDD-STP-01: Full STP transaction completes immediately
  - BDD-STP-02: High value triggers PENDING_REVIEW (NON_STP)
  - BDD-STP-03: Deposit - FULL_STP path
  - BDD-STP-04: Bill payment - FULL_STP path
  - BDD-STP-05: DuitNow transfer - FULL_STP path
  - BDD-STP-06: Invalid transaction - rejects at gateway
  - BDD-STP-07: Verify pendingReason is captured in PENDING_REVIEW

---

## Related Files to Modify

### Orchestrator Service
- `src/main/java/com/agentbanking/orchestrator/infrastructure/external/RulesServiceClient.java`
- `src/main/java/com/agentbanking/orchestrator/domain/port/out/RulesServicePort.java`
- `src/main/java/com/agentbanking/orchestrator/infrastructure/external/RulesServiceAdapter.java`
- `src/main/java/com/agentbanking/orchestrator/application/activity/EvaluateStpActivity.java`
- `src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/EvaluateStpActivityImpl.java`
- `src/main/java/com/agentbanking/orchestrator/domain/port/out/TransactionRecordRepository.java`
- `src/main/java/com/agentbanking/orchestrator/infrastructure/persistence/repository/TransactionRecordJpaRepository.java`
- `src/main/java/com/agentbanking/orchestrator/infrastructure/persistence/repository/TransactionRecordRepositoryImpl.java`
- `src/main/java/com/agentbanking/orchestrator/application/activity/PersistWorkflowResultActivity.java`
- `src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/PersistWorkflowResultActivityImpl.java`
- `src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/WithdrawalWorkflowImpl.java`
- `src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/DepositWorkflowImpl.java`
- `src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/BillPaymentWorkflowImpl.java`
- `src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/DuitNowTransferWorkflowImpl.java`
- `src/test/java/com/agentbanking/orchestrator/integration/RulesServiceContractTest.java` (new)