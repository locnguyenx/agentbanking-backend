# BDD Specification: Auth/IAM Service

**Version:** 1.0
**Date:** 2026-03-29
**Status:** Draft
**Module:** Auth/IAM Service (`com.agentbanking.auth`)
**BRD Reference:** `docs/superpowers/specs/auth-iam-service/2026-03-29-auth-iam-service-brd.md`

Each BDD scenario is tagged with exactly one `@US` (User Story) and one `@FR` (Functional Requirement) for atomic traceability.

---

## 1. User Management

```gherkin
Feature: User Account Management

  @US-AUTH-01 @FR-1.1
  Scenario: Administrator creates a new user account
    Given an administrator "admin" is authenticated
    When admin creates a user with username "john.doe", email "john.doe@bank.com", fullName "John Doe"
    Then the user is created with status "ACTIVE"
    And the user has username "john.doe"
    And the user has email "john.doe@bank.com"

  @US-AUTH-01 @FR-1.3
  Scenario: Duplicate username is rejected
    Given a user "john.doe" exists with username "john.doe"
    When an administrator attempts to create a user with username "john.doe"
    Then the creation is rejected with error "Username already exists"

  @US-AUTH-01 @FR-1.3
  Scenario: Duplicate email is rejected
    Given a user "jane.doe" exists with email "jane.doe@bank.com"
    When an administrator attempts to create a user with email "jane.doe@bank.com"
    Then the creation is rejected with error "Email already exists"

  @US-AUTH-02 @FR-1.2
  Scenario: Administrator assigns role to user
    Given a user "john.doe" exists with status "ACTIVE"
    And a role "TELLER" exists
    When an administrator assigns role "TELLER" to user "john.doe"
    Then the user "john.doe" has role "TELLER"

  @US-AUTH-06 @FR-1.4
  Scenario: Administrator locks user account
    Given a user "john.doe" exists with status "ACTIVE"
    When an administrator locks the user account "john.doe"
    Then the user "john.doe" has status "LOCKED"

  @US-AUTH-07 @FR-1.2
  Scenario: Administrator resets user password
    Given a user "john.doe" exists with status "ACTIVE"
    When an administrator resets the password for user "john.doe"
    Then the user "john.doe" must change password at next login
    And a temporary password is generated and provided to administrator

  @US-AUTH-01 @FR-1.1
  Scenario: Administrator deletes user account
    Given a user "john.doe" exists with status "ACTIVE"
    When an administrator deletes the user account "john.doe"
    Then the user "john.doe" is no longer retrievable
    And the user "john.doe" has status "DELETED"
```

## 2. Authentication

```gherkin
Feature: User Authentication

  @US-AUTH-16 @FR-2.1
  Scenario: End user authenticates with valid credentials
    Given a user "john.doe" exists with username "john.doe" and password hash for "CorrectPass123!"
    When the user attempts to authenticate with username "john.doe" and password "CorrectPass123!"
    Then authentication is successful
    And an access token is returned
    And a refresh token is returned
    And the access token contains user_id claim "john.doe"
    And the access token expires in configured time

  @US-AUTH-16 @FR-2.1
  Scenario: End user authentication fails with invalid password
    Given a user "john.doe" exists with username "john.doe" and password hash for "CorrectPass123!"
    When the user attempts to authenticate with username "john.doe" and password "WrongPass456!"
    Then authentication fails
    And error "Invalid credentials" is returned
    And no tokens are issued

  @US-AUTH-16 @FR-2.1
  Scenario: End user authentication fails with non-existent user
    Given no user exists with username "nonexistent"
    When an attempt is made to authenticate with username "nonexistent" and password "AnyPass123!"
    Then authentication fails
    And error "Invalid credentials" is returned
    And no tokens are issued

  @US-AUTH-17 @FR-2.3
  Scenario: End user refreshes access token
    Given a user "john.doe" exists with username "john.doe"
    And a valid refresh token exists for user "john.doe"
    When the user presents the refresh token to obtain new access token
    Then a new access token is issued
    And the new access token has same user_id claim "john.doe"
    And the old refresh token is invalidated (if rotation enabled)

  @US-AUTH-18 @FR-2.6
  Scenario: End user logs out and invalidates session
    Given a user "john.doe" exists with username "john.doe"
    And a valid access token exists for user "john.doe"
    When the user logs out presenting the access token
    Then the access token is added to blacklist
    And subsequent requests with that token are rejected
    And error "Token has been revoked" is returned

  @US-AUTH-21 @FR-2.4
  Scenario: Service account authenticates with client credentials
    Given a service account "ledger-service" exists with clientId "ledger-service" and clientSecret "secure-secret"
    When the service account authenticates with grant_type "client_credentials"
    Then authentication is successful
    And an access token is returned
    And the access token contains service_id claim "ledger-service"
    And the access token has appropriate service permissions

  @US-AUTH-22 @FR-2.5
  Scenario: Service account uses token to access protected resource
    Given a service account "ledger-service" exists with valid access token
    And the token contains permission "ledger:read"
    When the service presents the token to ledger service for a protected resource
    Then access is granted
    And the ledger service validates token with auth service
```

