# Technical Design Specification
## Auth/IAM Service

**Version:** 1.0
**Date:** 2026-03-29
**Status:** Draft
**BRD Reference:** `2026-03-29-auth-iam-service-brd.md`
**BDD Reference:** `2026-03-29-auth-iam-service-bdd.md`

---

## 1. Architecture Overview

### Service Architecture — Hexagonal (Ports & Adapters)

The Auth/IAM service follows hexagonal (Ports & Adapters) architecture.

```
auth-iam-service/
├── domain/                    # Pure business logic (no framework dependencies)
│   ├── model/                 # Entities, value objects (Java Records)
│   ├── port/                  # Interfaces (inbound + outbound)
│   │   ├── in/                # Inbound ports (use cases)
│   │   └── out/               # Outbound ports (repository, gateway, messaging)
│   └── service/               # Domain services (business rules)
├── application/               # Use case orchestration
├── infrastructure/            # Adapters (implementations of ports)
│   ├── web/                   # REST controllers (inbound adapter)
│   ├── persistence/           # JPA repositories (outbound adapter)
│   ├── messaging/             # Kafka producers/consumers (outbound adapter)
│   └── external/              # Feign clients for other services (outbound adapter)
└── config/                    # Spring configuration, beans
```

**Key Rules:**
- `domain/` has ZERO Spring/JPA/Kafka imports — pure Java
- `infrastructure/` implements ports defined in `domain/port/`
- Controllers accept DTOs, call use cases, return DTOs — never expose entities
- All security calculations and state changes in `domain/service/`

### Hexagonal Pattern Details

```
domain/
├── model/
│   ├── UserRecord.java
│   ├── RoleRecord.java
│   ├── PermissionRecord.java
│   ├── SessionRecord.java
│   └── AuditLogRecord.java
├── port/
│   ├── in/
│   │   ├── AuthenticateUserUseCase.java
│   │   ├── ManageUserUseCase.java
│   │   ├── ManageRoleUseCase.java
│   │   ├── ValidateTokenUseCase.java
│   │   └── CheckPermissionUseCase.java
│   └── out/
│       ├── UserRepository.java
│       ├── RoleRepository.java
│       ├── PermissionRepository.java
│       ├── SessionRepository.java
│       └── AuditLogRepository.java
└── service/
    ├── AuthenticationService.java
    ├── AuthorizationService.java
    ├── UserManagementService.java
    ├── TokenService.java
    └── AuditService.java
```

---

## 2. Service Boundaries & Data Flow

### Service Responsibility Matrix

| Service | Owns | Database | Exposes (Internal) | Exposes (External) |
|---------|------|----------|-------------------|-------------------|
| **Auth/IAM** | Users, Roles, Permissions, Sessions, Tokens | auth_db | POST /auth/token, POST /auth/validate, POST /auth/permission, POST /auth/revoke, GET /auth/user/{id}, GET /auth/role/{id} | — |

### Detailed Service Interfaces

| Interface Type | Connected System | Protocol | Data Exchanged |
|----------------|-----------------|----------|----------------|
| **Inbound (Web)** | API Gateway, Administrative UI, Service Clients | REST/HTTPS | **Req:** Authentication requests, user management requests. **Res:** Tokens, user data, permission results |
| **Outbound (Persistence)** | PostgreSQL Database | JDBC | **Req:** CRUD operations for users, roles, permissions, sessions, audit logs. **Res:** Query results, operation status |
| **Outbound (Cache)** | Redis | Redis Protocol | **Req:** Token blacklist, session storage, temporary data. **Res:** Cache hits/misses, stored values |
| **Outbound (Messaging)** | Apache Kafka | Async | **Event:** `USER_AUTHENTICATED`, `USER_LOGOUT`, `PASSWORD_CHANGED`, `ACCOUNT_LOCKED`. **Payload:** User ID, event details, timestamp |
| **Outbound (External)** | Other Microservices (via Feign) | REST/HTTPS | **Req:** Token validation requests, permission checks. **Res:** Validation results, permission decisions |

### Request Flow (Authentication Example)

```
External Client (POS Terminal / Admin UI)
    │ POST /auth/token
    │ Content-Type: application/json
    │ {
    │   "username": "john.doe",
    │   "password": "securePassword123!"
    │ }
    ▼
┌─────────────────────────┐
│ Spring Boot Controller  │
│ 1. Validate request     │
│ 2. Call AuthenticateUserUseCase
│ 3. Return response      │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ Auth Service Use Case   │
│ 1. Validate credentials │
│ 2. Check account status │
│ 3. Generate JWT tokens  │
│ 4. Store session info   │
│ 5. Publish auth event   │
│ 6. Return tokens        │
└─────────────────────────┘
            │
            ▼
External Client
    │ 200 OK
    │ {
    │   "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    │   "refresh_token": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
    │   "expires_in": 900,
    │   "token_type": "Bearer"
    │ }
    ▼
```

