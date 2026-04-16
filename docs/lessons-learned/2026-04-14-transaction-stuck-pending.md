# Lessons Learned: Transaction Status Always PENDING

**Date:** 2026-04-14  
**Issue:** All transactions remained in PENDING status indefinitely, regardless of success or failure  
**Root Cause:** Multiple issues in Temporal workflow implementation

---

## Problem Summary

Transactions created via `/api/v1/transactions` API remained in PENDING status forever:
- Happy path transactions never reached COMPLETED
- Failed transactions never reached FAILED
- Temporal workflows ran indefinitely with infinite retries
- Database status was never updated

---

## Root Causes Identified

### 1. Hardcoded "DEFAULT" Values Causing Deserialization Errors

**Location:** `StartTransactionUseCaseImpl.java`

Several transaction types used hardcoded "DEFAULT" strings that failed Temporal's deserialization:

```java
// WRONG - "DEFAULT" cannot be deserialized to UUID/BigDecimal
case PIN_BASED_PURCHASE -> new PinBasedPurchaseWorkflow.PinBasedPurchaseInput(
    command.agentId(),
    "DEFAULT",  // ❌ String "DEFAULT" passed where enum/UUID expected
    ...
);

// WRONG - Missing proper values
case PREPAID_TOPUP -> new PrepaidTopupWorkflow.PrepaidTopupInput(
    command.agentId(),
    "DEFAULT",  // ❌ Should be actual telco provider
    ...
);
```

**Error observed:**
```
Cannot deserialize value of type `java.util.UUID` from String "DEFAULT"
Cannot deserialize value of type `java.math.BigDecimal` from String "DEFAULT"
```

**Impact:** Workflow activities failed with deserialization errors, triggering infinite retry loops (400+ attempts observed).

---

### 2. Missing Retry Configuration in Workflow Activities

**Location:** `WithdrawalWorkflowImpl.java` (and other workflow implementations)

Activities were created without explicit retry options:

```java
// WRONG - Default retry configuration (infinite retries)
this.blockFloatActivity = Workflow.newActivityStub(BlockFloatActivity.class, 
    ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofMinutes(2))
        .build());
```

**Design Requirement from `2026-04-05-transaction-orchestrator-temporal-design.md`:**

| Activity | StartToClose | Retry Policy |
|----------|--------------|--------------|
| CheckVelocity | 5s | 3x (1s→2s→4s) |
| CalculateFees | 3s | 3x (1s→2s→4s) |
| BlockFloat | 5s | 0x (fail fast) |
| AuthorizeAtSwitch | 25s | 0x (no retry) |
| CommitFloat | 5s | 3x (1s→2s→4s) |
| SendReversalToSwitch | 10s/60s | Infinite (Store & Forward) |

**Impact:** Activities retried indefinitely instead of failing fast and updating status.

---

### 3. Temporal Queue Accumulated Stuck Workflows

**Observation:** 1045+ workflow executions stuck in retry loop

When activities fail due to deserialization errors:
1. Workflow starts
2. Activity fails with `DataConverterException`
3. Temporal retries the activity (indefinitely with default settings)
4. Workflow never progresses past the failing activity
5. Database status remains PENDING forever

---

## Solutions Implemented

### Fix 1: Replace "DEFAULT" with Valid Values

**File:** `StartTransactionUseCaseImpl.java`

```java
// FIXED - Use actual valid values
case PIN_BASED_PURCHASE -> new PinBasedPurchaseWorkflow.PinBasedPurchaseInput(
    command.agentId(),
    "PINEZ",  // ✅ Actual PIN provider
    ...
);

case PREPAID_TOPUP -> new PrepaidTopupWorkflow.PrepaidTopupInput(
    command.agentId(),
    "CELCOM",  // ✅ Actual telco provider
    ...
);

case EWALLET_WITHDRAWAL, EWALLET_TOPUP -> ...
    "GRABPAY",  // ✅ Actual e-wallet provider
    ...
);
```

---

### Fix 2: Add Proper Retry Configuration

**File:** `WithdrawalWorkflowImpl.java`

```java
// FIXED - Explicit retry configuration per design spec
this.blockFloatActivity = Workflow.newActivityStub(BlockFloatActivity.class, 
    ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofSeconds(5))
        .setRetryOptions(RetryOptions.newBuilder()
            .setMaximumAttempts(1)  // ✅ No retries for financial ops
            .build())
        .build());

this.checkVelocityActivity = Workflow.newActivityStub(CheckVelocityActivity.class,
    ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofSeconds(5))
        .setRetryOptions(RetryOptions.newBuilder()
            .setInitialInterval(Duration.ofSeconds(1))
            .setMaximumInterval(Duration.ofSeconds(4))
            .setBackoffCoefficient(2)
            .setMaximumAttempts(3)  // ✅ 3 retries with backoff
            .build())
        .build());
```

