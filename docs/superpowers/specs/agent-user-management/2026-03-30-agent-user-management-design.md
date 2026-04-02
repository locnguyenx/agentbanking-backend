# Technical Design Specification: Agent & User Management

**Version:** 1.0
**Date:** 2026-03-30
**Status:** Draft
**Module:** Auth & IAM Service (`com.agentbanking.auth`), Onboarding Service (`com.agentbanking.onboarding`)
**BRD Reference:** `docs/superpowers/specs/agent-user-management/2026-03-30-agent-user-management-brd.md`
**BDD Reference:** `docs/superpowers/specs/agent-user-management/2026-03-30-agent-user-management-bdd.md`

---

## 1. Architecture Overview

### Services Involved

```
┌─────────────────────┐     Feign (sync)      ┌──────────────────────┐
│  onboarding-service  │ ──────────────────►  │  auth-iam-service     │
│  (port 8083)         │                      │  (port 8087)          │
│                      │ ◄──────────────────  │                       │
│  AgentController     │     response         │  - User CRUD          │
│  (backoffice CRUD)   │                      │  - Auth/RBAC          │
│                      │                      │  - Password mgmt      │
│  AgentOnboardingCtrl │                      │  - OTP (Redis)        │
│  (KYC flow)          │                      │  - Kafka producer     │
│                      │                      └──────────┬────────────┘
│  Both call           │                                 │
│  AgentService        │                                 │ USER_CREATED
│  .createAgent()      │                                 │ USER_CREATION_FAILED
└──────────┬───────────┘                                 │ PASSWORD_RESET_CONFIRMED
           │                                             │
           │ AGENT_CREATED (Feign fallback)              │
           ▼                                             ▼
    ┌──────────────────────────────────┐    ┌────────────────────────┐
    │          Apache Kafka            │    │ External Notification  │
    │  Topics: agent.lifecycle,        │───►│ Gateway (REST API)     │
    │          user.lifecycle          │    │                        │
    └──────────────────────────────────┘    │ - SMS delivery         │
                                            │ - Email delivery       │
                                            └────────────────────────┘
```

### Alignment with Platform Architecture

Per the platform design (`2026-03-25-agent-banking-platform-design.md`), notifications follow this pattern:
- **Kafka async events** published by the service
- **External Notification Gateway** consumes events via REST API for SMS/email delivery
- No standalone notification microservice — this matches the existing Transaction Orchestrator pattern

### Agent Creation Trigger Points

| Creation Path | Endpoint | User Story | Trigger Point |
|---------------|----------|------------|---------------|
| Backoffice admin | `POST /backoffice/agents` | US-BO01 | `AgentService.createAgent()` → Feign to auth-iam-service |
| KYC micro-agent | `/internal/onboarding/agent/micro/evaluate` | N/A | `AgentOnboardingService` → `AgentService.createAgent()` → Feign |
| KYC standard | `/internal/onboarding/agent/standard/start` | N/A | `AgentOnboardingService` → `AgentService.createAgent()` → Feign |

### Data Flow: Agent Creation (Happy Path)

1. Agent is created via backoffice OR KYC flow → `AgentService.createAgent()` persists agent
2. `AgentService` calls auth-iam-service via Feign to create user (type EXTERNAL, AGENT role)
3. Auth-iam-service publishes `USER_CREATED` to `user.lifecycle` topic
4. Onboarding-service receives `USER_CREATED`, updates `userCreationStatus` → `CREATED`
5. External notification gateway receives `USER_CREATED`, sends temp password via SMS/email

### Data Flow: Agent Creation (Kafka Fallback)

1. `AgentService.createAgent()` → Feign call fails (circuit breaker opens)
2. `AgentService` publishes `AGENT_CREATED` to `agent.lifecycle` topic, sets `userCreationStatus` → `PENDING`
3. Auth-iam-service consumes `AGENT_CREATED`, creates user (idempotent — checks `agentId`)
4. Auth-iam-service publishes `USER_CREATED` to `user.lifecycle`
5. Onboarding-service and notification gateway consume `USER_CREATED`

