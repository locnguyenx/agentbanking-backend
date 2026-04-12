# Session Memory - Progress

**Project:** Agent Banking Platform  
**Last Update:** 2026-04-12

## 📊 THE HISTORY

### Completed Milestones

| Date | Milestone | Status |
|------|-----------|--------|
| 2026-04-12 | Identify root cause: workflows calling activities as Spring beans | ✅ |
| 2026-04-12 | Fix DuitNowTransferWorkflowImpl to use Temporal proxy | ✅ |
| 2026-04-12 | Fix WithdrawalWorkflowImpl to use Temporal proxy | ✅ |
| 2026-04-12 | Fix DepositWorkflowImpl to use Temporal proxy | ✅ |
| 2026-04-12 | Fix BillPaymentWorkflowImpl to use Temporal proxy | ✅ |
| 2026-04-12 | Fix CashlessPaymentWorkflowImpl to use Temporal proxy | ✅ |
| 2026-04-12 | Fix PinBasedPurchaseWorkflowImpl to use Temporal proxy | ✅ |
| 2026-04-12 | Fix PrepaidTopupWorkflowImpl to use Temporal proxy | ✅ |
| 2026-04-12 | Fix EWalletWithdrawalWorkflowImpl to use Temporal proxy | ✅ |
| 2026-04-12 | Fix EWalletTopupWorkflowImpl to use Temporal proxy | ✅ |
| 2026-04-12 | Fix ESSPPurchaseWorkflowImpl to use Temporal proxy | ✅ |
| 2026-04-12 | Fix PINPurchaseWorkflowImpl to use Temporal proxy | ✅ |
| 2026-04-12 | Fix RetailSaleWorkflowImpl to use Temporal proxy | ✅ |
| 2026-04-12 | Fix HybridCashbackWorkflowImpl to use Temporal proxy | ✅ |
| 2026-04-12 | Fix WithdrawalOnUsWorkflowImpl to use Temporal proxy | ✅ |
| 2026-04-12 | Rebuild orchestrator-service Docker container | ✅ |
| 2026-04-12 | Verify activity recording works via Temporal CLI | ✅ |

### Key Discovery

**Root Cause:** Activities were being called directly as Spring beans (`@Autowired`) instead of through Temporal's activity proxy (`Workflow.newActivityStub()`). This bypassed Temporal's activity recording entirely.

**Solution Pattern:**
```java
@SuppressWarnings("SpringJavaAutowiredMembersInspection")
public WorkflowImpl() {
    this.activity = Workflow.newActivityStub(ActivityClass.class, 
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(X))
            .build());
}
```

### Verification

- Used `temporal workflow show --detailed` to confirm `ActivityTaskScheduled` events now appear in workflow history
- Before fix: Only 5 events (workflow completed in one task)
- After fix: Activity events properly recorded with scheduling and execution

## 📈 Test Results

- E2E Tests: 117 tests, 12 failed (pre-existing failures unrelated to this fix)
- Activity recording: Verified working via Temporal CLI
