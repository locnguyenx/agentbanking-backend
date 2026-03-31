# BDD Specification: Agent & User Management

**Version:** 1.0
**Date:** 2026-03-30
**Status:** Draft
**Module:** Auth & IAM Service (`com.agentbanking.auth`), Onboarding Service (`com.agentbanking.onboarding`), Notification Service (`com.agentbanking.notification`)
**BRD Reference:** `docs/superpowers/specs/agent-user-management/2026-03-30-agent-user-management-brd.md`

Each BDD scenario is tagged with exactly one `@US` (User Story) and one or more `@FR` (Functional Requirements) for atomic traceability.

---

## 1. Agent User Auto-Creation

```gherkin
Feature: Agent User Auto-Creation

  @US-UM-01 @FR-2.1
  Scenario: Feign call creates user account when agent is created
    Given onboarding-service is creating an agent with code "AGT-00123"
    And auth-iam-service is available
    When onboarding-service calls auth-iam-service to create a user for the agent
    Then auth-iam-service creates a user with username "AGT-00123", userType "EXTERNAL", agentId linked
    And the user is assigned the "AGENT" role
    And a temporary password is generated and hashed
    And onboarding-service sets userCreationStatus to "CREATED"

  @US-UM-02 @FR-2.2 @FR-2.3
  Scenario: Temporary password is generated and notification event published
    Given auth-iam-service is creating an external user for agent "AGT-00123"
    When the user account is created successfully
    Then a temporary password is generated meeting complexity requirements (8+ chars, mixed case + digit)
    And temporary_password_expires_at is set to now + configured days
    And a USER_CREATED event is published to "user.lifecycle" Kafka topic
    And the event contains userId, username, email, phone, userType, agentId, and temporaryPassword

  @US-UM-03 @FR-1.3 @FR-2.7
  Scenario: Duplicate agent user creation is rejected (sync path)
    Given a user account already exists for agent "AGT-00123"
    When onboarding-service calls auth-iam-service to create a user for the same agent
    Then the creation is rejected with error "User already exists for this agent"
    And onboarding-service sets userCreationStatus to "CREATED"

  @US-UM-03 @FR-2.4 @FR-2.7
  Scenario: Kafka fallback creates user when Feign fails
    Given onboarding-service is creating an agent with code "AGT-00456"
    And auth-iam-service is unavailable (Feign call fails)
    When onboarding-service publishes an AGENT_CREATED event to "agent.lifecycle" Kafka topic
    And auth-iam-service consumes the event
    Then auth-iam-service creates a user with username "AGT-00456", userType "EXTERNAL", agentId linked
    And the user is assigned the "AGENT" role
    And a USER_CREATED event is published to "user.lifecycle"

  @US-UM-03 @FR-2.4 @FR-2.7
  Scenario: Kafka consumer is idempotent for duplicate agent
    Given a user account already exists for agent "AGT-00456"
    And auth-iam-service receives an AGENT_CREATED event for agent "AGT-00456"
    When auth-iam-service processes the event
    Then the event is silently skipped (no error, no duplicate created)

  @US-UM-02 @FR-8.1 @FR-8.2
  Scenario: Onboarding-service updates status on USER_CREATED event
    Given onboarding-service has an agent "AGT-00123" with userCreationStatus "PENDING"
    When onboarding-service receives a USER_CREATED event for agent "AGT-00123"
    Then userCreationStatus is updated to "CREATED"

  @US-UM-04 @FR-8.3
  Scenario: Onboarding-service updates status on USER_CREATION_FAILED event
    Given onboarding-service has an agent "AGT-00789" with userCreationStatus "PENDING"
    When onboarding-service receives a USER_CREATION_FAILED event for agent "AGT-00789"
    Then userCreationStatus is updated to "FAILED"
    And userCreationError stores the failure message
```

---

## 2. Staff User Manual Creation