### Data Flow: Forgot Password

1. User calls `POST /auth/password/forgot` with username
2. Auth-iam-service generates 6-digit OTP, stores in Redis, publishes notification event
3. External notification gateway sends OTP to user's phone/email
4. User calls `POST /auth/password/reset` with username, OTP, new password
5. Auth-iam-service validates OTP (Redis), resets password, publishes `PASSWORD_RESET_CONFIRMED`

---

## 2. Domain Model & Database Schema Changes

### auth-iam-service: User Record (Domain) Changes

```java
// domain/model/UserRecord.java — extended
public record UserRecord(
    UUID userId,
    String username,
    String email,
    String phone,
    String passwordHash,
    String fullName,
    UserStatus status,
    UserType userType,
    UUID agentId,
    String agentCode,
    Boolean mustChangePassword,
    LocalDateTime temporaryPasswordExpiresAt,
    Set<String> permissions,
    Integer failedLoginAttempts,
    LocalDateTime lockedUntil,
    LocalDateTime passwordChangedAt,
    LocalDateTime passwordExpiresAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime lastLoginAt,
    String createdBy
) {}
```

### New Enum

```java
// domain/model/UserType.java
public enum UserType {
    INTERNAL,   // Bank staff
    EXTERNAL    // Agent
}
```

### Database Migration (auth-iam-service): V3

```sql
-- V3__add_user_type_and_password_policy.sql

ALTER TABLE users ADD COLUMN user_type VARCHAR(20) NOT NULL DEFAULT 'INTERNAL';
ALTER TABLE users ADD COLUMN agent_id UUID UNIQUE;
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN temporary_password_expires_at TIMESTAMP WITH TIME ZONE;

-- Add constraint: agent_id only for EXTERNAL users
ALTER TABLE users ADD CONSTRAINT chk_agent_id_user_type
  CHECK (
    (user_type = 'EXTERNAL' AND agent_id IS NOT NULL) OR
    (user_type = 'INTERNAL' AND agent_id IS NULL)
  );

-- System parameters table
CREATE TABLE system_parameters (
    param_key VARCHAR(100) PRIMARY KEY,
    param_value VARCHAR(500) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

INSERT INTO system_parameters (param_key, param_value, description)
VALUES ('temp.password.expiry.days', '3', 'Temporary password expiry in days');
```

### Database Migration (onboarding-service): V8

```sql
-- V8__add_user_creation_status.sql

ALTER TABLE agent ADD COLUMN user_creation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE agent ADD COLUMN user_creation_error VARCHAR(500);
```

### JPA Entity Changes (auth-iam-service)

`UserEntity.java` — add columns:

| New Column | Type | Constraint |
|-----------|------|-----------|
| userType | @Enumerated(STRING) | NOT NULL |
| agentId | UUID | UNIQUE, nullable |
| phone | String | nullable |
| mustChangePassword | boolean | NOT NULL, default false |
| temporaryPasswordExpiresAt | LocalDateTime | nullable |

### JPA Entity Changes (onboarding-service)

`AgentEntity.java` — add columns:

| New Column | Type | Constraint |
|-----------|------|-----------|
| userCreationStatus | @Enumerated(STRING) | NOT NULL, default PENDING |
| userCreationError | String | nullable |

### Hexagonal Architecture: New Ports & Adapters

**auth-iam-service — New Outbound Ports:**

```java
// domain/port/out/NotificationPublisher.java
public interface NotificationPublisher {
    void publishUserCreated(UserCreatedEvent event);
    void publishUserCreationFailed(UserCreationFailedEvent event);
    void publishPasswordResetConfirmed(String userId, String email);
}

// domain/port/out/OtpStore.java
public interface OtpStore {
    void storeOtp(String username, String hashedOtp, int ttlSeconds);
    OtpData retrieveOtp(String username);
    void deleteOtp(String username);
    void incrementAttempts(String username);
}
```

**auth-iam-service — New Inbound Port:**

