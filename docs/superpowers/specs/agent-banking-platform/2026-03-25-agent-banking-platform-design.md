# Technical Design Specification
## Agent Banking Platform (Malaysia)

**Version:** 1.0
**Date:** 2026-03-25
**Status:** Draft — Pending Review
**BRD Reference:** `2026-03-25-agent-banking-platform-brd.md`
**BDD Reference:** `2026-03-25-agent-banking-platform-bdd.md`

---

## 1. Architecture Overview

### System Architecture — Hexagonal per Service

Each microservice follows hexagonal (Ports & Adapters) architecture.

### 5-Tier System Architecture

```
┌─────────────────────────────────────────────────────────┐
│  Tier 1: Channel Layer                                   │
│  POS Terminals (Android/Flutter)                         │
│  REST/HTTPS → single entry point                         │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│  Tier 2: Spring Cloud Gateway                            │
│  - JWT validation, rate limiting, routing                │
│  - OpenAPI 3.0 spec (external contract)                  │
│  - Circuit breaker for downstream failures               │
└──────────────────────────┬──────────────────────────────┘
                           │ Internal REST (OpenFeign)
┌──────────────────────────▼──────────────────────────────┐
│  Tier 3: Domain Core Services                            │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐           │
│  │ Rules  │ │ Ledger │ │Onboard │ │ Biller │           │
│  │        │ │& Float │ │  ing   │ │        │           │
│  │Fee eng.│ │Wallets │ │e-KYC   │ │Payments│           │
│  │Limits  │ │Journals│ │JPN     │ │Webhooks│           │
│  │Velocity│ │Settlem.│ │Biometr.│ │        │           │
│  └────────┘ └────────┘ └────────┘ └────────┘           │
│  Hexagonal architecture. Pure domain logic.              │
│  Database-per-service. Kafka for async events.           │
└──────────────────────────┬──────────────────────────────┘
                           │ Outbound ports (adapters)
┌──────────────────────────▼──────────────────────────────┐
│  Tier 4: Translation Layer (Protocol Adapters)           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │HSM Connector│  │Switch Conn. │  │Biller Conn. │     │
│  │             │  │             │  │             │     │
│  │- DUKPT PIN  │  │- ISO 8583   │  │- JomPAY API │     │
│  │  blocks     │  │  bitmaps    │  │- ASTRO API  │     │
│  │- TCP/IP     │  │- ISO 20022  │  │- TM API     │     │
│  │  persistent │  │  XML        │  │- EPF API    │     │
│  │  sockets    │  │- MAC calc   │  │- Webhook    │     │
│  │- Key mgmt   │  │- Session    │  │  validation │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
│  Lives in infrastructure/ layer of each service.         │
└──────────────────────────┬──────────────────────────────┘
                           │ Legacy protocols
┌──────────────────────────▼──────────────────────────────┐
│  Tier 5: Downstream Systems (External Partners)          │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐      │
│  │   HSM   │ │ PayNet  │ │  JPN    │ │ Billers │      │
│  │(Hardware│ │(Card +  │ │(MyKad   │ │(ASTRO,  │      │
│  │ Security│ │ DuitNow)│ │ verify) │ │ TM, EPF,│      │
│  │ Module) │ │         │ │         │ │ JomPAY) │      │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘      │
└─────────────────────────────────────────────────────────┘
```

### Hexagonal Pattern per Service