## 3. Authorization & Access Control

```gherkin
Feature: Role-Based Access Control

  @US-AUTH-02 @FR-3.1
  Scenario: Administrator creates role with permissions
    Given an administrator "admin" is authenticated
    When admin creates a role "TELLER" with permissions ["account:read", "transaction:create"]
    Then the role "TELLER" is created
    And the role has permission "account:read"
    And the role has permission "transaction:create"

  @US-AUTH-04 @FR-3.2
  Scenario: Administrator assigns multiple roles to user
    Given a user "john.doe" exists with status "ACTIVE"
    And a role "TELLER" exists with permissions ["account:read"]
    And a role "CASHIER" exists with permissions ["transaction:create"]
    When an administrator assigns roles "TELLER" and "CASHIER" to user "john.doe"
    Then the user "john.doe" has role "TELLER"
    And the user "john.doe" has role "CASHIER"
    And the user has permission "account:read"
    And the user has permission "transaction:create"

  @US-AUTH-19 @FR-3.5
  Scenario: Service requests permission validation
    Given a user "john.doe" exists with username "john.doe"
    And the user has permission "ledger:create"
    When the ledger service requests permission validation for user "john.doe" on resource "ledger" with action "create"
    Then the auth service returns permission granted: true
    When the ledger service requests permission validation for user "john.doe" on resource "ledger" with action "delete"
    And the user does not have permission "ledger:delete"
    Then the auth service returns permission granted: false

  @US-AUTH-10 @FR-3.1
  Scenario: System operator creates service account
    Given a system operator "sysop" is authenticated
    When sysop creates a service account with name "biller-service" and description "Handles bill payments"
    Then the service account "biller-service" is created
    And the service account has status "ACTIVE"
    And the service account can authenticate with client credentials
```

## 4. Token Management

```gherkin
Feature: JWT Token Lifecycle Management

  @US-AUTH-17 @FR-4.2
  Scenario: Refresh token rotation prevents reuse
    Given a user "john.doe" exists
    And a valid refresh token RT1 exists for user "john.doe"
    When the user uses RT1 to obtain new access token AT2 and new refresh token RT2
    Then access token AT2 is issued
    And refresh token RT2 is issued
    When the user attempts to use RT1 again to obtain another token
    Then the request is rejected
    And error "Refresh token has been used" is returned

  @US-AUTH-20 @FR-4.4
  Scenario: Token introspection returns valid token details
    Given a user "john.doe" exists with valid access token AT
    When the token AT is presented to introspection endpoint
    Then the response includes active: true
    And the response includes user_id: "john.doe"
    And the response includes exp: <future timestamp>
    And the response includes iat: <past timestamp>
    And the response includes scope: ["account:read", "ledger:create"]

  @US-AUTH-20 @FR-4.4
  Scenario: Token introspection returns inactive for expired token
    Given an expired access token ET exists
    When the token ET is presented to introspection endpoint
    Then the response includes active: false
    And error "Token has expired" may be present

  @US-AUTH-15 @FR-4.5
  Scenario: Administrator revokes user token
    Given a user "john.doe" exists with valid access token AT
    When an administrator revokes tokens for user "john.doe"
    Then the access token AT is added to blacklist
    And subsequent requests with AT are rejected
    And error "Token has been revoked" is returned

  @US-AUTH-15 @FR-4.3
  Scenario: System operator manages token blacklist
    Given a system operator "sysop" is authenticated
    When sysop adds token JTI-12345 to blacklist with reason "Security incident"
    Then the blacklist entry exists
    And any attempt to use token with JTI-12345 is rejected
    When sysop removes token JTI-12345 from blacklist
    Then the blacklist entry no longer exists
    And token JTI-12345 can be used again (if not expired)
```

