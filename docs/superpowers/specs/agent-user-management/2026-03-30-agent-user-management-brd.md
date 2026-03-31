# Business Requirements Document: Agent & User Management

**Version:** 1.0
**Date:** 2026-03-30
**Status:** Draft
**Module:** Auth & IAM Service (`com.agentbanking.auth`), Onboarding Service (`com.agentbanking.onboarding`)

---

## 1. Business Goals & Success Criteria

### Business Goals

1. Enable automatic user account creation for agents during onboarding
2. Support two user types: internal (bank staff) and external (agents)
3. Deliver temporary passwords via configurable channels (email/SMS)
4. Provide self-service password reset for users who forget their password
5. Enforce temporary password policies (expiry + forced change on first login)
6. Restrict permissions by user type — internal gets backoffice, external gets agent/merchant transactions

### Success Criteria

- Agent creation automatically provisions a user account with a temporary password delivered to the agent
- Staff users can be manually created by admin with configurable notification channel
- Users can initiate forgot-password and receive OTP to verify identity before resetting
- Temporary passwords expire after X days (configurable via system parameter)
- First login with temporary password forces password change before any other action
- Internal users cannot access agent transaction APIs and vice versa

---

## 2. User Roles & Stories

### Roles

| Role | Responsibilities |
|------|-----------------|
| **Bank Admin** | Creates internal staff users, manages user accounts, configures system parameters |
| **Internal User (Staff)** | Uses backoffice functions — cannot access agent transaction APIs |
| **External User (Agent)** | Uses agent/merchant transaction functions — cannot access backoffice |

### User Stories

#### Agent User Account (Auto-Creation)

- **US-UM-01** As a system, when an agent is created in the onboarding-service, I automatically create a corresponding user account in auth-iam-service with type EXTERNAL
- **US-UM-02** As a system, when auto-creating an agent user, I generate a temporary password and send it to the agent via their registered phone number (SMS)
- **US-UM-03** As a system, each agent has exactly one user account linked by `agentId`. If a user account already exists for the agent, I reject duplicate creation
- **US-UM-04** As a Bank Admin, I can check the user creation status for an agent (pending/created/failed) and manually trigger user creation if it failed or is still pending

#### Staff User Account (Manual Creation)

- **US-UM-05** As a Bank Admin, I can create an internal staff user account by providing username, email, phone, and full name
- **US-UM-06** As a Bank Admin, when I create a staff user, the system sends a temporary password to the configured notification channel (email or SMS)
- **US-UM-07** As a Bank Admin, I cannot create more than one user account for the same staff member

#### Temporary Password Policy

- **US-UM-08** As a user, when I first login with a temporary password, I must change my password before performing any other action
- **US-UM-09** As a system, I reject login if the temporary password has expired (past configurable X days)
- **US-UM-10** As a system, I store `mustChangePassword` flag and `temporaryPasswordExpiresAt` on the user record

#### Self-Service Password Reset

- **US-UM-11** As a user, I can request a password reset by providing my username
- **US-UM-12** As a system, when a reset is requested, I send a 6-digit OTP to the user's registered email or phone
- **US-UM-13** As a user, I can verify the OTP and set a new password in a single step
- **US-UM-14** As a system, I expire OTPs after 10 minutes and limit OTP attempts to 3

#### User Type Permission Restriction

- **US-UM-15** As a system, I enforce that INTERNAL users can only be assigned roles/permissions for backoffice functions
- **US-UM-16** As a system, I enforce that EXTERNAL users can only be assigned roles/permissions for agent/merchant transaction functions

#### Password Reset Confirmation

- **US-UM-17** As a user, after successfully resetting my password, I receive a confirmation notification via my registered email or phone

### User Story to Functional Requirement Mapping

