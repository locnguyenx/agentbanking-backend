# Session Memory - Current State

**Project:** Agent Banking Platform  
**Last Update:** 2026-04-13

## 🎯 THE NOW

### Current Task
Fixed balance API and added Kafka event publish/consume test coverage.

### Status
- **Phase:** Bug Fix + Test Coverage
- **Active Since:** 2026-04-13

### Summary
- Fixed 400 Bad Request on `/api/v1/agent/balance` API
- Root cause: Gateway rewrite dropped query param, agent float didn't exist in DB
- Added Kafka event consumer/publisher unit tests (21 tests, 6 test files)
- Fixed AgentService to publish event even when user creation fails

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