## 5. Security & Auditing

```gherkin
Feature: Security Event Logging and Auditing

  @US-AUTH-09 @FR-5.1
  Scenario: Successful authentication is audited
    Given a user "john.doe" exists with valid credentials
    When the user authenticates successfully from IP "192.168.1.100"
    Then an audit log entry is created
    And the entry has action "AUTHENTICATE_SUCCESS"
    And the entry has user_id "john.doe"
    And the entry has ip_address "192.168.1.100"
    And the entry has outcome "SUCCESS"

  @US-AUTH-09 @FR-5.1
  Scenario: Failed authentication is audited
    Given a user "john.doe" exists
    When authentication fails for username "john.doe" with wrong password from IP "10.0.0.50"
    Then an audit log entry is created
    And the entry has action "AUTHENTICATE_FAILURE"
    And the entry has user_id "john.doe"
    And the entry has ip_address "10.0.0.50"
    And the entry has outcome "FAILURE"
    And the entry has failure_reason "Invalid credentials"

  @US-AUTH-06 @FR-5.3
  Scenario: Account lockout after failed attempts is audited
    Given a user "john.doe" exists with max failed attempts set to 3
    When authentication fails for user "john.doe" for the 3rd consecutive time
    Then the user account is locked
    And an audit log entry is created with action "ACCOUNT_LOCKED"
    And the entry has user_id "john.doe"
    And the entry has failure_count 3

  @US-AUTH-09 @FR-5.2
  Scenario: Authorization decision is audited
    Given a user "john.doe" exists with permission "ledger:read"
    And user "john.doe" lacks permission "ledger:delete"
    When the ledger service requests validation for "ledger:read" action
    Then permission is granted
    And an audit log entry is created with action "AUTHORIZATION_GRANT"
    And the entry has user_id "john.doe"
    And the entry has resource "ledger"
    And the entry has action "read"
    When the ledger service requests validation for "ledger:delete" action
    Then permission is denied
    And an audit log entry is created with action "AUTHORIZATION_DENY"
    And the entry has user_id "john.doe"
    And the entry has resource "ledger"
    And the entry has action "delete"

  @US-AUTH-09 @FR-5.5
  Scenario: Administrator exports audit logs
    Given an administrator "admin" is authenticated
    And audit logs exist for the past week
    When admin requests audit log export for date range 2026-03-20 to 2026-03-27
    Then an export file is generated
    And the file contains all audit entries in the date range
    And the file is in CSV format with headers: timestamp, action, user_id, resource, action, outcome
```

## 6. Integration & Configuration

```gherkin
Feature: Service Integration and Configuration

  @US-AUTH-10 @FR-6.1
  Scenario: Ledger service validates token via OpenFeign client
    Given a ledger service instance is running
    And the ledger service has OpenFeign client configured for auth service
    And a valid access token exists for user "john.doe"
    When the ledger service receives a request with Authorization header containing the token
    Then the ledger service calls auth service to validate token
    And the auth service returns token validation result
    And if valid, the ledger service processes the request
    And if invalid, the ledger service returns 401 Unauthorized

  @US-AUTH-12 @FR-6.2
  Scenario: Administrator configures JWT signing settings
    Given an administrator "admin" is authenticated
    When admin updates JWT signing algorithm to "RS256"
    And admin sets access token expiration to 15 minutes
    Then the configuration is saved
    And newly issued tokens use RS256 algorithm
    And newly issued tokens expire in 15 minutes
    And existing tokens continue to work until expiration

  @US-AUTH-05 @FR-6.4
  Scenario: Administrator configures password policy
    Given an administrator "admin" is authenticated
    When admin sets minimum password length to 12
    And admin requires at least one uppercase letter
    And admin requires at least one digit
    And admin requires at least one special character
    Then the password policy is updated
    When a user attempts to set password "Pass1!"
    Then the password is rejected
    When a user sets password "SecurePass123!@#"
    Then the password is accepted

  @US-AUTH-13 @FR-6.5
  Scenario: Health check endpoint reports service status
    Given the auth service is running normally
    When a health check request is made to /actuator/health
    Then the response includes status: "UP"
    And the response includes component: "database" with status: "UP"
    And the response includes component: "redis" with status: "UP"
```

---