| User Story | Functional Requirements |
|------------|------------------------|
| US-UM-01 | FR-2.1, FR-2.2, FR-2.3 |
| US-UM-02 | FR-2.2, FR-2.3 |
| US-UM-03 | FR-1.3, FR-2.1, FR-2.7 |
| US-UM-04 | FR-7.1, FR-7.2 |
| US-UM-05 | FR-3.1, FR-3.2 |
| US-UM-06 | FR-3.2 |
| US-UM-07 | FR-3.3 |
| US-UM-08 | FR-4.2, FR-4.4 |
| US-UM-09 | FR-4.1, FR-4.3 |
| US-UM-10 | FR-4.1, FR-4.4 |
| US-UM-11 | FR-5.1 |
| US-UM-12 | FR-5.2 |
| US-UM-13 | FR-5.3 |
| US-UM-14 | FR-5.4 |
| US-UM-15 | FR-6.1, FR-6.2 |
| US-UM-16 | FR-6.1, FR-6.3 |
| US-UM-17 | FR-5.6 |

---

## 3. Functional Requirements

### FR-1: User Type Model

- **FR-1.1** Users table shall have a `user_type` column with values: `INTERNAL`, `EXTERNAL`
- **FR-1.2** `user_type` is immutable after creation — cannot be changed
- **FR-1.3** EXTERNAL users shall have a nullable `agent_id` column (FK to agent, unique constraint)
- **FR-1.4** INTERNAL users shall have `agent_id = NULL`
- **FR-1.5** Username uniqueness is global across both user types. Agent usernames use the pattern `agentCode` (prefixed with `AGT-` by onboarding-service), which is guaranteed not to collide with internal staff usernames

### FR-2: Agent User Auto-Creation

- **FR-2.1** When onboarding-service creates an agent, it calls auth-iam-service via Feign to create a user account (username = `agentCode`, userType = `EXTERNAL`, agentId linked)
- **FR-2.2** Auth-iam-service generates a random temporary password (configurable complexity), hashes it, and sets `temporary_password_expires_at` to now + X days
- **FR-2.3** Auth-iam-service publishes a `USER_CREATED` Kafka event after successful creation. Topic: `user.lifecycle`. Payload:
  ```json
  {
    "eventId": "uuid",
    "eventType": "USER_CREATED",
    "timestamp": "ISO-8601",
    "data": {
      "userId": "uuid",
      "username": "string",
      "email": "string",
      "phone": "string",
      "fullName": "string",
      "userType": "EXTERNAL|INTERNAL",
      "agentId": "uuid|null",
      "notificationChannel": "SMS|EMAIL",
      "temporaryPassword": "plaintext (transient, notification service consumes and discards)"
    }
  }
  ```
- **FR-2.4** If Feign call fails, onboarding-service publishes `AGENT_CREATED` Kafka event as fallback. Topic: `agent.lifecycle`. Payload:
  ```json
  {
    "eventId": "uuid",
    "eventType": "AGENT_CREATED",
    "timestamp": "ISO-8601",
    "data": {
      "agentId": "uuid",
      "agentCode": "string",
      "phoneNumber": "string",
      "email": "string",
      "businessName": "string"
    }
  }
  ```
  Auth-iam-service consumes this event and creates the user asynchronously, then publishes `USER_CREATED` on `user.lifecycle`
- **FR-2.5** A `user_creation_status` field on the agent tracks: `PENDING`, `CREATED`, `FAILED` (stored in onboarding-service). Status is updated by consuming `USER_CREATED` or `USER_CREATION_FAILED` events from `user.lifecycle` topic
- **FR-2.6** Auto-created agent users are automatically assigned the `AGENT` role during user creation in auth-iam-service. This applies to both the Feign (sync) and Kafka (async) paths, as well as the manual retry path (FR-7.2)
- **FR-2.7** Duplicate detection: auth-iam-service checks uniqueness by `agent_id` (unique constraint) and `username`. If a user already exists for the given `agent_id`, the Kafka consumer silently skips (idempotent). If the Feign caller receives a conflict, it updates `user_creation_status` to `CREATED`

### FR-3: Staff User Manual Creation

- **FR-3.1** Admin creates staff users via `POST /auth/users` with `userType = INTERNAL`
- **FR-3.2** System generates temporary password, sends `USER_CREATED` Kafka event on `user.lifecycle` topic for notification delivery
- **FR-3.3** System enforces uniqueness: one user per staff member (by email or username, globally)

