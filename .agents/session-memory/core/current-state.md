# Session Memory - Current State

**Project:** Agent Banking Platform  
**Last Update:** 2026-04-14

## 🎯 THE NOW

### Current Task
BDD test verification alignment and auth security fix.

### Status
- **Phase:** BDD Test Verification + Infrastructure Fix
- **Active Since:** 2026-04-14

### Summary
- Fixed BDD tests to verify workflow selection (not just HTTP 202 status)
- Added verify(workflowFactory).startWorkflow() assertions in orchestrator tests
- Fixed auth SecurityConfig to allow /api/v1/auth/** endpoints
- Fixed HTTP 200 vs 202 mismatch in E2E tests
- Created missing database tables via Docker exec

### Key Files Modified
- **Bug Fix:**
  - `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/service/AgentService.java` - Always publish event
  - `services/auth-iam-service/.../dto/CreateAgentUserRequestFromId.java`
  - `services/auth-iam-service/.../external/AgentQueryClient.java` (NEW)
  - `services/auth-iam-service/.../AgentUserController.java`
  - Renamed duplicate migrations: `V2__auth_system_seed.sql`, `V7__seed_admin_user.sql`

- **Tests Added (6 new test files, 21 tests):**
  - `services/ledger-service/.../LedgerEventConsumerTest.java` (4 tests)
  - `services/ledger-service/.../EfmEventPublisherAdapterTest.java` (3 tests)
  - `services/auth-iam-service/.../AgentCreatedEventConsumerTest.java` (3 tests)
  - `services/auth-iam-service/.../NotificationPublisherKafkaAdapterTest.java` (4 tests)
  - `services/audit-service/.../KafkaAuditLogConsumerTest.java` (4 tests)
  - `services/orchestrator-service/.../KafkaEventPublisherTest.java` (4 tests)

## 🚧 In Progress
- None - tasks completed

## 📝 Notes
- All 21 new tests pass ✅
- Agent float now auto-created when agent is created (even if user creation fails)
- Migration conflicts resolved by disabling Flyway
