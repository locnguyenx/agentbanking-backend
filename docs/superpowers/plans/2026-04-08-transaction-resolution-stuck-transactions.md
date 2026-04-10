# Transaction Resolution - Stuck Transactions & STP Auto-Creation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement stuck transactions list endpoint and auto-creation of resolution cases when STP evaluation returns NON_STP or CONDITIONAL_STP (not approved).

**Architecture:** 
- Add GET endpoint to list stuck transactions from transaction_record table (status: PENDING, COMPENSATING, FAILED, PENDING_REVIEW)
- Create SaveResolutionCaseActivity to auto-create resolution cases in workflows when STP returns non-approved
- Update backoffice UI to show stuck transactions

**Tech Stack:** Java 21, Spring Boot 3, Temporal SDK, React + TypeScript

---

## Current State Analysis

### What's Already Implemented (from existing plan):
- ✅ ResolutionController with maker-propose, checker-approve, checker-reject endpoints
- ✅ TransactionResolution.tsx UI page
- ✅ Gateway routes for backoffice transaction endpoints
- ✅ WorkflowStatus.PENDING_REVIEW enum
- ✅ EvaluateStpActivity added to workflows

### What's Missing:
- ❌ GET endpoint to list stuck transactions
- ❌ Backoffice UI to show stuck transactions
- ❌ Auto-creation of resolution cases when STP returns NON_STP/CONDITIONAL_STP (not approved)

---
## Explain the Enhancement
### When System Returns NON_STP / CONDITIONAL_STP
The STP (Straight-Through Processing) decision is evaluated in StpDecisionService based on two checks:
Category	Condition
FULL_STP	✅ Both velocity check AND daily limit passed → auto-approve
CONDITIONAL_STP	⚠️ Only one passed (velocity OR limit) → requires manual review
NON_STP	❌ Neither passed (both velocity AND limit failed) → requires manual maker-checker

**The Two Checks:**
1. Velocity Check - Limits transaction frequency/amount in a day
2. Daily Limit Check - Enforces per-transaction and cumulative daily limits

Example:
- Agent has done 15 transactions today (velocity limit: 20 max) 
- AND tries to withdraw RM 8,000 (daily limit: RM 5,000)
- Both fail → NON_STP → Transaction stuck → Resolution case created

Flow:
Transaction → EvaluateStpActivity → 
  ├── FULL_STP → Continue to processing
  ├── CONDITIONAL_STP → Create resolution case → PENDING_REVIEW
  └── NON_STP → Create resolution case → PENDING_REVIEW
___

### Propose Solution

**The Problem**
When you click Propose Resolution on a stuck transaction, the backend looked for an existing "Resolution Case" in the database. If none existed, it threw an error: "Resolution case not found for workflow: XYZ"
Why This Happened
The original design assumed:
1. A transaction becomes stuck → STP returns NON_STP/CONDITIONAL_STP
2. The workflow automatically creates a Resolution Case in the database
3. Admin then finds the case and proposes a resolution
But that auto-creation wasn't implemented yet. So when you tried to propose:
- Database: empty (no case)
- Backend: "case not found" → 400 Bad Request
The Fix
I changed ResolutionService.makerPropose() to:
1. If no case exists → create a new one automatically
2. Then apply the maker's proposal
This makes the flow work even without the auto-creation from workflow.
The Full Flow Should Be
Transaction starts → STP evaluation → NON_STP/CONDITIONAL_STP
                                                      ↓
                                          Workflow calls SaveResolutionCaseActivity
                                                      ↓
                                          Resolution Case created in DB (PENDING_MAKER)
                                                      ↓
Admin views stuck transactions → Propose Resolution → Update existing case
                                                      ↓
Admin approves → Transaction resolved
Currently, we're skipping step 2 (auto-creation) and directly creating the case when proposing.

---

## Phase 1: Add Stuck Transactions Endpoint

### Task 1: Add findStuckTransactions to TransactionRecordRepository

**Files:**
- Modify: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/TransactionRecordRepository.java`

- [x] **Step 1: Add method to interface**

```java
List<TransactionRecordDTO> findStuckTransactions();
```

### Task 2: Add findStuckTransactions to JpaRepository

**Files:**
- Modify: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/persistence/repository/TransactionRecordJpaRepository.java`

- [x] **Step 1: Add query method**

```java
@Query("SELECT t FROM TransactionRecordEntity t WHERE t.status IN ('PENDING', 'COMPENSATING', 'FAILED', 'PENDING_REVIEW') ORDER BY t.createdAt DESC")
List<TransactionRecordEntity> findStuckTransactions();
```

### Task 3: Implement findStuckTransactions in RepositoryImpl

**Files:**
- Modify: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/persistence/repository/TransactionRecordRepositoryImpl.java`

- [x] **Step 1: Add implementation**

```java
@Override
public List<TransactionRecordDTO> findStuckTransactions() {
    return jpaRepository.findStuckTransactions().stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
}
```

### Task 4: Add GET /stuck Endpoint

**Files:**
- Modify: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/web/ResolutionController.java`

