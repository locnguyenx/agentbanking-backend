# Session Memory - Progress

**Project:** Agent Banking Platform  
**Last Update:** 2026-04-13

## 📊 THE HISTORY

### Completed Milestones

| Date | Milestone | Status |
|------|-----------|--------|
| 2026-04-12 | Identify root cause: workflows calling activities as Spring beans | ✅ |
| 2026-04-12 | Fix 14 workflow implementations to use Temporal proxy | ✅ |
| 2026-04-13 | Stabilize E2E tests: Resolve response double-consumption | ✅ |
| 2026-04-13 | Gateway: Pass-through mode for Orchestrator payload preservation | ✅ |
| 2026-04-13 | Gateway: Defensive UUID validation for X-Agent-Id header | ✅ |
| 2026-04-13 | Infrastructure: Fix stale JAR issue (Gradle assemble before docker build) | ✅ |
| 2026-04-13 | Orchestrator: Resolve 400 Bad Request in transaction flows | ✅ |
| 2026-04-13 | Final Verification: 45/45 pass in SelfContainedOrchestratorE2ETest | ✅ |

### Key Discovery

**Root Cause 1:** Activities were being called directly as Spring beans instead of through Temporal's activity proxy.

**Root Cause 2:** Gateway was dropping `transactionType` and other fields for `/api/v1/transactions` because it lacked a pass-through mode for orchestrated requests.

**Root Cause 3:** `docker compose build` does not run Gradle `assemble`, leading to stale JARs in containers.

### Verification

- Verified 45/45 tests passing in `SelfContainedOrchestratorE2ETest`.
- Verified `X-Agent-Id` validation prevents `DataConverterException` in Orchestrator.

## 📈 Test Results

- **SelfContainedOrchestratorE2ETest**: 45 tests, 0 failed (100% success)
- **TransactionResolutionTest**: PASSED
- **MyKadVerifyTest**: PASSED
- **Gateway Stability**: Standardized 60s timeouts for WebTestClient