```
service-name/
├── domain/                    # Pure business logic (no framework dependencies)
│   ├── model/                 # Entities, value objects (Java Records)
│   ├── port/                  # Interfaces (inbound + outbound)
│   │   ├── in/                # Inbound ports (use cases)
│   │   └── out/               # Outbound ports (repository, gateway, messaging)
│   └── service/               # Domain services (business rules)
├── application/               # Use case orchestration
│   └── usecase/               # Use case implementations (call domain)
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
- All financial calculations and state changes in `domain/service/`

---

## 2. Service Boundaries & Data Flow

### Service Responsibility Matrix

| Service | Owns | Database | Exposes (Internal) | Exposes (External via Gateway) |
|---------|------|----------|-------------------|-------------------------------|
| **Rules** | FeeConfig, VelocityRule, limit checks | rules_db | GET /fees, GET /limits, POST /check-velocity | — |
| **Ledger & Float** | AgentFloat, Transaction, JournalEntry | ledger_db | POST /debit, POST /credit, GET /balance, POST /reverse | — |
| **Onboarding** | KycVerification, agent/customer records | onboarding_db | POST /verify-mykad, POST /biometric-match | — |
| **Switch Adapter** | ISO 8583/20022 translation, PayNet integration | switch_db | POST /auth, POST /reversal | — |
| **Biller** | BillerConfig, biller webhooks | biller_db | POST /validate-ref, POST /pay-bill | — |
| **Gateway** | — | — | — | All external POS endpoints |

### External API Flow (Withdrawal Example)

```
POS Terminal
    │ POST /api/v1/withdrawal
    │ Authorization: Bearer <token>
    ▼
