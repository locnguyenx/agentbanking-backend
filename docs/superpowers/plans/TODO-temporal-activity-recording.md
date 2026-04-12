# TODO: Temporal Workflow Activity Recording Issue

**Created**: 2026-04-12
**Priority**: HIGH
**Status**: TODO (Root Cause Identified)

## Problem Statement

Temporal workflow executions are not properly recording activity events, leading to incorrect status reporting in the backoffice UI.

### Symptoms

1. **Status Mismatch**: 
   - Temporal shows workflow status as `COMPLETED`
   - Database shows transaction status as `FAILED`
   - Backoffice UI was showing `COMPLETED` (from Temporal) instead of `FAILED` (from database)

2. **Missing Activity Timeline**:
   - The execution details API returns an empty activity timeline
   - Only 5 events recorded in Temporal history:
     - `WORKFLOW_EXECUTION_STARTED`
     - `WORKFLOW_TASK_SCHEDULED`
     - `WORKFLOW_TASK_STARTED`
     - `WORKFLOW_TASK_COMPLETED`
     - `WORKFLOW_EXECUTION_COMPLETED`

## Root Cause Analysis (CONFIRMED)

### Root Cause: Activities Called Directly as Spring Beans

**Evidence from Temporal CLI:**
```bash
$ temporal workflow show -w e2e-stp-duitnow-ffadf650-bfeb-4a79-908f-258942074eef
Progress:
  ID  Time                     Type
    1  2026-04-12T10:48:28Z    WorkflowExecutionStarted
    2  2026-04-12T10:48:28Z    WorkflowTaskScheduled
    3  2026-04-12T10:48:28Z    WorkflowTaskStarted
    4  2026-04-12T10:48:28Z    WorkflowTaskCompleted
    5  2026-04-12T10:48:28Z    WorkflowExecutionCompleted
```

**The workflow completed in ONE task** - all code ran synchronously without any activity scheduling.

### The Problem in Code

Looking at `DuitNowTransferWorkflowImpl.java`:

```java
@Autowired  // ❌ WRONG: Direct Spring bean injection
private CheckVelocityActivity checkVelocityActivity;

@Override
public WorkflowResult execute(DuitNowTransferInput input) {
    // ❌ WRONG: Calling activity directly as Spring bean
    VelocityCheckResult velocityResult = checkVelocityActivity.checkVelocity(...);
}
```

**Activities are being called as Spring beans directly instead of through Temporal's activity proxy.**

This bypasses Temporal's activity recording entirely - no activity events are created in the workflow history.

### Impact

| What Should Happen | What's Actually Happening |
|-------------------|--------------------------|
| Activity scheduled in Temporal | Activity called directly as Spring bean |
| Activity recorded in history | No activity events recorded |
| Can track progress/failures | No visibility |
| Can see retry attempts | No visibility |
| Activity input/output in audit | No data |

## Fix Plan

### Step 1: Fix All Workflow Implementations

All workflow implementations need to use Temporal's activity proxy instead of direct Spring bean injection.

**Before (❌ Wrong):**
```java
@Autowired
private CheckVelocityActivity checkVelocityActivity;

public WorkflowResult execute(DuitNowTransferInput input) {
    checkVelocityActivity.checkVelocity(...);  // Direct call - no recording
}
```

**After (✅ Correct):**
```java
private CheckVelocityActivity checkVelocityActivity;

public DuitNowTransferWorkflowImpl() {
    // Initialize in constructor with Temporal proxy
    this.checkVelocityActivity = Workflow.newActivityStub(
        CheckVelocityActivity.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(5))
            .setRetryOptions(RetryOptions.newBuilder()
                .setMaximumAttempts(3)
                .build())
            .build()
    );
}

public WorkflowResult execute(DuitNowTransferInput input) {
    checkVelocityActivity.checkVelocity(...);  // Through Temporal proxy - recorded!
}
```

### Step 2: Affected Workflows

List of workflow implementations that need fixing:

1. `DuitNowTransferWorkflowImpl.java`
2. `WithdrawalWorkflowImpl.java`
3. `DepositWorkflowImpl.java`
4. `BillPaymentWorkflowImpl.java`
5. `CashlessPaymentWorkflowImpl.java`
6. `PinBasedPurchaseWorkflowImpl.java`
7. `PrepaidTopupWorkflowImpl.java`
8. `EWalletWithdrawalWorkflowImpl.java`
9. `EWalletTopupWorkflowImpl.java`
10. `ESSPPurchaseWorkflowImpl.java`
11. `PINPurchaseWorkflowImpl.java`
12. `RetailSaleWorkflowImpl.java`
13. `HybridCashbackWorkflowImpl.java`

### Step 3: Implementation Pattern

Each workflow needs:
1. Remove `@Autowired` field injection
2. Add private field for activity stub
3. Add `@SuppressWarnings("SpringJavaAutowiredMembersInspection")` to suppress warnings
4. Initialize activity stubs in constructor using `Workflow.newActivityStub()`
5. Set appropriate `ActivityOptions` (timeouts, retries)

### Step 4: Add E2E Test Verification

Add assertions to E2E tests to verify Temporal activity recording is working:

```java
// After workflow execution, verify activities were recorded in Temporal
List<HistoryEvent> events = getWorkflowHistory(workflowId);
boolean hasActivityScheduled = events.stream()
    .anyMatch(e -> e.getEventType() == EventType.EVENT_TYPE_ACTIVITY_TASK_SCHEDULED);

assertTrue(hasActivityScheduled, "Activities should be recorded in Temporal history");
```

### Step 5: Testing

After fix, verify:
1. ✅ Temporal workflow history shows activity events
2. ✅ Activity timeline is populated in backoffice UI
3. ✅ Retry attempts are visible
4. ✅ Activity input/output is recorded
5. **Add E2E test assertions to verify activity recording**

## Current Status

| Component | Status | Notes |
|-----------|--------|-------|
| Backoffice UI | ✅ Working | Shows correct status from database |
| Execution Details API | ⚠️ Partial | Returns database status, but no activity timeline |
| DuitNowTransferWorkflowImpl | ✅ Fixed | Now uses Temporal activity proxy |
| Other Workflows | ❌ Pending | Still need fixing |
| Temporal Recording | ❌ Not working | Will be fixed after all workflows updated |
| E2E Test Verification | ❌ Pending | Need to add assertions |

## Workaround (Current)

- Modified `WorkflowExecutionService` to read status from database instead of Temporal
- Database is now the source of truth for business state
- Backoffice UI shows correct FAILED/COMPLETED status

## Questions Answered

1. **Are E2E tests using real Temporal workflows or mock implementations?**
   - Answer: Real Temporal workflows, but activities are called incorrectly
   
2. **Is there a custom workflow executor bypassing Temporal?**
   - Answer: Activities are called as Spring beans instead of through Temporal proxy
   
3. **What happens when an activity fails - does the workflow record this?**
   - Answer: No - because activities aren't recorded, failures aren't tracked in Temporal
   
4. **Are there any custom filters or interceptors affecting Temporal recording?**
   - Answer: No - the issue is in how activities are invoked in the workflow code

---

**Last Updated**: 2026-04-12
**Root Cause Confirmed by**: Querying Temporal CLI directly