```gherkin
Feature: Staff User Manual Creation

  @US-UM-05 @FR-3.1
  Scenario: Admin creates an internal staff user
    Given a Bank Admin "admin" is authenticated
    When admin creates a user with username "jsmith", email "jsmith@bank.com", fullName "John Smith", userType "INTERNAL"
    Then the user is created with status "ACTIVE", userType "INTERNAL", agentId NULL
    And a temporary password is generated and hashed
    And mustChangePassword is set to true
    And a USER_CREATED event is published to "user.lifecycle"

  @US-UM-06 @FR-3.2
  Scenario: Temporary password sent via configured notification channel
    Given admin creates an internal user with phone "+60123456789" and notificationChannel "SMS"
    When the user account is created
    Then a USER_CREATED event is published with notificationChannel "SMS"
    And the event includes the plaintext temporary password

  @US-UM-07 @FR-3.3
  Scenario: Duplicate username is rejected
    Given a user with username "jsmith" already exists
    When admin attempts to create a user with username "jsmith"
    Then the creation is rejected with error "Username already exists"

  @US-UM-07 @FR-3.3
  Scenario: Duplicate email is rejected
    Given a user with email "jsmith@bank.com" already exists
    When admin attempts to create a user with email "jsmith@bank.com"
    Then the creation is rejected with error "Email already exists"

  @US-UM-07 @FR-1.5
  Scenario: Agent username does not collide with staff username
    Given an agent user with username "AGT-00123" exists
    When admin creates a staff user with username "jsmith"
    Then the creation succeeds (no collision with AGT- prefixed username)
```

---

## 3. Temporary Password Policy

```gherkin
Feature: Temporary Password Policy

  @US-UM-08 @FR-4.2
  Scenario: First login with temporary password sets mustChangePassword flag
    Given a user "jsmith" has a temporary password and mustChangePassword is true
    When "jsmith" logs in with the temporary password
    Then authentication succeeds
    And the JWT token contains claim "mustChangePassword" set to true

  @US-UM-09 @FR-4.3
  Scenario: Expired temporary password is rejected
    Given a user "jsmith" has temporary_password_expires_at set to 2 days ago
    When "jsmith" attempts to login with the temporary password
    Then login is rejected with error "ERR_AUTH_TEMP_PASSWORD_EXPIRED"
    And action_code is "RETRY"

  @US-UM-10 @FR-4.4
  Scenario: Password change clears temporary password flags
    Given a user "jsmith" has mustChangePassword true and temporary_password_expires_at set
    When "jsmith" changes their password successfully
    Then mustChangePassword is set to false
    And temporary_password_expires_at is set to NULL

  @US-UM-10 @FR-4.1
  Scenario: Temporary password expiry uses configured system parameter
    Given system parameter "temp.password.expiry.days" is set to 5
    When a new user account is created
    Then temporary_password_expires_at is set to now + 5 days
```

---

## 4. Self-Service Password Reset (Forgot Password)

```gherkin
Feature: Self-Service Password Reset (Forgot Password)

  @US-UM-11 @FR-5.1 @FR-5.2
  Scenario: Forgot password sends OTP to user's phone
    Given a user "jsmith" exists with phone "+60123456789" and email "jsmith@bank.com"
    When "jsmith" requests a password reset
    Then a 6-digit OTP is generated and stored in Redis with key "otp:reset:jsmith"
    And OTP TTL is set to 10 minutes
    And OTP is sent via phone (phone preferred over email)

  @US-UM-11 @FR-5.1
  Scenario: Forgot password falls back to email when no phone
    Given a user "alice" exists with email "alice@bank.com" and no phone
    When "alice" requests a password reset
    Then OTP is sent via email

  @US-UM-13 @FR-5.3
  Scenario: Valid OTP allows password reset
    Given a valid OTP exists in Redis for user "jsmith"
    When "jsmith" submits the correct OTP and new password "NewSecure123!"
    Then the password is reset successfully
    And the OTP is invalidated (removed from Redis)
    And mustChangePassword is NOT set

  @US-UM-13 @FR-5.3
  Scenario: Invalid OTP is rejected
    Given a valid OTP exists in Redis for user "jsmith"
    When "jsmith" submits an incorrect OTP and new password
    Then the reset is rejected with error "Invalid OTP"
    And the OTP attempt counter is incremented

  @US-UM-14 @FR-5.4
  Scenario: OTP expires after 3 failed attempts
    Given a valid OTP exists in Redis for user "jsmith" with 2 attempts used
    When "jsmith" submits an incorrect OTP (attempt 3)
    Then the reset is rejected
    And the OTP is invalidated (removed from Redis)
    And user must request a new OTP

  @US-UM-14 @FR-5.4
  Scenario: Expired OTP is rejected
    Given an OTP for user "jsmith" has expired (TTL elapsed)
    When "jsmith" submits the OTP
    Then the reset is rejected with error "OTP expired"

  @US-UM-17 @FR-5.6
  Scenario: Confirmation notification sent after successful reset
    Given user "jsmith" has successfully reset their password
    When the reset completes
    Then a PASSWORD_RESET_CONFIRMED event is published to "user.lifecycle"
    And notification service sends a confirmation message to "jsmith"'s email or phone

  @US-UM-11 @FR-5.1
  Scenario: Forgot password for non-existent user returns generic response
    When a request is made to reset password for username "nonexistent"
    Then the response is identical to a successful OTP request (no user enumeration)
```

