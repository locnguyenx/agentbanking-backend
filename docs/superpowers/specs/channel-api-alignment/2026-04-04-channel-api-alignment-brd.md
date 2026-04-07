# Business Requirements Document: Channel API Alignment

**Version:** 1.0
**Date:** 2026-04-04
**Status:** Draft
**Module:** rules-service, biller-service, onboarding-service, docs/api/openapi.yaml

---

## 1. Business Goals & Success Criteria

### Business Goals

1. Implement 3 missing API endpoints required by the Channel App (POS terminals at agent retail locations)
2. Fix OpenAPI specification quality issues that cause frontend-backend contract drift
3. Ensure all monetary values use string type to prevent floating-point precision loss
4. Add consistent security definitions and error response schemas across all endpoints

### Success Criteria

- All 3 endpoints functional and reachable through the Spring Cloud Gateway with JWT authentication
- OpenAPI spec passes validation with zero `type: number` for monetary values
- Every `/api/v1/*` endpoint has bearerAuth security requirement and error response schemas
- No endpoint uses `'*/*'` content type — all use `application/json`
- Generated client code from OpenAPI spec compiles without manual edits

---

## 2. User Roles & Stories

### Roles

| Role | Responsibilities |
|------|-----------------|
| **Channel App (Agent POS)** | Android/Flutter app used by agents at retail locations to perform transactions |
| **Channel App Developer** | Frontend developer consuming the OpenAPI spec to generate client code |

### User Stories

- **US-001** As a Channel App user (agent), I want to get a quote with fees and commission before executing a transaction, so that I can show the customer the exact total amount and my commission
- **US-002** As a Channel App user (agent), I want to look up a DuitNow proxy ID (phone, NRIC, etc.) and get the registered account holder's name, so that I can confirm the recipient identity before transferring
- **US-003** As a Channel App, I want to check if an agent's compliance status is locked or unlocked, so that I can prevent locked agents from performing transactions
- **US-004** As a Channel App developer, I want the OpenAPI spec to use correct data types (strings for monetary amounts), include security definitions, and have consistent error response schemas, so that my generated client code works correctly without manual fixes

### User Story to Functional Requirement Mapping

| User Story | Functional Requirements |
|------------|------------------------|
| US-001 | FR-001 |
| US-002 | FR-002 |
| US-003 | FR-003 |
| US-004 | FR-004, FR-005, FR-006 |

---

## 3. Functional Requirements

### FR-001: Transaction Quote Endpoint (US-001)

- **FR-001.1** Endpoint: `POST /api/v1/transactions/quote`
- **FR-001.2** Service: rules-service (internal path: `/internal/transactions/quote`)
- **FR-001.3** Request body: `amount` (string/decimal), `serviceCode` (string), `fundingSource` (enum: CARD_EMV, CASH, DUITNOW_MOBILE, DUITNOW_MYKAD, DUITNOW_BRN, MYKAD_BIOMETRIC, DUITNOW_QR), optional `billerRouting` (ON_US, OFF_US). Agent identity extracted from JWT claims (not in request body).
- **FR-001.4** Response: `quoteId` (string), `amount` (string), `fee` (string), `total` (string), `commission` (string)
- **FR-001.5** Business logic: Fee calculated via rules-service fee engine. Commission rate sourced from `agent_tier` claim in JWT. If claim missing, fallback to database lookup by agent ID from token subject.
- **FR-001.6** Security: Bearer JWT required
- **FR-001.7** Error responses: 400 (validation), 401 (unauthorized), 422 (quote calculation failed)
- **FR-001.8** Idempotency: Quote is a read-only calculation with no side effects. Exempt from AGENTS.md Law II idempotency requirement. No `X-Idempotency-Key` header required.

### FR-002: DuitNow Proxy Enquiry Endpoint (US-002)

- **FR-002.1** Endpoint: `GET /api/v1/transfer/proxy/enquiry`
- **FR-002.2** Service: biller-service (internal path: `/internal/transfer/proxy/enquiry`)
- **FR-002.3** Query parameters: `proxyId` (string, required), `proxyType` (string, required, enum: MOBILE, NRIC, PASSPORT, BIZ_REG_NO)
- **FR-002.4** Response: `{ "name": "string", "proxyType": "string" }`. Only name and proxyType exposed to channel app; downstream DuitNow fields (bank code, account type) are internal.
- **FR-002.5** Business logic: Calls downstream DuitNow network via switch adapter
- **FR-002.6** Security: Bearer JWT required
- **FR-002.7** Error responses: 400 (missing parameter), 401 (unauthorized), 404 (proxy not found)

### FR-003: Compliance Status Endpoint (US-003)

- **FR-003.1** Endpoint: `GET /api/v1/compliance/status`
- **FR-003.2** Service: onboarding-service (internal path: `/internal/compliance/status`)
- **FR-003.3** Response: `{ "status": "LOCKED" | "UNLOCKED", "reason": "string (optional)", "checkedAt": "ISO 8601 timestamp" }`
- **FR-003.4** Business logic: Checks agent compliance rules, AML flags, regulatory holds
- **FR-003.5** Security: Bearer JWT required, agent identity extracted from token
- **FR-003.6** Error responses: 401 (unauthorized), 503 (compliance service unavailable)