┌─────────────────────────┐
│ Spring Cloud Gateway    │
│ 1. Validate JWT         │
│ 2. Extract agentId      │
│ 3. Route to Service     │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ Ledger & Float Service  │
│ 1. Check idempotency    │
│ 2. Call Rules Service   │──── GET /check-velocity, GET /fees
│ 3. Validate limits      │
│ 4. Debit AgentFloat     │ (PESSIMISTIC_WRITE lock)
│ 5. Call Switch Adapter  │──── POST /auth (ISO 8583)
│ 6. Create JournalEntry  │
│ 7. Publish Kafka event  │
│ 8. Return response      │
└─────────────────────────┘
```

### Inter-Service Communication Rules

| From | To | Type | Mechanism | Resilience |
|------|-----|------|-----------|-----------|
| Ledger | Rules | Sync | OpenFeign | Resilience4j circuit breaker, retry 3x |
| Ledger | Switch Adapter | Sync | OpenFeign | Circuit breaker + Store & Forward for reversals |
| Ledger | Kafka | Async | Spring Cloud Stream | Fire-and-forget (SMS, Commission, EFM events) |
| Biller | Rules | Sync | OpenFeign | Circuit breaker |
| Biller | Ledger | Sync | OpenFeign | Circuit breaker |
| Any | Onboarding | Sync | OpenFeign | Circuit breaker |

### Database per Service

| Service | Database | Key Tables |
|---------|----------|-----------|
| Rules | rules_db | fee_config, velocity_rule |
| Ledger & Float | ledger_db | agent_float, transaction, journal_entry |
| Onboarding | onboarding_db | kyc_verification, agent, customer |
| Switch Adapter | switch_db | switch_log, reversal_queue |
| Biller | biller_db | biller_config, bill_payment |

No shared databases. No cross-service joins. Each service queries only its own DB.

### Degradation Strategy (Tier 5 Failure)

| Downstream | Failure Mode | Core Impact | Recovery |
|-----------|-------------|-------------|----------|
| HSM | Unavailable | All PIN transactions blocked | No fallback — security critical, wait for restore |
| PayNet | Unavailable | Card + DuitNow blocked | Store & Forward, auto-retry when restored |
| JPN | Unavailable | e-KYC queues for retry | Manual review fallback in backoffice |
| Biller | Unavailable | Specific bill payment rejected | Customer asked to retry later |

Core Ledger and Gateway stay online regardless.

---

## 3. API Design

### External API (Gateway → POS Terminal)

**Contract:** OpenAPI 3.0 spec at `docs/api/openapi.yaml`
**Auth:** Bearer JWT token (agent identity extracted from claims)
**Format:** JSON request/response

**Key Endpoints:**

| Method | Path | Service | Description |
|--------|------|---------|-------------|
| POST | /api/v1/withdrawal | Ledger | Cash withdrawal (EMV + PIN) |
| POST | /api/v1/deposit | Ledger | Cash deposit |
| POST | /api/v1/balance-inquiry | Ledger | Customer or agent balance |
| POST | /api/v1/kyc/verify | Onboarding | MyKad verification |
| POST | /api/v1/kyc/biometric | Onboarding | Biometric match |
| POST | /api/v1/bill/pay | Biller | Bill payment |
| POST | /api/v1/topup | Biller | Prepaid top-up |
| POST | /api/v1/transfer/duitnow | Switch | DuitNow transfer |
| GET | /api/v1/agent/balance | Ledger | Agent wallet balance |

### Request Headers

| Header | Required | Description |
|--------|----------|-------------|
| Authorization | Yes | Bearer JWT token |
| X-Idempotency-Key | Yes | UUID for dedup |
| X-POS-Terminal-Id | Yes | Terminal device identifier |
| X-GPS-Latitude | Yes | Current terminal GPS |
| X-GPS-Longitude | Yes | Current terminal GPS |

### Internal API (Service-to-Service)

**Mechanism:** OpenFeign clients with Resilience4j
**Format:** JSON (internal DTOs, may differ from external)
**Auth:** Service-to-service mTLS (internal network)

**Internal OpenAPI specs location:** Per-service at `<service-root>/docs/openapi-internal.yaml`
- `services/rules-service/docs/openapi-internal.yaml`
- `services/ledger-service/docs/openapi-internal.yaml`
- `services/onboarding-service/docs/openapi-internal.yaml`
- `services/switch-adapter-service/docs/openapi-internal.yaml`
- `services/biller-service/docs/openapi-internal.yaml`

Internal APIs are NOT exposed through the Gateway.

---

## 4. Request & Response Payloads

### Withdrawal

**Request:**
```json
{
  "amount": 500.00,
  "card_data": "encrypted_card_blob",
  "pin_block": "encrypted_pin_block",
  "currency": "MYR"
}
```

**Response (Success):**
```json
{
  "status": "COMPLETED",
  "transaction_id": "TXN-uuid-123",
  "amount": 500.00,
  "customer_fee": 1.00,
  "reference_number": "PAYNET-REF-789",
  "timestamp": "2026-03-25T14:30:00+08:00"
}
```

**Response (Failed):**
```json
{
  "status": "FAILED",
  "error": {
    "code": "ERR_INSUFFICIENT_FLOAT",
    "message": "Agent float balance insufficient",
    "action_code": "DECLINE",
    "trace_id": "abc-123-def-456",
    "timestamp": "2026-03-25T14:30:00+08:00"
  }
}
```

### Deposit

**Request:**
```json
{
  "amount": 1000.00,
  "destination_account": "1234567890",
  "currency": "MYR"
}
```

**Response (Success):**
```json
{
  "status": "COMPLETED",
  "transaction_id": "TXN-uuid-456",
  "amount": 1000.00,
  "customer_fee": 1.00,
  "reference_number": "DEP-REF-012",
  "timestamp": "2026-03-25T14:35:00+08:00"
}
```

### Balance Inquiry (Customer)

**Request:**
```json
{
  "card_data": "encrypted_card_blob",
  "pin_block": "encrypted_pin_block"
}
```

**Response (Success):**
```json
{
  "status": "COMPLETED",
  "balance": 15000.00,
  "currency": "MYR",
  "account_masked": "****7890",
  "timestamp": "2026-03-25T14:40:00+08:00"
}
```

### Balance Inquiry (Agent)

**Request:**
```json
{}
```

**Response (Success):**
```json
{
  "status": "COMPLETED",
  "balance": 10000.00,
  "reserved_balance": 500.00,
  "available_balance": 9500.00,
  "currency": "MYR",
  "timestamp": "2026-03-25T14:40:00+08:00"
}
```

### e-KYC Verify

**Request:**
```json
{
  "mykad_number": "123456789012"
}
```

**Response (Success - Auto Approved):**
```json
{
  "status": "AUTO_APPROVED",
  "verification_id": "KYC-uuid-789",
  "full_name": "AHMAD BIN ABU",
  "age": 35,
  "timestamp": "2026-03-25T14:45:00+08:00"
}
```

**Response (Manual Review):**
```json
{
  "status": "MANUAL_REVIEW",
  "verification_id": "KYC-uuid-790",
  "reason": "Biometric match failed — queued for manual review",
  "timestamp": "2026-03-25T14:45:00+08:00"
}
```

### e-KYC Biometric

**Request:**
```json
{
  "verification_id": "KYC-uuid-789",
  "biometric_data": "encrypted_thumbprint_blob"
}
```

**Response (Success):**
```json
{
  "status": "AUTO_APPROVED",
  "verification_id": "KYC-uuid-789",
  "biometric_match": "MATCH",
  "timestamp": "2026-03-25T14:46:00+08:00"
}
```

### Common Response Envelope

**Success:**
```json
{
  "status": "COMPLETED",
  "transaction_id": "TXN-uuid",
  "timestamp": "2026-03-25T14:30:00+08:00",
  ...<service-specific fields>...
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
    "timestamp": "2026-03-25T14:30:00+08:00"
  }
}
```

### Field Notes

| Field | Source | Notes |
|-------|--------|-------|
| `card_data` | POS terminal | Encrypted at POS, decrypted only at HSM |
| `pin_block` | POS terminal | DUKPT encrypted, never decrypted outside HSM |
| `agent_id` | JWT token | Extracted by Gateway, never from request body |
| `trace_id` | Generated by Gateway | Propagated through all services via headers |
| `timestamp` | Server-side | ISO 8601 with timezone (+08:00 for MYT) |
| `x-idempotency-key` | Client | UUID v4, required for all mutation requests |

---

## 5. Error Handling & Security

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
| Validation | ERR_VAL_xxx | ERR_INVALID_AMOUNT, ERR_INVALID_MYKAD_FORMAT |
| Business | ERR_BIZ_xxx | ERR_LIMIT_EXCEEDED, ERR_INSUFFICIENT_FLOAT |
| External | ERR_EXT_xxx | ERR_SWITCH_DECLINED, ERR_KYC_SERVICE_UNAVAILABLE |
| Auth | ERR_AUTH_xxx | ERR_TOKEN_EXPIRED, ERR_MISSING_TOKEN |
| System | ERR_SYS_xxx | ERR_SERVICE_UNAVAILABLE, ERR_INTERNAL |

Each error code maps to a message template (localization-ready).

### Security Architecture

```
┌─────────────────────────────────────────────────────────┐
│  Security Layers                                         │
│                                                         │
│  Layer 1: Transport (TLS 1.2+)                          │
│  All external traffic encrypted                         │
│                                                         │
│  Layer 2: Authentication (JWT)                          │
│  Gateway validates token, extracts agentId              │
│  Internal: mTLS between services                        │
│                                                         │
│  Layer 3: Authorization (RBAC)                          │
│  Agent tier determines allowed operations               │
│  Backoffice: role-based (VIEWER, OPERATOR, ADMIN)       │
│                                                         │
│  Layer 4: Data Protection                               │
│  PIN: HSM encryption, never logged                      │
│  PAN: Masked (first 6, last 4)                          │
│  MyKad: Encrypted at rest (AES-256)                     │
│  PII: Never in logs, masked in responses                │
│                                                         │
│  Layer 5: Fraud Prevention                              │
│  Geofencing: 100m radius check                          │
│  Velocity: per-MyKad transaction limits                 │
│  EFM: Kafka events for real-time monitoring             │
└─────────────────────────────────────────────────────────┘
```

### Idempotency Flow

```
Request with X-Idempotency-Key
    │
    ▼