### Service-to-Service Communication (Token Validation)

```
Service A (e.g., Ledger Service)
    │ Internal Request with Auth Header
    │ Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
    ▼
┌─────────────────────────┐
│ Service A Feign Client  │
│ 1. Extract token        │
| 2. Call Auth Service    │
│ 3. Return validation    │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ Auth Service Use Case   │
│ 1. Validate token sig   │
│ 2. Check token expiry   │
│ 3. Check token blacklist│
│ 4. Extract claims       │
│ 5. Return validation    │
└───────────┬─────────────┘
            │
            ▼
Service A
    │ 200 OK (if valid)
    │ {
    │   "valid": true,
    │   "user_id": "john.doe",
    │   "permissions": ["ledger:read", "ledger:create"]
    │ }
    │ 401 Unauthorized (if invalid)
    │ {
    │   "valid": false,
    │   "error": "Token has expired"
    │ }
    ▼
```

### Database per Service

| Service | Database | Key Tables |
|---------|----------|------------|
| Auth/IAM | auth_db | users, roles, permissions, user_roles, role_permissions, sessions, audit_logs, token_blacklist |

No shared databases. No cross-service joins. Each service queries only its own DB.

### Degradation Strategy (Dependency Failure)

| Dependency | Failure Mode | Impact | Mitigation |
|------------|--------------|--------|------------|
| PostgreSQL | Unavailable | Cannot persist users, sessions, audit logs | Fail fast - return 503 Service Unavailable |
| Redis | Unavailable | Token blacklist ineffective, no session caching | Continue operation with warnings - security reduced but system functional |
| Kafka | Unavailable | Cannot publish audit events | Continue operation - events lost but core auth works |

---

## 3. API Design

### Internal API (Service-to-Service & Administrative)

**Contract:** OpenAPI 3.0 spec at `docs/openapi-internal.yaml` (per service)
**Auth:** Bearer JWT token (service account or user identity)
**Format:** JSON request/response

#### Key Endpoints:

| Method | Path | Description |
|--------|------|-------------|
| POST | /auth/token | Authenticate user credentials and issue access/refresh tokens |
| POST | /auth/refresh | Refresh access token using refresh token |
| POST | /auth/validate | Validate JWT token and return claims/permissions |
| POST | /auth/revoke | Revoke token (add to blacklist) |
| POST | /auth/permission | Check if user/service has specific permission |
| GET | /auth/user/{id} | Get user details by ID |
| POST | /auth/user | Create new user |
| PUT | /auth/user/{id} | Update user details |
| DELETE | /auth/user/{id} | Delete/user deactivate user |
| GET | /auth/role/{id} | Get role details by ID |
| POST | /auth/role | Create new role |
| PUT | /auth/role/{id} | Update role details |
| DELETE | /auth/role/{id} | Delete role |
| GET | /auth/permission/{id} | Get permission details by ID |
| POST | /auth/permission | Create new permission |
| GET | /auth/sessions/{userId} | Get active sessions for user |
| DELETE | /auth/sessions/{userId} | Invalidate all sessions for user |
| GET | /auth/audit-logs | Query audit logs with filters |
| GET | /actuator/health | Health check endpoint |
| GET | /actuator/info | Service information |

### Request Headers

| Header | Required | Description |
|--------|----------|-------------|
| Authorization | Yes | Bearer JWT token (for authenticated endpoints) |
| Content-Type | Yes (for POST/PUT) | application/json |
| X-Request-ID | No | Unique request ID for tracing |
| X-Real-IP | No | Client IP address (set by reverse proxy) |

### Internal API (Service-to-Service)

**Mechanism:** OpenFeign clients with Resilience4j circuit breakers
**Format:** JSON (internal DTOs)
**Auth:** Bearer JWT token (service account credentials)
**Timeout Config:** 5s for auth calls, with 3 retries for non-auth endpoints

### Internal OpenAPI Specs Location
`auth-iam-service/docs/openapi-internal.yaml`

### Common Response Envelope

**Success:**
```json
{
  "status": "COMPLETED",
  "data": { ...service-specific fields... },
  "timestamp": "2026-03-29T10:30:00+08:00"
}
```

**Error:**
```json
{
  "status": "FAILED",
  "error": {
    "code": "ERR_xxx",
    "message": "Human-readable message",
    "action_code": "DECLINE | RETRY | REVIEW",
    "trace_id": "distributed-trace-id",
    "timestamp": "2026-03-29T10:30:00+08:00"
  }
}
```

---

## 4. Domain Model Details