### FR-4: Temporary Password Policy

- **FR-4.1** Temporary password expiry duration is configurable via system parameter `temp.password.expiry.days` (default: 3 days)
- **FR-4.2** On first login with temporary password, auth response includes `mustChangePassword: true` in JWT claims
- **FR-4.3** If `temporary_password_expires_at < now`, login is rejected with error `ERR_AUTH_TEMP_PASSWORD_EXPIRED`
- **FR-4.4** After successful password change, `mustChangePassword` is cleared and `temporary_password_expires_at` is nullified

### FR-5: Self-Service Password Reset (Forgot Password)

- **FR-5.1** `POST /auth/password/forgot` accepts `{ username }`, sends 6-digit OTP to user's registered email or phone (phone preferred if available, else email). Email is a required field on the User entity, so email is always available as fallback
- **FR-5.2** OTP stored in Redis with key `otp:reset:{username}`, TTL = 10 minutes
- **FR-5.3** `POST /auth/password/reset` accepts `{ username, otp, newPassword }`, validates OTP, resets password
- **FR-5.4** OTP has max 3 verification attempts; after that, OTP is invalidated and user must request a new one
- **FR-5.5** On successful reset, `mustChangePassword` is NOT set (user chose their own password)
- **FR-5.6** Notification service sends confirmation message after successful password reset

### FR-6: User Type Permission Enforcement

- **FR-6.1** When assigning a role to a user, system validates that all permissions in that role match the user's type
- **FR-6.2** INTERNAL users can only have roles containing backoffice permissions (`user:*`, `role:*`, `audit:read`, `ledger:*`)
- **FR-6.3** EXTERNAL users can only have roles containing agent/merchant permissions (`transaction:*`, `kyc:verify`)
- **FR-6.4** Violation returns error `ERR_AUTHZ_USER_TYPE_MISMATCH`

### FR-7: Agent User Status Check (Admin)

- **FR-7.1** `GET /auth/users/agent/{agentId}/status` returns user creation status for an agent
- **FR-7.2** `POST /auth/users/agent/{agentId}/create` triggers synchronous user creation for an agent (retry/manual trigger). Auto-assigns AGENT role (same as FR-2.6)

### FR-8: Integration — Status Sync

- **FR-8.1** Onboarding-service consumes `USER_CREATED` and `USER_CREATION_FAILED` events from `user.lifecycle` Kafka topic to update `user_creation_status` on the agent record
- **FR-8.2** On `USER_CREATED`: set status to `CREATED`
- **FR-8.3** On `USER_CREATION_FAILED`: set status to `FAILED`, store error message in `user_creation_error`

---

## 4. Entity Definitions

BDD scenarios reference these entities by name and field. All entities use UUID primary keys unless noted otherwise.

### User (Extended)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| userId | UUID | Yes | Unique user identifier |
| username | String(50) | Yes | Login username (unique, global across user types) |
| email | String(100) | Yes | Email address (unique) |
| phone | String(20) | No | Phone number for SMS notifications |
| passwordHash | String(255) | Yes | BCrypt hashed password |
| fullName | String(200) | Yes | Display name |
| userType | Enum | Yes | INTERNAL or EXTERNAL |
| agentId | UUID | No | FK to agent (unique, only for EXTERNAL) |
| status | Enum | Yes | ACTIVE, INACTIVE, LOCKED, DELETED |
| mustChangePassword | Boolean | Yes | True if user must change password on next login |
| temporaryPasswordExpiresAt | LocalDateTime | No | When the temporary password expires |
| failedLoginAttempts | Integer | Yes | Counter for brute force protection |
| lockedUntil | LocalDateTime | No | Account lockout expiry |
| createdAt | LocalDateTime | Yes | Creation timestamp |
| updatedAt | LocalDateTime | Yes | Last update timestamp |
| lastLoginAt | LocalDateTime | No | Last successful login |

### Agent (Extended — in onboarding-service)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| agentId | UUID | Yes | Unique agent identifier |
| agentCode | String(20) | Yes | Human-readable agent code (e.g., "AGT-00123") |
| userCreationStatus | Enum | Yes | PENDING, CREATED, FAILED |
| userCreationError | String(500) | No | Error details if user creation failed |

