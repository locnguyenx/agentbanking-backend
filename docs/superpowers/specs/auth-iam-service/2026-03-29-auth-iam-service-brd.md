# Business Requirements Document (BRD)
## Auth/IAM Service

**Version:** 1.0
**Date:** 2026-03-29
**Status:** Draft
**Module:** Auth/IAM Service (`com.agentbanking.auth`)

---

## 1. Project Overview & Goals

### Business Purpose
Provide centralized authentication and authorization services for the Agent Banking Platform, ensuring zero-trust architecture where every request is authenticated and every service is authorized.

### Target Users
- **Bank Operations (Backoffice)** — bank staff managing system access, user accounts, roles, and permissions
- **System Services** — microservices requiring secure inter-service communication
- **External Clients** — POS terminals and other external systems accessing the platform via API Gateway

### Deliverables
1. **Auth/IAM Microservice** — Centralized service for:
   - User authentication (username/password, token validation)
   - Authorization (role-based access control, permission checking)
   - User and role management
   - JWT token issuance, validation, and revocation
   - Session management
   - Audit logging for security events

2. **Integration Components**:
   - Updated Gateway configuration to delegate authentication to Auth service
   - Service-to-service authentication mechanisms
   - Client libraries for services to validate tokens and check permissions

### External API Consumers
- API Gateway (for external request authentication)
- All internal microservices (for service-to-service authentication)
- Administrative UI/Backoffice (for user management)

### Internal Communication
- Service-to-service calls (OpenFeign with Resilience4j circuit breakers)
- Database access (PostgreSQL for user/role/permission storage)
- Cache layer (Redis for session/token blacklist storage)

### Business Goals
1. Implement zero-trust security architecture as required by NFR-3.1
2. Centralize authentication logic currently scattered across services
3. Provide consistent authorization model across all platform services
4. Enable secure service-to-service communication
5. Support comprehensive audit trails for security-related events

### MVP Scope (Phase 1)
- Username/password authentication for backoffice users
- JWT token issuance and validation
- Role-based access control (RBAC) implementation
- Basic user and role management APIs
- Service-to-service authentication using service accounts
- Integration with API Gateway for external request authentication
- Audit logging for authentication and authorization events

### Full Platform Scope (Phase 2+)
- Multi-factor authentication (MFA) support
- Social login/OAuth integration
- Advanced permission models (ABAC - Attribute-Based Access Control)
- Password policy management
- Account lockout and brute force protection
- Self-service password reset
- Just-in-time access provisioning
- Advanced session management

---

## 2. User Stories

### User Roles & Stories

#### Roles

| Role | Responsibilities |
|------|-----------------|
| **Administrator** | Manages users, roles, permissions, and system security settings |
| **System Operator** | Monitors authentication events, manages service accounts, reviews audit logs |
| **End User** | Authenticates to access platform services (via POS terminal or administrative interfaces) |
| **Service Account** | Represents a microservice for secure inter-service communication |

#### User Stories

##### Administrator

- **US-AUTH-01** As an Administrator, I can create, read, update, and delete user accounts
- **US-AUTH-02** As an Administrator, I can assign roles and permissions to users
- **US-AUTH-03** As an Administrator, I can create, read, update, and delete roles
- **US-AUTH-04** As an Administrator, I can assign permissions to roles
- **US-AUTH-05** As an Administrator, I can define and manage password policies
- **US-AUTH-06** As an Administrator, I can lock/unlock user accounts
- **US-AUTH-07** As an Administrator, I can reset user passwords (with appropriate security controls)
- **US-AUTH-08** As an Administrator, I can configure multi-factor authentication settings
- **US-AUTH-09** As an Administrator, I can view and export authentication audit logs

##### System Operator

- **US-AUTH-10** As a System Operator, I can create and manage service accounts for microservices
- **US-AUTH-11** As a System Operator, I can assign appropriate permissions to service accounts
- **US-AUTH-12** As a System Operator, I can monitor authentication success/failure rates
- **US-AUTH-13** As a System Operator, I can review real-time authentication events
- **US-AUTH-14** As a System Operator, I can investigate suspicious authentication patterns
- **US-AUTH-15** As a System Operator, I can manage token blacklist/revocation lists

##### End User

- **US-AUTH-16** As an End User, I can authenticate using username and password to obtain a JWT token
- **US-AUTH-17** As an End User, I can refresh an expired JWT token using a refresh token
- **US-AUTH-18** As an End User, I can logout/invalidate my current session
- **US-AUTH-19** As an End User, I can change my own password (after authentication)
- **US-AUTH-20** As an End User, I can view my own authentication history and active sessions

##### Service Account

- **US-AUTH-21** As a Service Account, I can obtain a JWT token using client credentials grant
- **US-AUTH-22** As a Service Account, I can use my JWT token to authenticate with other microservices
- **US-AUTH-23** As a Service Account, I can have my permissions validated by the Auth service when requested by other services

---

## 3. Functional Requirements

### FR-1: User Management
- **FR-1.1** System shall support CRUD operations for user accounts
- **FR-1.2** System shall store user credentials securely using industry-standard hashing algorithms
- **FR-1.3** System shall enforce unique usernames/email addresses per user
- **FR-1.4** System shall support user account status (active, locked, expired, disabled)
- **FR-1.5** System shall track user login attempts and implement account lockout after failed attempts

### FR-2: Authentication
- **FR-2.1** System shall authenticate users via username/password credentials
- **FR-2.2** System shall validate JWT tokens presented for authentication
- **FR-2.3** System shall support refresh token flow for obtaining new access tokens
- **FR-2.4** System shall support service account authentication using client credentials grant
- **FR-2.5** System shall generate JWT tokens with appropriate claims (user_id, roles, permissions, expiry)
- **FR-2.6** System shall invalidate tokens upon logout or token revocation

### FR-3: Authorization & Access Control
- **FR-3.1** System shall implement Role-Based Access Control (RBAC) for authorization decisions
- **FR-3.2** System shall support defining roles with associated permissions
- **FR-3.3** System shall support assigning multiple roles to users
- **FR-3.4** System shall evaluate permissions based on user's assigned roles
- **FR-3.5** System shall provide permission checking APIs for services to validate access
- **FR-3.6** System shall support hierarchical role inheritance (future enhancement)

### FR-4: Token Management
- **FR-4.1** System shall issue access tokens (JWT) with configurable expiration times
- **FR-4.2** System shall issue refresh tokens for obtaining new access tokens
- **FR-4.3** System shall maintain a token blacklist for revoked tokens
- **FR-4.4** System shall provide token introspection endpoint to check token validity and claims
- **FR-4.5** System shall support token revocation for immediate invalidation

### FR-5: Security & Auditing
- **FR-5.1** System shall log all authentication attempts (success and failure) with contextual information
- **FR-5.2** System shall log all authorization decisions (granted and denied) with user/resource/action details
- **FR-5.3** System shall log security-relevant events (account lockouts, password changes, etc.)
- **FR-5.4** System shall ensure audit logs are tamper-evident and immutable
- **FR-5.5** System shall support exporting audit logs for compliance reporting

### FR-6: Integration & Configuration
- **FR-6.1** System shall provide OpenFeign client for other services to validate tokens and check permissions
- **FR-6.2** System shall support configuration of JWT signing keys and validation parameters
- **FR-6.3** System shall support configuration of token expiration times and refresh policies
- **FR-6.4** System shall support configuration of password policy requirements
- **FR-6.5** System shall provide health check endpoints for monitoring service status

---