```java
// domain/port/in/CreateAgentUserUseCase.java
public interface CreateAgentUserUseCase {
    UserRecord createAgentUser(UUID agentId, String agentCode, String phone, String email, String businessName);
}
```

**onboarding-service — New Outbound Port:**

```java
// domain/port/out/AuthServiceClient.java
public interface AuthServiceClient {
    UserRecord createUserForAgent(CreateAgentUserRequest request);
}
```

---

## 3. API Endpoints

### New REST Endpoints (auth-iam-service)

| Method | Path | Description | Request DTO | Response DTO |
|--------|------|-------------|-------------|--------------|
| POST | `/auth/password/forgot` | Request OTP for password reset | `ForgotPasswordRequest{username}` | `ForgotPasswordResponse{message}` |
| POST | `/auth/password/reset` | Reset password with OTP | `ResetPasswordRequest{username, otp, newPassword}` | `ResetPasswordResponse{message}` |
| POST | `/auth/password/change` | Change password (authenticated) | `ChangePasswordRequest{currentPassword, newPassword}` | `ChangePasswordResponse{message}` |
| GET | `/auth/users/agent/{agentId}/status` | Get user creation status for agent | — | `AgentUserStatusResponse{agentId, status, userId, error}` |
| POST | `/auth/users/agent/{agentId}/create` | Manual trigger user creation for agent | — | `UserResponseDto` |

### Modified REST Endpoints (auth-iam-service)

| Method | Path | Change |
|--------|------|--------|
| POST | `/auth/users` | Accept `userType` field (INTERNAL/EXTERNAL), `phone` field. Generate temp password, set `mustChangePassword=true` |
| POST | `/auth/users/{id}/roles` | Validate role permissions match user's `userType` before assignment |

### Modified REST Endpoints (onboarding-service)

| Method | Path | Change |
|--------|------|--------|
| POST | `/backoffice/agents` | After creating agent, call auth-iam-service via Feign to create user |
| POST | `/internal/onboarding/agent/micro/evaluate` | After agent creation, trigger user creation |
| POST | `/internal/onboarding/agent/standard/start` | After agent creation, trigger user creation |

### API Gateway Routes

| Route ID | External Path | Target Service | Rewrite | Auth |
|----------|--------------|----------------|---------|------|
| `auth-password-forgot` | `/api/v1/auth/password/forgot` | auth-iam-service:8087 | `/auth/password/forgot` | Public |
| `auth-password-reset` | `/api/v1/auth/password/reset` | auth-iam-service:8087 | `/auth/password/reset` | Public |
| `auth-password-change` | `/api/v1/auth/password/change` | auth-iam-service:8087 | `/auth/password/change` | JwtAuthFilter |
| `backoffice-agent-user-status` | `/api/v1/backoffice/agents/{agentId}/user-status` | auth-iam-service:8087 | `/auth/users/agent/{agentId}/status` | JwtAuthFilter |
| `backoffice-agent-user-create` | `/api/v1/backoffice/agents/{agentId}/create-user` | auth-iam-service:8087 | `/auth/users/agent/{agentId}/create` | JwtAuthFilter |

All external-facing paths follow the `/api/v1/...` convention (consistent with the rest of the platform API).

### Gateway YAML

**IMPORTANT:** The specific `/auth/users/agent/*` routes MUST be placed BEFORE the existing wildcard `/auth/users/*` route in the gateway config to prevent route conflicts.