### OTP (Redis)

| Field | Type | Description |
|-------|------|-------------|
| key | String | `otp:reset:{username}` |
| value | String | 6-digit OTP (hashed) |
| ttl | Integer | 10 minutes |
| attempts | Integer | Max 3 |

---

## 5. Non-Functional Requirements

### NFR-1: Performance

- Agent user creation via Feign: < 500ms end-to-end
- Password reset OTP delivery: < 10 seconds
- Login with `mustChangePassword` check: < 100ms (no additional DB queries beyond normal auth)

### NFR-2: Security

- Temporary passwords must meet minimum complexity (8+ chars, mixed case + digit)
- OTPs are 6 digits, stored hashed in Redis (not plaintext)
- Failed OTP attempts tracked, rate-limited per username
- No PII (passwords, OTPs, MyKad) in logs

### NFR-3: Reliability

- Feign call to auth-iam-service includes Resilience4j circuit breaker (50% failure threshold, 30s wait)
- Kafka fallback ensures eventual consistency if Feign fails
- Kafka consumer for `agent.lifecycle` and `user.lifecycle` topics is idempotent (dedup by eventId)
- Notification delivery failures are retried 3 times with exponential backoff

---

## 6. Constraints & Assumptions

### Constraints

- Must follow existing hexagonal architecture in auth-iam-service
- Notifications follow platform pattern: Kafka events → External Notification Gateway (REST API)
- Kafka already configured in the platform — auth-iam-service needs Kafka producer added
- Redis already configured in auth-iam-service — used for OTP storage

### Assumptions

- Existing seeded roles (IT_ADMIN, BANK_OPERATOR, AGENT, AUDITOR, TELLER) remain unchanged
- AGENT role is pre-assigned to auto-created agent users (FR-2.6)
- External Notification Gateway (existing platform component) handles SMS/email delivery
- System parameters table (`system_parameters`) requires a new Flyway migration in auth-iam-service to store `temp.password.expiry.days` and future config values
- Agent codes use `AGT-` prefix (set by onboarding-service), preventing username collisions with internal staff

---

## 7. Permission Scope Definition

| User Type | Allowed Permission Scope | Example Permissions |
|-----------|------------------------|-------------------|
| **INTERNAL (Backoffice)** | User management, role management, audit viewing, ledger read, system configuration | `user:read`, `user:write`, `user:delete`, `role:read`, `role:write`, `audit:read`, `ledger:read`, `ledger:write` |
| **EXTERNAL (Agent/Merchant)** | Transaction creation, transaction viewing, KYC verification | `transaction:create`, `transaction:read`, `kyc:verify` |

Permission keys are defined in the seed data (`V2__seed_admin_user.sql`). The system validates role assignments against the user's type using prefix-based matching (e.g., `user:*` matches `user:read`, `user:write`, etc.). New permissions automatically match if they follow the naming convention.

---

## 8. Traceability Matrix

### User Story → Functional Requirement Coverage

| User Story | FR(s) |
|------------|-------|
| US-UM-01 | FR-2.1, FR-2.2, FR-2.3 |
| US-UM-02 | FR-2.2, FR-2.3 |
| US-UM-03 | FR-1.3, FR-2.1, FR-2.7 |
| US-UM-04 | FR-7.1, FR-7.2 |
| US-UM-05 | FR-3.1, FR-3.2 |
| US-UM-06 | FR-3.2 |
| US-UM-07 | FR-3.3 |
| US-UM-08 | FR-4.2, FR-4.4 |
| US-UM-09 | FR-4.1, FR-4.3 |
| US-UM-10 | FR-4.1, FR-4.4 |
| US-UM-11 | FR-5.1 |
| US-UM-12 | FR-5.2 |
| US-UM-13 | FR-5.3 |
| US-UM-14 | FR-5.4 |
| US-UM-15 | FR-6.1, FR-6.2 |
| US-UM-16 | FR-6.1, FR-6.3 |
| US-UM-17 | FR-5.6 |