### ENT-1: UserRecord
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| userId | UUID | Yes | Unique user identifier |
| username | String(50) | Yes | Login username (unique) |
| email | String(100) | Yes | Email address (unique) |
| passwordHash | String(255) | Yes | BCrypt hashed password |
| fullName | String(200) | Yes | Full name of user |
| status | Enum | Yes | ACTIVE, LOCKED, EXPIRED, DISABLED |
| failedLoginAttempts | Integer | Yes | Count of consecutive failed login attempts |
| lockedUntil | Timestamp | Conditional | Timestamp until which account is locked |
| passwordChangedAt | Timestamp | Yes | When password was last changed |
| passwordExpiresAt | Timestamp | Conditional | When password expires (if policy enabled) |
| createdAt | Timestamp | Yes | Creation timestamp |
| updatedAt | Timestamp | Yes | Last update timestamp |
| lastLoginAt | Timestamp | Conditional | Last successful login timestamp |
| createdBy | String(100) | Yes | Who created the user |

### ENT-2: RoleRecord
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| roleId | UUID | Yes | Unique role identifier |
| roleName | String(50) | Yes | Role name (unique) |
| description | String(255) | Yes | Role description |
| isActive | Boolean | Yes | Whether role is active |
| createdAt | Timestamp | Yes | Creation timestamp |
| updatedAt | Timestamp | Yes | Last update timestamp |
| createdBy | String(100) | Yes | Who created the role |

### ENT-3: PermissionRecord
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| permissionId | UUID | Yes | Unique permission identifier |
| permissionKey | String(100) | Yes | Permission key (e.g., "ledger:read") |
| description | String(255) | Yes | Permission description |
| resource | String(50) | Yes | Resource this permission applies to |
| action | String(20) | Yes | Action permitted (read, create, update, delete) |
| isActive | Boolean | Yes | Whether permission is active |
| createdAt | Timestamp | Yes | Creation timestamp |
| updatedAt | Timestamp | Yes | Last update timestamp |
| createdBy | String(100) | Yes | Who created the permission |

### ENT-4: SessionRecord
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| sessionId | UUID | Yes | Unique session identifier |
| userId | UUID (FK) | Yes | References UserRecord.userId |
| refreshTokenHash | String(255) | Yes | Hashed refresh token |
| userAgent | String(255) | Conditional | User agent string |
| ipAddress | String(45) | Yes | IP address of session |
| createdAt | Timestamp | Yes | Session creation timestamp |
| expiresAt | Timestamp | Yes | Session expiration timestamp |
| lastAccessedAt | Timestamp | Yes | Last time session was used |
| revokedAt | Timestamp | Conditional | When session was revoked |
| isActive | Boolean | Yes | Whether session is active |

### ENT-5: AuditLogRecord
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| auditId | UUID | Yes | Unique audit log identifier |
| entityType | String(50) | Yes | Type of entity (USER, ROLE, PERMISSION, SESSION, TOKEN) |
| entityId | UUID | Yes | ID of the entity |
| action | Enum | Yes | CREATE, UPDATE, DELETE, AUTHENTICATE, LOGOUT, PERMISSION_CHECK, TOKEN_VALIDATE, etc. |
| performedBy | String(100) | Yes | Who performed the action (userId or "system") |
| changes | JSON | Conditional | Field changes (for UPDATE operations) |
| ipAddress | String(45) | Yes | Source IP address |
| timestamp | Timestamp | Yes | When action occurred |
| outcome | Enum | Yes | SUCCESS, FAILURE |
| failureReason | String(500) | Conditional | Reason for failure if applicable |

### ENT-6: TokenBlacklistRecord
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| blacklistId | UUID | Yes | Unique blacklist entry identifier |
| tokenJti | String(255) | Yes | JWT ID (jti claim) to blacklist |
| userId | UUID (FK) | Yes | References UserRecord.userId (if applicable) |
| clientId | String(100) | Conditional | Service account ID (if applicable) |
| revokedAt | Timestamp | Yes | When token was revoked |
| expiresAt | Timestamp | Yes | When blacklist entry expires (matches token expiry) |
| revokedBy | String(100) | Yes | Who revoked the token (userId or "system") |
| reason | String(500) | Yes | Reason for revocation |

---

## 5. Security Architecture

### Authentication Flow
```
Client Credentials
        │
        ▼
┌─────────────────────┐
│ Auth Service        │
│ 1. Validate input   │
│ 2. Fetch user record│
│ 3. Verify password  │
│ 4. Check account    │
│    status           │
│ 5. Increment failed │
│    attempts on      │
│    mismatch         │
│ 6. Reset failed     │
│    attempts on      │
│    success          │
│ 7. Check lockout    │
│    policy           │
│ 8. Generate tokens  │
│ 9. Hash & store     │
│    refresh token    │
│10. Create session   │
│11. Publish event    │
│12. Return tokens    │
└─────────────────────┘
        │
        ▼
Client receives JWT tokens
```

