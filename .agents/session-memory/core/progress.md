# Session Memory - Progress

**Project:** Agent Banking Platform  
**Last Update:** 2026-04-15

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
| 2026-04-15 | Backoffice UI: Fix "No workflows found" orchestrator issue | ✅ |
| 2026-04-15 | Fix workflow PENDING status - 4 root causes resolved | ✅ |
| 2026-04-15 | - Missing @ActivityImpl on PersistWorkflowResultActivityImpl | ✅ |
| 2026-04-15 | - Duplicate activity type names in 7 Validate interfaces | ✅ |
| 2026-04-15 | - Hardcoded short timeouts in 13 workflow implementations | ✅ |
| 2026-04-15 | - NullPointerException in rules-service endpoints | ✅ |
| 2026-04-15 | Switch-adapter service: Fix port 8083->8084 in default URL | ✅ |
| 2026-04-15 | STP evaluation: Fix nullPointerException for null agentTier | ✅ |
| 2026-04-15 | Backoffice UI: Fix Create Case button for COMPENSATING/PENDING | ✅ |
| 2026-04-15 | Orphan case: Add isOrphan detection in ResolutionController | ✅ |
| 2026-04-16 | Dashboard agent counts: Fix to use registered agents instead of transaction agents | ✅ |
| 2026-04-16 | Agents page stats: Fix to calculate from complete dataset, not paginated results | ✅ |
| 2026-04-16 | Frontend caching: Add no-cache headers, update React Query keys | ✅ |
| 2026-04-16 | OpenAPI spec: Fix agentTier enum from TIER_1/2/3 to MICRO/STANDARD/PREMIER | ✅ |

### Key Discovery

**Root Cause 1:** Activities were being called directly as Spring beans instead of through Temporal's activity proxy.

**Root Cause 2:** Gateway was dropping `transactionType` and other fields for `/api/v1/transactions` because it lacked a pass-through mode for orchestrated requests.

**Root Cause 3:** `docker compose build` does not run Gradle `assemble`, leading to stale JARs in containers.

**Root Cause 4:** AgentUserController required `agentCode` in request body, but agent ID was in URL path - backend should query onboarding-service.

**Root Cause 5:** Gateway rewrite `/api/v1/agent/balance` to `/internal/balance` dropped query param; agent float not created when user creation fails.

**Root Cause 6:** No unit tests for Kafka event consumers/publishers.

**Root Cause 7:** Orchestrator test configuration had `ddl-auto: create-drop` and `flyway.enabled: false`, causing database tables to be dropped.

### Verification

- Verified 45/45 tests passing in `SelfContainedOrchestratorE2ETest`.
- Verified `X-Agent-Id` validation prevents `DataConverterException` in Orchestrator.
- Verified "Create User Account" works via curl test.
- Verified balance API returns correct response.
- Verified all 21 new Kafka event tests pass.
- Verified backoffice UI now displays workflows created by E2E tests.

## 📈 Test Results

- **SelfContainedOrchestratorE2ETest**: 45 tests, 0 failed (100% success)
- **Kafka Event Consumer Tests**: 10 tests, 0 failed
- **Kafka Event Publisher Tests**: 11 tests, 0 failed
- **Total New Tests**: 21 tests, all passing ✅
- **Backoffice UI**: Now properly displays workflows ✅