```yaml
# Backoffice agent user management (MUST be before /auth/users/* wildcard)
- id: backoffice-agent-user-status
  uri: http://auth-iam-service:8087
  predicates:
    - Path=/api/v1/backoffice/agents/*/user-status
  filters:
    - JwtAuthFilter
    - RewritePath=/api/v1/backoffice/agents/(?<agentId>.*)/user-status, /auth/users/agent/${agentId}/status

- id: backoffice-agent-user-create
  uri: http://auth-iam-service:8087
  predicates:
    - Path=/api/v1/backoffice/agents/*/create-user
  filters:
    - JwtAuthFilter
    - RewritePath=/api/v1/backoffice/agents/(?<agentId>.*)/create-user, /auth/users/agent/${agentId}/create

# Public password endpoints (/api/v1 prefix for external-facing consistency)
- id: auth-password-forgot
  uri: http://auth-iam-service:8087
  predicates:
    - Path=/api/v1/auth/password/forgot
  filters:
    - RewritePath=/api/v1/auth/password/forgot, /auth/password/forgot

- id: auth-password-reset
  uri: http://auth-iam-service:8087
  predicates:
    - Path=/api/v1/auth/password/reset
  filters:
    - RewritePath=/api/v1/auth/password/reset, /auth/password/reset

# Protected password change
- id: auth-password-change
  uri: http://auth-iam-service:8087
  predicates:
    - Path=/api/v1/auth/password/change
  filters:
    - JwtAuthFilter
    - RewritePath=/api/v1/auth/password/change, /auth/password/change
```

### Feign URL Property (onboarding-service application.yaml)

```yaml
auth:
  service:
    url: http://auth-iam-service:8087
```

---

## 4. Kafka Topics & Events

### Topic: `user.lifecycle`

Producer: auth-iam-service. Consumers: onboarding-service, external notification gateway.

```json
// USER_CREATED
{
  "eventId": "uuid",
  "eventType": "USER_CREATED",
  "timestamp": "ISO-8601",
  "data": {
    "userId": "uuid",
    "username": "string",
    "email": "string",
    "phone": "string|null",
    "fullName": "string",
    "userType": "EXTERNAL|INTERNAL",
    "agentId": "uuid|null",
    "notificationChannel": "SMS|EMAIL",
    "temporaryPassword": "string (plaintext, transient)"
  }
}

// USER_CREATION_FAILED
{
  "eventId": "uuid",
  "eventType": "USER_CREATION_FAILED",
  "timestamp": "ISO-8601",
  "data": {
    "agentId": "uuid",
    "agentCode": "string",
    "error": "string"
  }
}

// PASSWORD_RESET_CONFIRMED
{
  "eventId": "uuid",
  "eventType": "PASSWORD_RESET_CONFIRMED",
  "timestamp": "ISO-8601",
  "data": {
    "userId": "uuid",
    "username": "string",
    "email": "string",
    "phone": "string|null"
  }
}
```

### Topic: `agent.lifecycle`

Producer: onboarding-service. Consumer: auth-iam-service.

```json
// AGENT_CREATED (Kafka fallback when Feign fails)
{
  "eventId": "uuid",
  "eventType": "AGENT_CREATED",
  "timestamp": "ISO-8601",
  "data": {
    "agentId": "uuid",
    "agentCode": "string",
    "phoneNumber": "string|null",
    "email": "string|null",
    "businessName": "string"
  }
}
```

### Kafka Configuration

| Topic | Producer | Consumer(s) | Partitions | Replication |
|-------|----------|-------------|------------|-------------|
| `user.lifecycle` | auth-iam-service | onboarding-service, notification gateway | 3 | 1 |
| `agent.lifecycle` | onboarding-service | auth-iam-service | 3 | 1 |

### Spring Cloud Stream Configuration (auth-iam-service application.yaml)

```yaml
spring:
  cloud:
    stream:
      bindings:
        userCreated-out-0:
          destination: user.lifecycle
          content-type: application/json
        agentCreated-in-0:
          destination: agent.lifecycle
          group: auth-iam-service
          content-type: application/json
      kafka:
        binder:
          brokers: ${KAFKA_BROKERS:localhost:9092}
```

### Spring Cloud Stream Configuration (onboarding-service application.yaml)

```yaml
spring:
  cloud:
    stream:
      bindings:
        agentCreated-out-0:
          destination: agent.lifecycle
          content-type: application/json
        userCreated-in-0:
          destination: user.lifecycle
          group: onboarding-service
          content-type: application/json
      kafka:
        binder:
          brokers: ${KAFKA_BROKERS:localhost:9092}
```

---

## 5. Inter-Service Communication

### Feign Client (onboarding-service → auth-iam-service)

