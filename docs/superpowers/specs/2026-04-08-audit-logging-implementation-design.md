# Audit Logging Implementation Design

**Date:** 2026-04-08  
**Status:** Implementation Plan  
**Goal:** Add audit logging to all services using common module

---

## Current State

### Common Module (Already Exists)
- `common/src/main/java/com/agentbanking/common/audit/AuditLogService` - interface
- `common/src/main/java/com/agentbanking/common/audit/AuditLogRecord` - record
- `common/src/main/java/com/agentbanking/common/audit/AuditAction` - enum (incomplete)
- `common/src/main/java/com/agentbanking/common/audit/AuditEventPublisher` - Kafka publisher

### Services Current Status

| Service | Uses common | Has AuditLogService impl | DB table |
|---------|-------------|-------------------------|----------|
| auth-iam-service | ✅ | ❌ (has own AuditService) | ✅ |
| onboarding-service | ✅ | ✅ | ✅ |
| rules-service | ✅ | ❌ | ✅ |
| biller-service | ✅ | ❌ | ✅ |
| switch-adapter-service | ❌ | ❌ | ✅ |
| ledger-service | ❌ | ❌ | ✅ |
| orchestrator-service | ❌ | ❌ | ✅ |

---

## Requirements

All operations must log:
- Timestamp (ISO 8601)
- User ID
- Action performed
- Resource accessed
- IP address
- Result (SUCCESS/FAILURE)

---

## Implementation Steps

### Phase 1: Enhance Common Module

1. **Expand AuditAction enum** - Add all action types needed across services
   ```
   CREATE, UPDATE, DELETE, LOGIN, LOGOUT, REFRESH_TOKEN, REVOKE_TOKEN,
   LOCK_USER, UNLOCK_USER, RESET_PASSWORD, CHANGE_PASSWORD,
   AGENT_CREATED, AGENT_UPDATED, AGENT_DEACTIVATED, AGENT_ACTIVATED,
   TRANSACTION_CREATED, TRANSACTION_COMPLETED, TRANSACTION_FAILED, TRANSACTION_REVERSED,
   PAYMENT_INITIATED, PAYMENT_COMPLETED, PAYMENT_FAILED,
   KYC_SUBMITTED, KYC_APPROVED, KYC_REJECTED
   ```

2. **Add AuditLogService to common** - Already exists, just need to ensure it's complete

### Phase 2: Auth IAM Service (Refactor)

1. Create `AuditLogServiceImpl` implementing common's `AuditLogService`
2. Add AuditLogRepository port & implementation
3. Add `AuditLogEntity` and `AuditLogMapper`
4. Wire `AuditLogService` into controllers
5. Replace calls to existing `AuditService` with `AuditLogService`

### Phase 3: Services Missing Implementation

For each: rules, biller
1. Create `AuditLogServiceImpl` implementing common's `AuditLogService`
2. Add AuditLogRepository port & implementation
3. Add `AuditLogEntity` and `AuditLogMapper`
4. Wire into controllers

### Phase 4: Services Missing Common Dependency

For each: switch-adapter-service, ledger-service, orchestrator-service
1. Add common dependency to build.gradle
2. Create `AuditLogServiceImpl` implementing common's `AuditLogService`
3. Add AuditLogRepository port & implementation
4. Add `AuditLogEntity` and `AuditLogMapper`
5. Wire into controllers

### Phase 5: Wire Audit into Controllers

Each service needs to call `auditLogService.log()` after:
- POST (create)
- PUT (update)
- DELETE (delete)
- Login/logout
- Important business actions

---

## Implementation Order

1. Expand AuditAction enum in common
2. Auth IAM Service - refactor to use common
3. Onboarding Service - already done, verify
4. Rules Service - add implementation
5. Biller Service - add implementation
6. Switch Adapter - add common dep + implementation
7. Ledger Service - add common dep + implementation
8. Orchestrator Service - add common dep + implementation

---

## Files to Create/Modify

### Common Module
- `common/src/main/java/com/agentbanking/common/audit/AuditAction.java` - expand

### Per Service (template)
- `domain/port/out/AuditLogRepository.java` - interface
- `infrastructure/persistence/AuditLogEntity.java` - JPA entity
- `infrastructure/persistence/AuditLogMapper.java` - mapper
- `infrastructure/persistence/AuditLogRepositoryImpl.java` - implementation
- `application/usecase/AuditLogServiceImpl.java` - implements common.AuditLogService
- `infrastructure/config/DomainServiceConfig.java` - register bean
- Controllers - inject and call auditService.log()

### Build Gradle Changes
- switch-adapter-service, ledger-service, orchestrator-service: add `implementation project(':common')`

---

## Verification

Each service should have:
- GET /internal/audit-logs returning data
- Controllers logging after operations
- Tests for audit logging