### FR-004: OpenAPI Amount Type Fix (US-004)

- **FR-004.1** All monetary fields (`amount`, `fee`, `total`, `commission`, `balance`, etc.) MUST use `type: string` instead of `type: number`
- **FR-004.2** Applies to all request and response schemas in the spec

### FR-005: OpenAPI Security Definition (US-004)

- **FR-005.1** Add `securitySchemes.bearerAuth` component with type `http`, scheme `bearer`, bearerFormat `JWT`
- **FR-005.2** Add `security: [{ bearerAuth: [] }]` to all `/api/v1/*` endpoints
- **FR-005.3** Internal `/internal/*` endpoints are excluded from security in the spec

### FR-006: OpenAPI Error Response Consistency (US-004)

- **FR-006.1** Every endpoint MUST have `400` response with `ErrorResponse` schema
- **FR-006.2** Every endpoint MUST have `401` response with `ErrorResponse` schema
- **FR-006.3** Remove all `'*/*'` content types, replace with `application/json`
- **FR-006.4** Add proper `enum` constraints to `proxyType`, `fundingSource` fields

---

## 4. Entity Definitions

### Transaction Quote Request
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| amount | String | Yes | Transaction amount (decimal string, e.g. "100.00") |
| serviceCode | String | Yes | Transaction type code (e.g. "CASH_WITHDRAWAL") |
| fundingSource | String | Yes | Enum: CARD_EMV, CASH, DUITNOW_MOBILE, DUITNOW_MYKAD, DUITNOW_BRN, MYKAD_BIOMETRIC, DUITNOW_QR |
| billerRouting | String | No | Enum: ON_US, OFF_US (for biller transactions) |

### Transaction Quote Response
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| quoteId | String | Yes | Unique quote identifier (e.g., "QT-12345") |
| amount | String | Yes | Transaction amount (decimal string) |
| fee | String | Yes | Calculated fee (decimal string) |
| total | String | Yes | amount + fee (decimal string) |
| commission | String | Yes | Agent commission (decimal string) |

### Proxy Enquiry Response
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | String | Yes | Registered account holder name |
| proxyType | String | Yes | Type of proxy resolved (MOBILE, NRIC, PASSPORT, BIZ_REG_NO) |

### Compliance Status Response
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| status | String | Yes | LOCKED or UNLOCKED |
| reason | String | No | Reason for lock (present when LOCKED) |
| checkedAt | String | Yes | ISO 8601 timestamp of status check |

### ErrorResponse (Global Error Schema)
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| status | String | Yes | Always "FAILED" |
| error | Object | Yes | Nested error detail object |
| error.code | String | Yes | Error code (e.g., "ERR_VAL_INVALID_REQUEST") |
| error.message | String | Yes | Human-readable error message |
| error.action_code | String | Yes | DECLINE, RETRY, or REVIEW |
| error.trace_id | String | No | Distributed trace ID |
| error.timestamp | String | Yes | ISO 8601 timestamp |

### JWT Claims (Agent Identity)
| Claim | Type | Description |
|-------|------|-------------|
| sub | String | User ID (UUID format) |
| agentId | String | Agent ID (UUID format) |
| agent_tier | String | Agent tier level (MICRO, STANDARD, PREMIUM) |
| roles | Array | User roles for authorization |

---

## 5. Non-Functional Requirements

### NFR-1: Performance

- Transaction quote response: < 200ms (including fee calculation)
- Proxy enquiry response: < 500ms (including downstream switch call)
- Compliance status response: < 100ms

### NFR-2: Security

- All endpoints require valid JWT bearer token
- No PII in logs (mask names, never log proxy values in full)
- Agent identity always extracted from JWT claims (never from request body)
- Quote endpoint is read-only calculation, exempt from idempotency requirement (AGENTS.md Law II)

### NFR-3: Rate Limiting

- Transaction quote: 60 requests/minute per agent
- Proxy enquiry: 30 requests/minute per agent
- Compliance status: 10 requests/minute per agent

### NFR-4: API Contract

- OpenAPI spec is the single source of truth for channel app integration
- All changes to endpoints must be reflected in the spec before deployment
- Error responses follow the Global Error Schema (ERR_xxx codes, action_code, trace_id)

---

## 6. Constraints & Assumptions

### Constraints

- Must follow existing hexagonal architecture pattern in each service
- Must use existing tech stack: Java 21, Spring Boot 3.x, Spring Cloud Gateway
- Must follow AGENTS.md conventions: DTOs as records, constructor injection, domain layer with zero framework imports
- Gateway routes must use existing JwtAuth filter pattern
- Error codes must come from centralized `ErrorCodes.java` in common module

### Assumptions

- Fee calculation for quotes calls the existing rules-service fee engine
- Proxy enquiry connects to the existing DuitNow switch adapter infrastructure
- Compliance status checks agent identity from JWT token claims
- The `api_mismatch_report.md` is the authoritative source for missing endpoints
- OpenAPI spec changes are backward-compatible (adding fields, not removing)