```java
@FeignClient(name = "auth-service", url = "${auth.service.url}",
             fallbackFactory = AuthServiceFallbackFactory.class)
public interface AuthUserFeignClient {
    @PostMapping("/internal/users/agent")
    UserResponseDto createAgentUser(@RequestBody CreateAgentUserRequest request);
}
```

### Resilience Configuration

| Inter-service Call | Timeout | Retry | Circuit Breaker |
|-------------------|---------|-------|-----------------|
| onboarding → auth-iam (user creation) | 5s | 0x (avoid duplicate creation) | 50% failure → open for 30s |

### Fallback Behavior

When circuit breaker opens:
1. `AuthServiceFallbackFactory` catches the exception
2. Publishes `AGENT_CREATED` to `agent.lifecycle` Kafka topic
3. Returns null to caller (onboarding-service handles status as PENDING)

---

## 6. Domain Services Design

### auth-iam-service: Domain Services

**1. UserManagementService (modified)**
- `createUser(UserRecord)` — extended to handle `userType`, generate temp password, set `mustChangePassword`, publish `USER_CREATED` event
- `createAgentUser(agentId, agentCode, phone, email)` — new method, creates EXTERNAL user with AGENT role auto-assignment
- `changePassword(userId, currentPassword, newPassword)` — clears `mustChangePassword`, nullifies `temporaryPasswordExpiresAt`
- `validateUserTypeForRole(userType, rolePermissions)` — validates permission prefix matching

**2. PasswordResetService (new)**
- `requestReset(username)` — generates 6-digit OTP, stores in Redis, publishes notification event
- `verifyReset(username, otp, newPassword)` — validates OTP (max 3 attempts), resets password, publishes `PASSWORD_RESET_CONFIRMED`
- OTP is hashed (SHA-256) before storing in Redis
- Attempt counter stored alongside OTP in Redis hash: `{otp_hash, attempts: 0, created_at}`

**3. AgentUserSyncService (new)**
- Consumes `AGENT_CREATED` Kafka events from `agent.lifecycle` topic
- Calls `createAgentUser()` (idempotent — checks `agentId` uniqueness)
- Publishes `USER_CREATED` or `USER_CREATION_FAILED` on `user.lifecycle`

**Bean Registration (DomainServiceConfig.java):**

```java
@Bean
public PasswordResetService passwordResetService(
    UserRepository userRepository,
    PasswordHasher passwordHasher,
    NotificationPublisher notificationPublisher,
    OtpStore otpStore) {
    return new PasswordResetService(userRepository, passwordHasher, notificationPublisher, otpStore);
}

@Bean
public AgentUserSyncService agentUserSyncService(
    CreateAgentUserUseCase createAgentUserUseCase) {
    return new AgentUserSyncService(createAgentUserUseCase);
}
```

### onboarding-service: Domain Services

**AgentService (modified)**
- `createAgent()` — after persisting agent entity, calls auth-iam-service via Feign
- On Feign success: sets `userCreationStatus = CREATED`
- On Feign failure: publishes `AGENT_CREATED` to Kafka, sets `userCreationStatus = PENDING`

**AgentUserStatusService (new)**
- Consumes `USER_CREATED` and `USER_CREATION_FAILED` events
- Updates `userCreationStatus` and `userCreationError` on agent entity

### Domain Service Flow: Agent Creation

```
AgentService.createAgent()
    │
    ├─► Persist agent entity (userCreationStatus = PENDING)
    │
    ├─► Try: Feign → auth-iam-service POST /internal/users/agent
    │       │
    │       ├─► Success: userCreationStatus = CREATED
    │       │
    │       └─► Failure (circuit open):
    │               Publish AGENT_CREATED to Kafka
    │               (status stays PENDING, updated by Kafka consumer later)
    │
    └─► Return agent
```

### Domain Service Flow: Password Reset