- [ ] **Step 1: Add TransactionRecordRepository import and dependency**

```java
import com.agentbanking.orchestrator.domain.port.out.TransactionRecordRepository;

// Add field
private final TransactionRecordRepository transactionRecordRepository;

// Update constructor
public ResolutionController(..., TransactionRecordRepository transactionRecordRepository) {
    ...
    this.transactionRecordRepository = transactionRecordRepository;
}
```

- [ ] **Step 2: Add GET endpoint**

```java
@GetMapping("/stuck")
public ResponseEntity<?> getStuckTransactions() {
    var stuckTransactions = transactionRecordRepository.findStuckTransactions();
    var content = stuckTransactions.stream()
        .map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("workflowId", t.workflowId());
            map.put("transactionId", t.id() != null ? t.id().toString() : "");
            map.put("transactionType", t.transactionType() != null ? t.transactionType().name() : "");
            map.put("agentId", t.agentId() != null ? t.agentId().toString() : "");
            map.put("amount", t.amount());
            map.put("status", t.status());
            map.put("errorCode", t.errorCode() != null ? t.errorCode() : "");
            map.put("errorMessage", t.errorMessage() != null ? t.errorMessage() : "");
            map.put("createdAt", t.createdAt() != null ? t.createdAt().toString() : "");
            map.put("completedAt", t.completedAt() != null ? t.completedAt().toString() : "");
            return map;
        })
        .toList();
    
    Map<String, Object> response = new HashMap<>();
    response.put("content", content);
    response.put("total", content.size());
    return ResponseEntity.ok(response);
}
```

---

## Phase 2: Update Backoffice UI

### Task 5: Add getStuckTransactions API Method

**Files:**
- Modify: `backoffice/src/api/client.ts`

- [ ] **Step 1: Add API method**

```typescript
getStuckTransactions: () => 
  client.get('/backoffice/transactions/stuck').then((r) => r.data),
```

### Task 6: Update TransactionResolution.tsx

**Files:**
- Modify: `backoffice/src/pages/TransactionResolution.tsx`

- [ ] **Step 1: Add StuckTransactionItem interface**

```typescript
interface StuckTransactionItem {
  workflowId: string
  transactionId: string
  transactionType: string
  agentId: string
  amount: number
  status: string
  errorCode: string
  errorMessage: string
  createdAt: string
  completedAt: string
}
```

- [ ] **Step 2: Update query to use getStuckTransactions**

```typescript
const { data: stuckResponse } = useQuery({
  queryKey: ['stuckTransactions', statusFilter],
  queryFn: async () => {
    const response = await api.getStuckTransactions()
    return response as { content: StuckTransactionItem[] }
  }
})

const stuckItems = stuckResponse?.content || []

// Map stuck transactions to resolution items for display
const resolutionItems: ResolutionItem[] = stuckItems.map(item => ({
  workflowId: item.workflowId,
  transactionId: item.transactionId || '',
  agentId: item.agentId || '',
  agentName: item.agentName || 'Unknown',
  amount: item.amount || 0,
  currency: 'MYR',
  transactionType: item.transactionType || '',
  status: mapTransactionStatusToResolutionStatus(item.status),
  createdAt: item.createdAt || '',
  makerProposedAt: item.makerProposedAt || undefined,
  makerAction: item.makerAction,
  makerReason: item.makerReason,
  makerReasonCode: item.makerReasonCode,
  checkerApprovedAt: item.checkerApprovedAt || undefined,
  checkerRejectedAt: item.checkerRejectedAt || undefined,
  checkerReason: item.checkerReason
}))
```

- [ ] **Step 3: Add status mapping function**

```typescript
const mapTransactionStatusToResolutionStatus = (status: string): ResolutionItem['status'] => {
  switch (status) {
    case 'PENDING':
    case 'PENDING_REVIEW':
      return 'PENDING_MAKER'
    case 'FAILED':
    case 'COMPENSATING':
      return 'PENDING_MAKER'
    default:
      return 'PENDING_MAKER'
  }
}
```

---

## Phase 3: STP Auto-Creation

### Task 7: Create SaveResolutionCaseActivity Interface

**Files:**
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/SaveResolutionCaseActivity.java`

- [ ] **Step 1: Create the interface**

```java
package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import java.util.UUID;

@ActivityInterface
public interface SaveResolutionCaseActivity {

    record Input(
        String workflowId,
        UUID transactionId,
        String reasonCode,
        String reason
    ) {}

    @ActivityMethod
    void saveResolutionCase(Input input);
}
```

### Task 8: Create SaveResolutionCaseActivity Implementation

**Files:**
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/SaveResolutionCaseActivityImpl.java`

- [ ] **Step 1: Create the implementation**

