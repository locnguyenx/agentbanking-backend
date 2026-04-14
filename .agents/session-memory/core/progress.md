# Session Memory - Progress

**Project:** Agent Banking Platform  
**Last Update:** 2026-04-14

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
| 2026-04-13 | Backoffice: Fix "Create User Account" for agents (400 error) | ✅ |
| 2026-04-13 | Balance API: Fix 400 error (gateway rewrite + missing agent float) | ✅ |
| 2026-04-13 | Tests: Add 21 unit tests for Kafka event publish/consume | ✅ |
| 2026-04-14 | BDD Test Alignment: Verify workflow selection, not just HTTP 202 | ✅ |
| 2026-04-14 | Auth Security: Fix /api/v1/auth/** endpoint access | ✅ |
| 2026-04-14 | Infrastructure: Create missing DB tables via Docker exec | ✅ |

### Key Discovery

**Root Cause 1:** Activities were being called directly as Spring beans instead of through Temporal's activity proxy.

**Root Cause 2:** Gateway was dropping `transactionType` and other fields for `/api/v1/transactions` because it lacked a pass-through mode for orchestrated requests.

**Root Cause 3:** `docker compose build` does not run Gradle `assemble`, leading to stale JARs in containers.

**Root Cause 4:** AgentUserController required `agentCode` in request body, but agent ID was in URL path - backend should query onboarding-service.

**Root Cause 5:** Gateway rewrite `/api/v1/agent/balance` to `/internal/balance` dropped query param; agent float not created when user creation fails.

**Root Cause 6:** No unit tests for Kafka event consumers/publishers.

### Verification

- Verified 45/45 tests passing in `SelfContainedOrchestratorE2ETest`.
- Verified `X-Agent-Id` validation prevents `DataConverterException` in Orchestrator.
- Verified "Create User Account" works via curl test.
- Verified balance API returns correct response.
- Verified all 21 new Kafka event tests pass.

## 📈 Test Results

- **SelfContainedOrchestratorE2ETest**: 45 tests, 0 failed (100% success)
- **Kafka Event Consumer Tests**: 10 tests, 0 failed
- **Kafka Event Publisher Tests**: 11 tests, 0 failed
- **Total New Tests**: 21 tests, all passing ✅