```
PasswordResetService.requestReset(username)
    │
    ├─► Find user by username
    │       └─► Not found: return generic success (no user enumeration)
    │
    ├─► Generate 6-digit OTP
    ├─► Hash OTP (SHA-256)
    ├─► Store via OtpStore port: otp:reset:{username} → {otp_hash, attempts: 0, created_at}
    │       TTL = 10 minutes
    │       Infrastructure adapter uses Redis (OtpStoreRedisAdapter)
    │
    ├─► Publish OTP_REQUESTED event to user.lifecycle
    │       Notification gateway sends OTP via phone (preferred) or email
    │
    └─► Return generic success message

PasswordResetService.verifyReset(username, otp, newPassword)
    │
    ├─► Fetch via OtpStore port: otp:reset:{username}
    │       └─► Not found/expired: ERR_AUTH_OTP_EXPIRED
    │
    ├─► Check attempts >= 3
    │       └─► Exceeded: delete via OtpStore, ERR_AUTH_OTP_MAX_ATTEMPTS
    │
    ├─► Hash provided OTP, compare with stored hash
    │       └─► Mismatch: increment attempts via OtpStore, ERR_AUTH_OTP_INVALID
    │
    ├─► Hash new password, update user record
    │       mustChangePassword = false (user chose their own password)
    │
    ├─► Delete OTP via OtpStore
    ├─► Publish PASSWORD_RESET_CONFIRMED to user.lifecycle
    │
    └─► Return success
```

---

## 7. Error Codes

| Code | Category | Message | Action Code |
|------|----------|---------|-------------|
| `ERR_AUTH_TEMP_PASSWORD_EXPIRED` | AUTH | Temporary password has expired | RETRY |
| `ERR_AUTH_MUST_CHANGE_PASSWORD` | AUTH | Password change required before proceeding | RETRY |
| `ERR_AUTH_OTP_EXPIRED` | AUTH | OTP has expired or not found | RETRY |
| `ERR_AUTH_OTP_INVALID` | AUTH | Invalid OTP provided | RETRY |
| `ERR_AUTH_OTP_MAX_ATTEMPTS` | AUTH | Maximum OTP attempts exceeded. Request a new OTP | RETRY |
| `ERR_AUTH_USER_CREATION_FAILED` | AUTH | Failed to create user account for agent | RETRY |
| `ERR_AUTHZ_USER_TYPE_MISMATCH` | AUTHZ | Role permissions do not match user type | REVIEW |
| `ERR_AUTHZ_INTERNAL_ONLY` | AUTHZ | This operation is restricted to internal users | REVIEW |
| `ERR_AUTHZ_EXTERNAL_ONLY` | AUTHZ | This operation is restricted to external users | REVIEW |

---

## 8. Testing Strategy

### Unit Tests (domain layer)

- `UserManagementService` — user type validation, temp password generation, AGENT role assignment
- `PasswordResetService` — OTP generation, hashing, Redis storage, verification, attempt limits
- `AuthorizationService` — user type permission prefix matching

### Integration Tests (infrastructure layer)

- `UserJpaRepository` — `user_type` column, `agent_id` unique constraint, CHECK constraint
- Kafka producer/consumer — `USER_CREATED`, `USER_CREATION_FAILED`, `AGENT_CREATED` event round-trip
- Redis OTP storage — TTL, attempt counter, hash verification

### ArchUnit Tests (architecture enforcement)

- Verify new domain services have no framework imports
- Verify `UserType` enum in `domain/model/` has no annotations
- Verify `PasswordResetService` does not import Spring `@Service`

### End-to-End Scenarios (from BDD)

- Backoffice creates agent → user auto-created → temp password sent
- Agent logs in with temp password → must change password
- User forgets password → OTP flow → reset → confirmation

### Testing Matrix

| Layer | Test Type | Key Scenarios |
|-------|-----------|---------------|
| Domain | Unit | User type validation, OTP flow, permission matching |
| Application | Unit | Use case orchestration, Feign fallback |
| Infrastructure | Integration | JPA constraints, Kafka events, Redis OTP |
| Architecture | ArchUnit | Hexagonal compliance, no domain framework imports |
| E2E | Integration | Agent creation → user creation → notification delivery |