---

### Fix 3: Add Fail-Fast Interceptor for Deserialization Errors

**File:** `DeserializationErrorInterceptor.java` (NEW)

```java
@Component
public class DeserializationErrorInterceptor extends WorkerInterceptorBase {
    
    @Override
    public ActivityInboundCallsInterceptor interceptActivity(ActivityInboundCallsInterceptor next) {
        return new DeserializationErrorActivityInterceptor(next);
    }
    
    private static class DeserializationErrorActivityInterceptor extends ActivityInboundCallsInterceptorBase {
        
        @Override
        public ActivityOutput execute(ActivityInput input) {
            try {
                return super.execute(input);
            } catch (DataConverterException e) {
                // ✅ Fail fast instead of infinite retries
                throw ApplicationFailure.newNonRetryableFailure(
                    "Deserialization error: " + e.getMessage(),
                    "ERR_DESERIALIZATION_FAILED",
                    e
                );
            }
        }
    }
}
```

**Registration:** `TemporalWorkerConfig.java`

```java
@Bean
public WorkerFactoryOptions workerFactoryOptions() {
    return WorkerFactoryOptions.newBuilder()
            .setWorkerInterceptors(new DeserializationErrorInterceptor())
            .build();
}
```

---

### Fix 4: Clear Temporal Queue

**Command:**
```bash
# Stop and clear Temporal
docker compose stop temporal temporal-ui
docker compose rm -f temporal temporal-ui

# Clear Temporal database (PostgreSQL)
docker exec temporal-postgres psql -U temporal -d temporal \
    -c "DELETE FROM current_executions WHERE workflow_id IS NOT NULL;"

# Restart Temporal
docker compose up -d temporal temporal-ui
```

---

## Verification

### Unit Tests - All Passing
- `QueryWorkflowStatusUseCaseImplTest`: 5 tests PASSED
- `StartTransactionUseCaseImplTest`: 4 tests PASSED

### Integration Tests - All Passing (22 tests)
- `BDDAlignedTransactionIntegrationTest`: 10 tests PASSED
  - BDD-WF-02: TransactionRecord updated with COMPLETED status ✅
  - BDD-WF-03: TransactionRecord updated with FAILED status ✅
  - BDD-TO-01 through BDD-TO-06: Workflow routing tests ✅
  
- `BDDWorkflowLifecycleIntegrationTest`: 12 tests PASSED
  - BDD-WF-EC-W01: Switch DECLINED triggers compensation ✅
  - BDD-WF-EC-W04: Insufficient float fails immediately ✅
  - BDD-WF-EC-W05: Velocity check fails before float block ✅
  - BDD-WF-EC-W06: Fee config not found fails early ✅
  - BDD-WF-EC-W08: Kafka failure still completes workflow ✅

---

## Key Learnings

### 1. Temporal Retry Defaults Are Dangerous

**Default behavior:** Infinite retries on any activity failure
**Solution:** Always specify `RetryOptions` explicitly per activity type

### 2. Workflow Input Types Matter

**Issue:** Temporal uses JSON serialization/deserialization
**Rule:** Never pass placeholder strings ("DEFAULT") where typed values are expected
**Pattern:** Use actual enum values or valid UUIDs/strings

### 3. Design Specs Must Be Implemented Exactly

The design document specified retry policies that weren't implemented:
- Design said: "BlockFloat: Retry 0x"
- Code had: No retry options (defaults to infinite)

### 4. Activity Failures Should Be Explicit

**Anti-pattern:** Let activities fail with generic exceptions
**Pattern:** Wrap in `ApplicationFailure.newNonRetryableFailure()` for non-retryable errors

### 5. Monitor Temporal Queue Health

**Command to check stuck workflows:**
```bash
docker exec temporal-postgres psql -U temporal -d temporal \
    -c "SELECT workflow_id, state, start_time FROM current_executions;"
```

---

## Prevention Checklist

- [ ] Always validate workflow input DTOs before starting workflow
- [ ] Always specify RetryOptions for every Temporal activity
- [ ] Never use placeholder values ("DEFAULT", "PLACEHOLDER") in workflow inputs
- [ ] Add deserialization error interceptor for fail-fast behavior
- [ ] Include activity retry configuration in code reviews
- [ ] Monitor Temporal queue for stuck workflows
- [ ] Write integration tests that verify status transitions

---

## Related Documents

- Design: `docs/superpowers/specs/agent-banking-platform/2026-04-05-transaction-orchestrator-temporal-design.md`
- BDD: `docs/superpowers/specs/agent-banking-platform/2026-04-05-transaction-bdd-addendum.md`
- Previous Fix: `2026-04-11-temporal-worker-registration-fix.md`