---

## 5. User Type Permission Enforcement

```gherkin
Feature: User Type Permission Enforcement

  @US-UM-15 @FR-6.1 @FR-6.2
  Scenario: INTERNAL user can be assigned backoffice role
    Given a user "jsmith" exists with userType "INTERNAL"
    And role "BANK_OPERATOR" has permissions "user:read", "user:write", "audit:read"
    When admin assigns role "BANK_OPERATOR" to "jsmith"
    Then the role assignment succeeds

  @US-UM-15 @FR-6.1 @FR-6.4
  Scenario: INTERNAL user cannot be assigned agent transaction role
    Given a user "jsmith" exists with userType "INTERNAL"
    And role "AGENT" has permissions "transaction:create", "transaction:read", "kyc:verify"
    When admin attempts to assign role "AGENT" to "jsmith"
    Then the assignment is rejected with error "ERR_AUTHZ_USER_TYPE_MISMATCH"

  @US-UM-16 @FR-6.1 @FR-6.3
  Scenario: EXTERNAL user can be assigned agent transaction role
    Given a user "AGT-00123" exists with userType "EXTERNAL"
    And role "AGENT" has permissions "transaction:create", "transaction:read", "kyc:verify"
    When admin assigns role "AGENT" to "AGT-00123"
    Then the role assignment succeeds

  @US-UM-16 @FR-6.1 @FR-6.4
  Scenario: EXTERNAL user cannot be assigned backoffice role
    Given a user "AGT-00123" exists with userType "EXTERNAL"
    And role "IT_ADMIN" has permissions "user:read", "user:write", "user:delete", "role:write"
    When admin attempts to assign role "IT_ADMIN" to "AGT-00123"
    Then the assignment is rejected with error "ERR_AUTHZ_USER_TYPE_MISMATCH"

  @US-UM-16 @FR-6.1
  Scenario: Mixed permission role is rejected for both user types
    Given a role "MIXED_ROLE" has permissions "user:read" and "transaction:create"
    When admin attempts to assign "MIXED_ROLE" to any user type
    Then the assignment is rejected with error "ERR_AUTHZ_USER_TYPE_MISMATCH"
```

---

## 6. Agent User Status Check (Admin)

```gherkin
Feature: Agent User Status Check (Admin)

  @US-UM-04 @FR-7.1
  Scenario: Admin checks user creation status for an agent
    Given agent "AGT-00123" exists with userCreationStatus "CREATED"
    When admin queries user creation status for agent "AGT-00123"
    Then the response contains status "CREATED" and associated userId

  @US-UM-04 @FR-7.1
  Scenario: Admin checks status for agent with failed creation
    Given agent "AGT-00789" exists with userCreationStatus "FAILED" and userCreationError "Auth service unavailable"
    When admin queries user creation status for agent "AGT-00789"
    Then the response contains status "FAILED" and error message

  @US-UM-04 @FR-7.2
  Scenario: Admin manually triggers user creation for pending agent
    Given agent "AGT-00456" exists with userCreationStatus "PENDING"
    And auth-iam-service is available
    When admin triggers user creation for agent "AGT-00456"
    Then auth-iam-service creates a user with username "AGT-00456", userType "EXTERNAL"
    And the user is assigned the "AGENT" role
    And agent userCreationStatus is updated to "CREATED"

  @US-UM-04 @FR-7.2
  Scenario: Admin manually triggers user creation for failed agent
    Given agent "AGT-00789" exists with userCreationStatus "FAILED"
    And auth-iam-service is available
    When admin triggers user creation for agent "AGT-00789"
    Then auth-iam-service creates a user with username "AGT-00789", userType "EXTERNAL"
    And the user is assigned the "AGENT" role
    And agent userCreationStatus is updated to "CREATED"

  @US-UM-04 @FR-7.2
  Scenario: Admin cannot trigger creation for already created agent
    Given agent "AGT-00123" exists with userCreationStatus "CREATED"
    When admin triggers user creation for agent "AGT-00123"
    Then the request is rejected with error "User already exists for this agent"
```