Check Redis cache for key
    │
    ├─── Found → Return cached response
    │
    └─── Not Found → Process transaction
                      │
                      ├─── Success → Cache response in Redis (TTL: 24h)
                      │                Return response
                      │
                      └─── Failure → Cache error in Redis (TTL: 24h)
                                      Return error
```

---

## 6. Backoffice UI

### Technology

React + TypeScript + Vite

### Architecture

```
Backoffice UI (React + TypeScript + Vite)
    │ REST calls to Gateway
    ▼
Spring Cloud Gateway
    │ Internal routing
    ▼
Backend Services (same Tier 3 services)
```

### UI Modules (MVP)

| Module | Pages | API Calls |
|--------|-------|-----------|
| **Dashboard** | Transaction overview, KPIs | GET /api/v1/backoffice/dashboard |
| **Agent Management** | List, Create, Edit, Deactivate agents | CRUD /api/v1/backoffice/agents |
| **Transaction Monitor** | Real-time transaction list, filters | GET /api/v1/backoffice/transactions |
| **Settlement** | Settlement reports, CSV export | GET /api/v1/backoffice/settlement |
| **e-KYC Review** | Manual review queue | GET /api/v1/backoffice/kyc/review-queue |

### UI Modules (Phase 2)

| Module | Pages |
|--------|-------|
| **Configuration** | Fee config, limits, velocity rules editor |
| **Audit Logs** | Search, filter, view audit trail |
| **Compliance** | EFM alerts, geofence violations, smurfing reports |
| **Analytics** | Transaction volume, agent performance, revenue |

### Backoffice Auth

- Separate JWT tokens (not shared with POS agents)
- Roles: VIEWER, OPERATOR, ADMIN
- Role-based page/component access

---

## 7. Technology Stack Summary

| Layer | Technology |
|-------|-----------|
| Language | Java 21 (LTS) |
| Framework | Spring Boot 3.x, Spring Cloud |
| Database | PostgreSQL (per service) |
| Caching | Redis (Spring Data Redis) |
| Messaging | Apache Kafka (Spring Cloud Stream) |
| Gateway | Spring Cloud Gateway (Reactive) |
| Service Discovery | Eureka (if needed, otherwise static config) |
| Inter-service | OpenFeign + Resilience4j |
| Testing | JUnit 5, Mockito, ArchUnit |
| DTOs | Java Records |
| Validation | jakarta.validation |
| Logging | SLF4J |
| Backoffice | React + TypeScript + Vite |
| Build | Gradle (multi-module) |
| Container | Docker |
| Orchestration | Kubernetes (future) |

---

## 8. Project Structure

```
agent-banking-platform/
├── docs/
│   ├── api/
│   │   └── openapi.yaml                    # External API spec (Gateway)
│   └── superpowers/
│       └── specs/
│           └── agent-banking-platform/
│               ├── *-brd.md
│               ├── *-bdd.md
│               └── *-design.md
├── services/
│   ├── rules-service/
│   │   ├── docs/openapi-internal.yaml
│   │   ├── src/main/java/.../domain/
│   │   ├── src/main/java/.../application/
│   │   ├── src/main/java/.../infrastructure/
│   │   └── build.gradle
│   ├── ledger-service/
│   ├── onboarding-service/
│   ├── switch-adapter-service/
│   └── biller-service/
├── gateway/
│   └── (Spring Cloud Gateway config)
├── backoffice/
│   ├── src/
│   ├── package.json
│   └── vite.config.ts
├── common/                                  # Shared module (error codes, DTOs)
│   └── src/main/java/.../common/
├── mock-server/                             # Downstream mock server (Tier 5)
│   ├── src/main/java/.../mock/
│   ├── src/main/resources/mock-data/
│   └── build.gradle
├── build.gradle                             # Root build
└── settings.gradle
```


## 9. Phased Implementation Roadmap

| Phase | Scope | Depends On |
|-------|-------|-----------|
| P0 | Mock Server (all downstream simulators) | None |
| P1 | Rules Service (fee engine, limits, velocity) | P0 |
| P2 | Ledger & Float Service (wallets, journals, real-time settlement) | P1 |
| P3 | Onboarding Service (e-KYC, MyKad, biometric) | P1 |
| P4 | Switch Adapter (ISO 8583, PayNet integration) | P1, P2 |
| P5 | Backoffice UI (agent mgmt, monitoring, config) | P1, P2, P3 |
| P6 | Biller Service + Phase 2 transactions | P1, P2, P4 |
| P7 | API Gateway hardening + OpenAPI finalization | All |
| P8 | Security hardening (EFM, geofencing, encryption audit) | All |

---

## 10. Downstream Mock Server

### Purpose

A standalone mock server simulating all Tier 5 downstream systems. Unblocks parallel development — frontend and backend teams can build and test while compliance handles sandbox key paperwork.

### Technology

Spring Boot 3.x (same stack as core services). Runs as a standalone service.

### Simulated Systems

| Mock Endpoint | Simulates | Behavior |
|---------------|-----------|----------|
| POST /mock/paynet/iso8583/auth | PayNet card authorization | Approve/decline based on card number pattern |
| POST /mock/paynet/iso8583/reversal | PayNet reversal (MTI 0400) | Always acknowledges |
| POST /mock/paynet/iso20022/transfer | PayNet DuitNow transfer | Approve, settle within simulated delay |
| POST /mock/jpn/verify | JPN MyKad verification | Returns mock citizen data based on MyKad number |
| POST /mock/jpn/biometric | JPN biometric match | Match/No-match based on input pattern |
| POST /mock/hsm/verify-pin | HSM PIN verification | Verifies DUKPT PIN block |
| POST /mock/hsm/generate-key | HSM key generation | Returns mock key bundle |
| POST /mock/billers/jompay/validate | JomPAY Ref-1 validation | Valid/invalid based on Ref-1 pattern |
| POST /mock/billers/jompay/pay | JomPAY payment | Approves payment |
| POST /mock/billers/astro/validate | ASTRO RPN validation | Mock bill lookup |
| POST /mock/billers/astro/pay | ASTRO payment | Approves payment |
| POST /mock/billers/tm/validate | TM RPN validation | Mock bill lookup |
| POST /mock/billers/tm/pay | TM payment | Approves payment |
| POST /mock/billers/epf/validate | EPF validation | Mock member lookup |
| POST /mock/billers/epf/pay | EPF payment | Approves payment |

### Configuration

The mock server supports configurable responses via `application-mock.yaml`:

```yaml
mock:
  paynet:
    default-response: APPROVE          # or DECLINE
    decline-codes: ["51", "54", "55"]  # ISO 8583 decline codes
    latency-ms: 200                    # Simulated network delay
  jpn:
    default-match: MATCH               # or NO_MATCH
    aml-default: CLEAN                 # or FLAGGED
  hsm:
    pin-validation: VALID              # or INVALID
  billers:
    default-validation: VALID
    latency-ms: 500
```

### Switching Between Mock and Real

Each Translation Layer adapter reads a config flag:

```yaml
# application.yaml in each service
adapter:
  paynet:
    enabled: mock                      # or live
    mock-base-url: http://localhost:8090/mock/paynet
    live-base-url: https://api.paynet.com.my
  jpn:
    enabled: mock
    mock-base-url: http://localhost:8090/mock/jpn
```

### Test Data Seeding

The mock server loads test fixtures on startup:
- `mock-data/citizens.json` — 100 sample MyKad records
- `mock-data/agents.json` — 20 sample agents (Micro/Standard/Premier)
- `mock-data/billers.json` — Biller reference data

### Docker Compose Integration

```yaml
services:
  mock-server:
    build: ./mock-server
    ports:
      - "8090:8090"
    profiles: ["dev", "test"]
```