```java
package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.SaveResolutionCaseActivity;
import com.agentbanking.orchestrator.domain.model.TransactionResolutionCase;
import com.agentbanking.orchestrator.domain.port.out.ResolutionCaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SaveResolutionCaseActivityImpl implements SaveResolutionCaseActivity {

    private static final Logger log = LoggerFactory.getLogger(SaveResolutionCaseActivityImpl.class);
    private final ResolutionCaseRepository resolutionCaseRepository;

    public SaveResolutionCaseActivityImpl(ResolutionCaseRepository resolutionCaseRepository) {
        this.resolutionCaseRepository = resolutionCaseRepository;
    }

    @Override
    public void saveResolutionCase(Input input) {
        log.info("Creating resolution case for workflow: {}, reasonCode: {}",
            input.workflowId(), input.reasonCode());

        var resolutionCase = TransactionResolutionCase.createPendingMaker(
            UUID.fromString(input.workflowId()),
            input.transactionId()
        );

        var updatedCase = resolutionCase.makerPropose(
            null,
            "SYSTEM",
            input.reasonCode(),
            input.reason(),
            null
        );

        resolutionCaseRepository.save(updatedCase);
        log.info("Resolution case created with id: {}", updatedCase.id());
    }
}
```

### Task 9: Update Workflows to Call SaveResolutionCaseActivity

**Files to modify:**
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/WithdrawalWorkflowImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/DepositWorkflowImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/BillPaymentWorkflowImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/DuitNowTransferWorkflowImpl.java`

**For each workflow:**

- [ ] **Step 1: Add field declaration**

```java
private final SaveResolutionCaseActivity saveResolutionCaseActivity;
```

- [ ] **Step 2: Add activity stub initialization**

```java
this.saveResolutionCaseActivity = Workflow.newActivityStub(SaveResolutionCaseActivity.class, defaultOptions);
```

- [ ] **Step 3: Update STP evaluation block to create resolution case**

```java
StpDecision stpDecision = evaluateStpActivity.evaluateStp(...);
if (!stpDecision.approved()) {
    if (stpDecision.category().equals("NON_STP") || 
        stpDecision.category().equals("CONDITIONAL_STP")) {
        saveResolutionCaseActivity.saveResolutionCase(
            new SaveResolutionCaseActivity.Input(
                input.idempotencyKey(),
                null,
                stpDecision.category(),
                stpDecision.reason()
            )
        );
    }
    currentStatus = WorkflowStatus.PENDING_REVIEW;
    return WorkflowResult.failed("ERR_STP_REVIEW", stpDecision.reason(), "REVIEW");
}
```

### Task 10: Register Activity in TemporalWorkerConfig

**Files:**
- Modify: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/config/TemporalWorkerConfig.java`

- [ ] **Step 1: Add bean registration**

```java
@Bean
public SaveResolutionCaseActivity saveResolutionCaseActivity(ResolutionCaseRepository repository) {
    return new SaveResolutionCaseActivityImpl(repository);
}
```

- [ ] **Step 2: Register worker**

```java
worker.registerActivityImplementations(
    saveResolutionCaseActivity(...),
    // other activities
);
```

---

## Phase 4: Build and Test

### Task 11: Build and Deploy

- [ ] **Step 1: Compile orchestrator-service**

```bash
cd services/orchestrator-service && ./gradlew compileJava -q
```

- [ ] **Step 2: Build bootJar**

```bash
./gradlew :services:orchestrator-service:bootJar -q
```

- [ ] **Step 3: Rebuild Docker container**

```bash
docker compose build --no-cache orchestrator-service
```

- [ ] **Step 4: Restart services**

```bash
docker compose up -d orchestrator-service backoffice
```

- [ ] **Step 5: Test the endpoint**

```bash
curl -s http://localhost:8086/api/v1/backoffice/transactions/stuck
```

Expected: JSON with stuck transactions from transaction_record table

---

## Summary

| Task | Description | Status |
|------|-------------|--------|
| 1-3 | Add findStuckTransactions to repository | ✅ Done |
| 4 | Add GET /stuck endpoint | 🔄 In Progress |
| 5-6 | Update backoffice UI | Pending |
| 7-8 | Create SaveResolutionCaseActivity | Pending |
| 9 | Update workflows | Pending |
| 10 | Register activity in TemporalWorkerConfig | Pending |
| 11 | Build and test | Pending |

---

## Implementation Notes

### Force Resolve vs Propose Resolution
Based on spec Section 9.3 and 18.6:
- **Force Resolve** is for manual backoffice admin action to force COMMIT/REVERSE on stuck transactions (workflows in COMPENSATING state > 4 hours)
- **Propose Resolution** (maker-propose) is part of the Maker-Checker workflow where maker proposes action and checker approves/rejects

The existing `proposeResolution` endpoint with `action: 'COMMIT' | 'REVERSE'` is correct for the Maker-Checker workflow. Force Resolve would be a separate simpler flow.

### API Flow After Implementation
1. Transaction starts → EvaluateStpActivity returns NON_STP/CONDITIONAL_STP (not approved) -> when it returns NON_STP/CONDITIONAL_STP?
2. SaveResolutionCaseActivity creates resolution case with PENDING_MAKER status
3. Backoffice shows stuck transaction in Transaction Resolution list
4. Maker clicks "Propose" → selects COMMIT/REVERSE → status → PENDING_CHECKER
5. Checker reviews → approves/rejects
6. On approval, Temporal signal sent to workflow to continue/rollback