### Token Structure (JWT)
```
Header: {"alg": "RS256", "typ": "JWT"}
Payload: {
  "iss": "auth.agentbanking.com",
  "sub": "user-id-or-client-id",
  "aud": "agentbanking-platform",
  "exp": 1234567890,
  "iat": 1234567890,
  "jti": "unique-token-id",
  "userId": "user-uuid",
  "username": "john.doe",
  "roles": ["TELLER", "SUPERVISOR"],
  "permissions": ["ledger:read", "ledger:create", "account:view"],
  "tokenType": "access" or "refresh"
}
Signature: RS256 signature using private key
```

### Authorization Model
- Role-Based Access Control (RBAC)
- Users assigned to roles
- Roles assigned to permissions
- Permissions evaluated when accessing resources
- Permission format: `{resource}:{action}` (e.g., "ledger:read", "account:create")
- Service accounts treated similarly to users but with client credentials grant

### Password Security
- Passwords hashed using BCrypt with configurable workload
- Never stored or transmitted in plaintext
- Password policies configurable (length, complexity, expiration)
- Password change required on first login after reset
- Password reuse prevention (configurable history size)

### Session Management
- Sessions tracked via refresh tokens
- Refresh tokens hashed before storage
- Session invalidation on logout, password change, or admin revocation
- Concurrent session limits configurable
- Session timeout configurable (absolute and idle timeout)

### Cryptographic Standards
- JWT signing: RS256 (asymmetric) with key rotation support
- Password hashing: BCrypt with workload factor 12
- Token blacklist: SHA-256 hashing of JTI values before storage
- Secure random: SecureRandom for token generation
- Key management: Private key stored in secure keystore/secret manager

### Transport Security
- All service communication over HTTPS/TLS 1.2+
- Internal service-to-service communication uses mutual TLS (mTLS)
- Certificate validation enabled for all connections
- HTTP Strict Transport Security (HSTS) enabled
- Content Security Policy (CSP) headers set

---

## 6. Error Handling & Security

### Error Handling Architecture

**Global Exception Handler** — every service implements `@ControllerAdvice`:

```
Request → Controller → Service → Exception thrown
                                     │
                                     ▼
                           GlobalExceptionHandler
                           - Maps exception → error code
                           - Returns standardized JSON
                           - Logs (without PII)
```

**Error Code Registry** (centralized, shared via common module):

| Category | Code Range | Example |
|----------|-----------|---------|
| Validation | ERR_VAL_xxx | ERR_INVALID_USERNAME, ERR_WEAK_PASSWORD |
| Authentication | ERR_AUTH_xxx | ERR_INVALID_CREDENTIALS, ERR_ACCOUNT_LOCKED |
| Authorization | ERR_AUTHZ_xxx | ERR_INSUFFICIENT_PERMISSIONS, ERR_ROLE_NOT_FOUND |
| Token | ERR_TOKEN_xxx | ERR_TOKEN_EXPIRED, ERR_TOKEN_REVOKED, ERR_INVALID_TOKEN |
| User Management | ERR_USER_xxx | ERR_USERNAME_EXISTS, ERR_EMAIL_EXISTS, ERR_USER_NOT_FOUND |
| System | ERR_SYS_xxx | ERR_SERVICE_UNAVAILABLE, ERR_INTERNAL |

Each error code maps to a message template (localization-ready).

### Security Measures

1. **Password Protection**
   - BCrypt hashing with salt
   - No password logging or exposure in responses
   - Secure password reset with time-limited tokens

2. **Token Security**
   - Short-lived access tokens (15 minutes)
   - Refresh token rotation to prevent replay attacks
   - JWT signature validation on every request
   - Token blacklist for immediate revocation
   - HTTPS-only cookie flags for web clients (if used)

3. **Input Validation**
   - All inputs validated using jakarta.validation
   - SQL injection prevention via parameterized queries
   - No sensitive data in URLs or logs
   - Username/email enumeration prevention (same error messages)

4. **Audit & Monitoring**
   - All authentication attempts logged (success/failure)
   - All authorization decisions logged
   - Security-relevant events (account lockouts, password changes) logged
   - Audit logs immutable and append-only
   - Failed login rate limiting and alerting

5. **Defense in Depth**
   - Rate limiting on authentication endpoints
   - Account lockout after failed attempts
   - Password breach checking (if implemented)
   - Regular security scanning and penetration testing
   - Dependency vulnerability monitoring

### Brute Force Protection
- Failed attempt tracking per username/IP combination
- Progressive delays (linear or exponential backoff)
- Account lockout after configurable threshold (default: 5 attempts)
- Lockout duration configurable (default: 15 minutes)
- CAPTCHA implementation consideration for web interfaces

---