---

## 7. Traceability Matrix

### User Story → BDD Scenario Coverage

| User Story | FR(s) | Happy Path Scenario | Edge Case Scenario(s) |
|------------|-------|---------------------|----------------------|
| US-UM-01 | FR-2.1 | S1.1 (Feign creates user) | — |
| US-UM-02 | FR-2.2, FR-2.3 | S1.2 (temp password + event) | — |
| US-UM-03 | FR-1.3, FR-2.1, FR-2.7 | S1.4 (Kafka fallback) | S1.3 (duplicate rejected), S1.5 (idempotent skip) |
| US-UM-04 | FR-7.1, FR-7.2 | S6.3 (manual trigger) | S6.1 (status check), S6.2 (failed status), S6.4 (retry failed), S6.5 (already created) |
| US-UM-05 | FR-3.1, FR-3.2 | S2.1 (admin creates staff) | — |
| US-UM-06 | FR-3.2 | S2.2 (notification channel) | — |
| US-UM-07 | FR-3.3, FR-1.5 | — | S2.3 (duplicate username), S2.4 (duplicate email), S2.5 (no collision) |
| US-UM-08 | FR-4.2 | S3.1 (first login flag) | — |
| US-UM-09 | FR-4.1, FR-4.3 | — | S3.2 (expired temp password) |
| US-UM-10 | FR-4.1, FR-4.4 | S3.3 (password change clears), S3.4 (configurable expiry) | — |
| US-UM-11 | FR-5.1, FR-5.2 | S4.1 (OTP via phone) | S4.2 (fallback to email), S4.8 (non-existent user) |
| US-UM-13 | FR-5.3 | S4.3 (valid OTP reset) | S4.4 (invalid OTP) |
| US-UM-14 | FR-5.4 | — | S4.5 (3 attempts exhausted), S4.6 (OTP expired) |
| US-UM-15 | FR-6.1, FR-6.2 | S5.1 (internal gets backoffice role) | S5.2 (internal denied agent role) |
| US-UM-16 | FR-6.1, FR-6.3 | S5.3 (external gets agent role) | S5.4 (external denied backoffice), S5.5 (mixed role rejected) |
| US-UM-17 | FR-5.6 | S4.7 (confirmation notification) | — |

### Requirement Coverage Summary

| Requirement | Covered By Scenario(s) |
|-------------|----------------------|
| FR-1.3 | S1.3 |
| FR-1.5 | S2.5 |
| FR-2.1 | S1.1, S1.4 |
| FR-2.2 | S1.2 |
| FR-2.3 | S1.2 |
| FR-2.4 | S1.4 |
| FR-2.7 | S1.3, S1.5 |
| FR-3.1 | S2.1 |
| FR-3.2 | S2.1, S2.2 |
| FR-3.3 | S2.3, S2.4 |
| FR-4.1 | S3.4 |
| FR-4.2 | S3.1 |
| FR-4.3 | S3.2 |
| FR-4.4 | S3.3 |
| FR-5.1 | S4.1, S4.2, S4.8 |
| FR-5.2 | S4.1 |
| FR-5.3 | S4.3, S4.4 |
| FR-5.4 | S4.5, S4.6 |
| FR-5.6 | S4.7 |
| FR-6.1 | S5.1, S5.2, S5.3, S5.4, S5.5 |
| FR-6.2 | S5.1 |
| FR-6.3 | S5.3 |
| FR-6.4 | S5.2, S5.4, S5.5 |
| FR-7.1 | S6.1, S6.2 |
| FR-7.2 | S6.3, S6.4, S6.5 |
| FR-8.1 | S1.6 |
| FR-8.2 | S1.6 |
| FR-8.3 | S1.7 |
