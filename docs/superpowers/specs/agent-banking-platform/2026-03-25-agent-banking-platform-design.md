# Technical Design Specification
## Agent Banking Platform (Malaysia)

**Version:** 1.1
**Date:** 2026-03-26
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
| **Rules** | FeeConfig, VelocityRule, limit checks | rules_db | GET /fees, GET /limits, POST /check-velocity, GET /config | — |
| **Ledger & Float** | AgentFloat, Transaction, JournalEntry, SettlementSummary | ledger_db | POST /block-float, POST /commit, POST /rollback, POST /credit, GET /balance, POST /reverse | — |
| **Onboarding** | KycVerification, agent/customer records | onboarding_db | POST /verify-mykad, POST /biometric-match, POST /activate-agent | — |
| **Switch Adapter** | ISO 8583/20022 routing, PayNet integration | switch_db | POST /auth, POST /reversal, POST /reversal-saf | — |
| **Biller** | BillerConfig, biller webhooks | biller_db | POST /validate-ref, POST /pay-bill, POST /notify-biller | — |
| **Transaction Orchestrator** | Saga coordination, error handling | orchestrator_db | POST /execute-saga, POST /compensate | — |
| **Gateway** | — | — | — | All external POS endpoints |

### Detailed Service Interfaces

#### Onboarding Service
| Interface Type | Connected System | Protocol | Data Exchanged |
|---------------|-----------------|----------|----------------|
| **Internal** | Rules & Parameter Service | Feign (Sync) | **Req:** Agent Type, Location. **Res:** KYC required fields, Age limits |
| **Internal** | Transaction Orchestrator | Kafka (Async) | **Event:** `AGENT_READY`. **Payload:** Agent UUID, Default Tier |
| **External** | **JPN (National ID)** | SOAP/REST | **Req:** NRIC, Biometric Template. **Res:** Match Score, Full Name, Photo |
| **External** | **SSM (Business Reg)** | REST | **Req:** SSM Number. **Res:** Registration Status, Director Names |
| **Downstream** | **Core Banking (CBS)** | REST/MQ | **Req:** NRIC. **Res:** Existing Customer Info, Internal Risk Rating |

#### Ledger & Float Service
| Interface Type | Connected System | Protocol | Data Exchanged |
|---------------|-----------------|----------|----------------|
| **Internal (Peer)** | Transaction Orchestrator | Feign (Sync) | **Req/Res:** Float Block/Commit commands (JSON) |
| **Internal** | Rules Service | Feign (Sync) | **Req:** Agent ID, Txn Amount. **Res:** Velocity Check Result |
| **Downstream (Tier 3)** | CBS Connector | REST / MQ | **Req:** Real-time Balance Inquiry. **Res:** Account Status (JSON) |
| **Downstream (Tier 3)** | Batch File Generator | Local File System | **Outbound:** Raw Transaction CSV for EOD Settlement |

#### Switch Adapter Service
| Interface Type | Connected System | Protocol | Data Exchanged |
|---------------|-----------------|----------|----------------|
| **Internal (Peer)** | Transaction Orchestrator | Feign (Sync) | **Req:** Txn Data (Amount, PAN, PIN). **Res:** Network Approval/Decline |
| **Downstream (Tier 3)** | ISO Translation Engine | gRPC / REST | **Req:** Transaction JSON. **Res:** Decoded ISO Response (JSON) |

#### Biller Service
| Interface Type | Connected System | Protocol | Data Exchanged |
|---------------|-----------------|----------|----------------|
| **Internal** | Transaction Orchestrator | Feign (Sync) | **Req:** Biller Code, Ref-1. **Res:** Validation, Biller Name, Balance |
| **Internal** | Rules Service | Feign (Sync) | **Req:** Biller ID. **Res:** Convenience Fee |
| **External** | **JomPAY (PayNet)** | REST / XML | **Req:** Bill Account Number (Ref-1). **Res:** Validation Status |
| **External** | **Fiuu / Aggregators** | REST API | **Req:** Product ID. **Res:** 16-digit PIN code or Instant Top-up status |

#### Rules & Parameter Service
| Interface Type | Connected System | Protocol | Data Exchanged |
|---------------|-----------------|----------|----------------|
| **Internal** | All Services | Feign (Sync) | **Req:** Parameter Key. **Res:** Value |
| **Internal** | Admin Dashboard | REST (Inbound) | **Req:** Update Fee. **Res:** Update Success, Audit Log ID |

#### Transaction Orchestrator
| Interface Type | Connected System | Protocol | Data Exchanged |
|---------------|-----------------|----------|----------------|
| **Internal** | Ledger, Switch, Biller | Feign (Sync) | **Outbound:** Commands to execute parts of flow |
| **External** | **Notification Gateway** | REST API | **Req:** Mobile Number, Message Template. **Res:** SMS Delivery Status |

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
| POST | /api/v1/withdrawal | Ledger | Cash withdrawal (EMV + PIN or MyKad) |
| POST | /api/v1/deposit | Ledger | Cash deposit |
| POST | /api/v1/balance-inquiry | Ledger | Customer or agent balance |
| POST | /api/v1/kyc/verify | Onboarding | MyKad verification |
| POST | /api/v1/kyc/biometric | Onboarding | Biometric match |
| POST | /api/v1/bill/pay | Biller | Bill payment (JomPAY, ASTRO, TM, EPF) |
| POST | /api/v1/topup | Biller | Prepaid top-up (CELCOM, M1) |
| POST | /api/v1/transfer/duitnow | Switch | DuitNow transfer |
| POST | /api/v1/retail/sale | Merchant | Retail card/QR purchase (MDR applies) |
| POST | /api/v1/retail/pin-purchase | Merchant | PIN voucher purchase (commission) |
| POST | /api/v1/retail/cashback | Merchant | Hybrid purchase + cash withdrawal |
| POST | /api/v1/ewallet/withdraw | Biller | e-Wallet withdrawal |
| POST | /api/v1/ewallet/topup | Biller | e-Wallet top-up |
| POST | /api/v1/essp/purchase | Biller | eSSP certificate purchase |
| GET | /api/v1/agent/balance | Ledger | Agent wallet balance |

**Backoffice Endpoints (via separate route):**

| Method | Path | Service | Description |
|--------|------|---------|-------------|
| GET | /api/v1/backoffice/agents | Onboarding | List agents |
| POST | /api/v1/backoffice/agents | Onboarding | Create agent |
| PUT | /api/v1/backoffice/agents/{id} | Onboarding | Update agent |
| DELETE | /api/v1/backoffice/agents/{id} | Onboarding | Deactivate agent |
| GET | /api/v1/backoffice/transactions | Ledger | Transaction monitoring |
| GET | /api/v1/backoffice/settlement | Ledger | Settlement reports |
| GET | /api/v1/backoffice/kyc/review-queue | Onboarding | Manual review queue |
| POST | /api/v1/backoffice/discrepancy/{id}/maker-action | Reconciliation | Maker proposes adjustment |
| POST | /api/v1/backoffice/discrepancy/{id}/checker-approve | Reconciliation | Checker approves/rejects |
| GET | /api/v1/backoffice/audit-logs | Audit | Audit log viewer |

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

### External Error Mapping (Tier 3 → Tier 2)

The Translation Layer (Tier 3) normalizes legacy error codes to clean business errors:

| Legacy Source | External Code | Legacy Description | Business Error | Action |
|--------------|--------------|-------------------|---------------|--------|
| ISO 8583 | 00 | Approved | SUCCESS | Finalize |
| ISO 8583 | 51 | Insufficient Funds | ERR_INSUFFICIENT_FUNDS | Notify Customer |
| ISO 8583 | 05 | Do Not Honor | ERR_DECLINED_BY_ISSUER | Notify Customer |
| ISO 8583 | 13 | Invalid Amount | ERR_INVALID_TRANSACTION | Stop / Alert |
| ISO 8583 | 91 | Issuer/Switch Inoperative | ERR_NETWORK_TIMEOUT | Trigger Reversal |
| ISO 20022 | AB05 | Timeout at Clearing | ERR_NETWORK_TIMEOUT | Trigger Reversal |
| ISO 20022 | AC04 | Closed Account | ERR_ACCOUNT_INACTIVE | Notify Customer |
| CBS | E102 | Hold on Account | ERR_ACCOUNT_FROZEN | Notify Customer |
| CBS | E999 | System Error | ERR_DOWNSTREAM_UNAVAILABLE | Retry / Alert |
| HSM | 15 | PIN Block Mismatch | ERR_INVALID_PIN | Block / Security Alert |

**Action Categories:**
- **Notify Customer:** Clean failure — stop and tell customer why
- **Trigger Reversal:** Technical failure — call Rollback to release float
- **Stop / Alert:** Potential fraud — block terminal, alert security team

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

---

## 11. Tier 3 Translation Layer Detail

### Overview

The Translation Layer (Tier 3) exists to protect clean, modern Tier 2 services from the messy, legacy protocols of external systems. It handles "dirty work" so Tier 2 only speaks JSON.

### 11.1 ISO Translation Engine

**Purpose:** Transform internal JSON to ISO 8583 (card) and ISO 20022 (DuitNow) binary/XML formats.

**Responsibilities:**
- Marshal JSON into binary bitmaps (ISO 8583) or XML (ISO 20022)
- Manage persistent TCP/IP socket connections with PayNet
- Send Echo/Heartbeat messages every 30-60 seconds
- Unmarshal binary responses back to clean JSON
- Generate and track System Trace Audit Number (STAN) incrementally (000001-999999)

**Interface:**
| Direction | Protocol | Data |
|-----------|----------|------|
| Inbound (Tier 2) | gRPC/REST | Canonical JSON |
| Outbound (Tier 4) | ISO 8583/20022 | Binary bitmaps |

### 11.2 CBS Connector

**Purpose:** Shield Tier 2 from Core Banking System legacy interfaces (SOAP, MQ, fixed-length strings).

**Responsibilities:**
- Wrap JSON data into SOAP envelopes or MQ message headers
- Manage Request/Reply Queue orchestration
- Handle long timeouts without blocking Tier 2 threads
- Convert settlement JSON to the specific "Flat-File" format for CBS batch engine

**Interface:**
| Direction | Protocol | Data |
|-----------|----------|------|
| Inbound (Tier 2) | REST/JSON | Settlement JSON, Account Inquiry JSON |
| Outbound (Tier 4) | SOAP/MQ/Fixed-Length | Legacy XML/Fixed-Format |

### 11.3 HSM Wrapper

**Purpose:** Handle all communication with the Hardware Security Module. Tier 2 never sees a raw PIN or clear-text encryption key.

**Responsibilities:**
- Format proprietary HSM commands (Thales "CA"/"BA" commands)
- Translate PIN blocks from Zone Personal Key (ZPK) to Local Master Key (LMK)
- Verify PIN blocks
- Isolate encryption keys (reside only in HSM, never in Tier 2 memory)

**Interface:**
| Direction | Protocol | Data |
|-----------|----------|------|
| Inbound (Tier 2/ISO Engine) | gRPC/REST | Encrypted PIN Block (ZPK) |
| Outbound (Tier 4) | TCP Socket (Proprietary) | Thales HSM commands |

### 11.4 Biller Gateway

**Purpose:** Provide unified interface for multiple biller providers (TNB, Astro, JomPAY, Fiuu) who use different API standards.

**Responsibilities:**
- Route requests to correct 3rd party URL based on Biller_ID
- Inject 3rd party API keys and OAuth tokens automatically
- Use Adapter Pattern to convert internal JSON to biller-specific XML/REST
- Ensure idempotency (prevent double-charging biller on retry)

**Interface:**
| Direction | Protocol | Data |
|-----------|----------|------|
| Inbound (Tier 2) | gRPC/REST | Generic Bill Payment JSON |
| Outbound (Tier 4) | Varies (XML, REST) | Biller-specific payload |

### 11.5 Tier 2 → Tier 3 Interaction Summary

| Tier 2 Service | Calls | Tier 3 Service | Data Exchanged |
|---------------|-------|---------------|----------------|
| Switch Adapter | → | ISO Translation Engine | JSON (Logic) ↔ Binary/ISO (Network) |
| Ledger Service | → | CBS Connector | JSON (Sub-ledger) ↔ SOAP/MQ (Mainframe) |
| Biller Service | → | Biller Gateway | JSON (Unified) ↔ XML/REST (Multiple Billers) |
| ISO Engine | → | HSM Wrapper | PIN Block (ZPK) ↔ PIN Block (LMK) |

---

## 12. Transaction Orchestrator (Saga Pattern)

### Overview

The Orchestrator is the "Conductor" of the multi-step financial flow. It coordinates the Saga: Reserve Float → Switch Auth → Commit Ledger.

### 12.1 ExecuteSaga Flow

```
POS Request
    │
    ▼
┌─────────────────────────────────────────────┐
│ Transaction Orchestrator                     │
│                                              │
│ Step 1: Validate Idempotency Key (Redis)    │
│         → Return cached if found            │
│                                              │
│ Step 2: Call Rules Service                   │
│         → Check velocity, limits, fees      │
│                                              │
│ Step 3: Call Ledger Service                  │
│         → BlockFloat (PESSIMISTIC_WRITE)    │
│         → RESERVE the float amount          │
│                                              │
│ Step 4: Call Switch Adapter                  │
│         → Authorize at PayNet               │
│         → Get approval/decline              │
│                                              │
│ Step 5: If Switch SUCCESS:                   │
│         → Call Ledger Service               │
│         → CommitTransaction                 │
│         → Update actual_balance             │
│         → Post journal entries              │
│                                              │
│ Step 6: If Switch FAILED:                   │
│         → Call Ledger Service               │
│         → RollbackTransaction               │
│         → Release reserved float            │
│                                              │
│ Step 7: Publish Kafka events                 │
│         → SMS notification                  │
│         → Commission accrual                │
│         → EFM event (if applicable)         │
│                                              │
│ Step 8: Cache response in Redis (TTL: 24h) │
│                                              │
│ Step 9: Return response to Gateway           │
└─────────────────────────────────────────────┘
```

### 12.2 Compensate (Error Recovery)

**Purpose:** Ensure the system is "clean" if a mid-way failure occurs.

**Trigger:** Any exception or timeout during a saga step.

**Processing:**
1. Identify which step failed
2. Trigger Reverse/Undo on all previously completed steps
3. Ensure no "Zombie" locks remain on AgentFloat
4. Log the compensation action in AuditLog

### 12.3 Resilience Configuration

| Inter-service Call | Timeout | Retry | Circuit Breaker |
|-------------------|---------|-------|-----------------|
| Rules Service | 5s | 3x exponential | 50% failure → open |
| Ledger Service | 5s | 0x (financial) | 50% failure → open |
| Switch Adapter | 25s | 0x (financial) | Timeout → trigger reversal |
| Biller Service | 10s | 3x exponential | 50% failure → open |

---

## 13. Error Mapping Table

### Legacy to Business Core Mapping

| Legacy Source | External Code | Legacy Description | Business Tier 2 Error | Action Category |
|--------------|--------------|-------------------|----------------------|----------------|
| ISO 8583 | 00 | Approved or Completed | SUCCESS | Finalize |
| ISO 8583 | 51 | Insufficient Funds | INSUFFICIENT_FUNDS | Notify Customer |
| ISO 8583 | 05 | Do Not Honor (Generic) | DECLINED_BY_ISSUER | Notify Customer |
| ISO 8583 | 13 | Invalid Amount | INVALID_TRANSACTION | Stop / Alert |
| ISO 8583 | 91 | Issuer or Switch Inoperative | NETWORK_TIMEOUT | Trigger Reversal |
| ISO 20022 | AB05 | Timeout at Clearing | NETWORK_TIMEOUT | Trigger Reversal |
| ISO 20022 | AC04 | Closed Account | ACCOUNT_INACTIVE | Notify Customer |
| CBS (Core) | E102 | Hold on Account | ACCOUNT_FROZEN | Notify Customer |
| CBS (Core) | E999 | System Error / DB Down | DOWNSTREAM_UNAVAILABLE | Retry / Alert |
| HSM | 15 | PIN Block Mismatch | INVALID_PIN | Block / Security Alert |

### Action Categories

- **Notify Customer:** Clean failures. Stop transaction and tell customer why.
- **Trigger Reversal:** Technical failures. Call Rollback to release float lock.
- **Stop / Alert:** Potential fraud or major configuration error. Block terminal and alert security team.

### Error Object Contract

When Tier 3 sends an error to Tier 2:
```json
{
  "status": "FAILED",
  "business_error": {
    "code": "INSUFFICIENT_FUNDS",
    "message": "The customer does not have enough balance.",
    "action": "NOTIFY_CUSTOMER"
  },
  "legacy_context": {
    "source": "PAYNET_ISO_8583",
    "raw_code": "51",
    "trace_id": "99283741"
  }
}
```

---

## 14. Retry & Reversal Policy

### 14.1 The 30-Second Rule (Timeout Policy)

| Stage | Timeout Limit | Action on Timeout |
|-------|--------------|-------------------|
| Tier 2 → Tier 3 | 25 Seconds | Initiate Automatic Reversal |
| Tier 3 → Tier 4 | 20 Seconds | Tier 3 sends NETWORK_TIMEOUT to Tier 2 |
| Database Lock | 5 Seconds | Fail transaction immediately |

### 14.2 Retry Logic

- **Non-Financial (Echo, Inquiry):** 3 retries with exponential backoff (1s, 2s, 4s). Idempotent — requesting twice doesn't move money.
- **Financial (Withdrawal, Bill Pay):** ZERO retries at Orchestrator level. If timeout → assume money might have moved → trigger Reversal Flow immediately.

### 14.3 Safety Reversal Flow (MTI 0400)

If Tier 2 hits 25-second timeout:
1. Mark transaction_history as REVERSAL_INITIATED
2. Call Tier 3 ISO Engine with REVERSAL_REQUEST (ISO MTI 0400)
3. Release virtual float via Ledger Service Rollback
4. Notify POS terminal: "Transaction Timeout. Funds Reversed."

### 14.4 Reversal Persistence (Store & Forward)

- Failed reversal messages persisted in Persistent Queue (Redis/PostgreSQL)
- Background worker retries every 60 seconds until SUCCESS from Switch
- Every attempt logged in reversal_audit table

---

## 15. EOD Net Settlement Architecture

### 15.1 Settlement Formula

```
Net Settlement = (Total Withdrawals + Total Commissions + Total Retail Sales)
               - (Total Deposits + Total Bill Payments)
```

- **Positive result:** Bank owes Agent (Direct Credit to Agent's Bank Account)
- **Negative result:** Agent owes Bank (Direct Debit from Agent's Bank Account)

### 15.2 Transaction Impact on Settlement

| Transaction Type | Float Impact | Net Settlement Effect |
|-----------------|-------------|----------------------|
| Cash Withdrawal | Agent gives cash → Float increases (+) | Bank owes Agent |
| Cash Deposit | Agent collects cash → Float decreases (-) | Agent owes Bank |
| Bill Payment | Agent collects cash → Float decreases (-) | Agent owes Bank |
| Retail Sale | No cash, digital → Float increases (+) | Bank owes Agent (net of MDR) |
| Commission Earned | None | Bank owes Agent |
| Float Top-up | Prefunding | Excluded from daily net (settled real-time) |
| Reversals/Voids | Float restored | Neutral (must sum to zero) |

### 15.3 EOD Batch Job Sequence

```
23:59:59 MYT
    │
    ▼
┌─────────────────────────────────────────────┐
│ Step 1: Data Harvest                         │
│ - Extract PayNet PSR file via SFTP           │
│ - Extract internal ledger for business date  │
│                                              │
│ Step 2: Reconciliation (Triple-Match)        │
│ - Match Internal Ledger ↔ PayNet PSR         │
│ - Categorize: Ghost/Orphan/Mismatch          │
│ - If discrepancies → HOLD settlement         │
│                                              │
│ Step 3: Settlement Calculation               │
│ - Aggregate SUCCESS transactions per agent   │
│ - Calculate net settlement amount            │
│ - Determine direction (BANK_OWES/AGENT_OWES) │
│                                              │
│ Step 4: CBS File Generation                  │
│ - Generate CSV/flat file per agent           │
│ - Place at /sftp/cbs/outbound/               │
│ - Target: 02:00 AM                           │
│                                              │
│ Step 5: Commission Accrual                   │
│ - Calculate daily commission per agent       │
│ - Post to agent_earnings sub-ledger          │
│ - Include in settlement file                 │
└─────────────────────────────────────────────┘
```

### 15.4 Settlement Status Flow

```
PENDING → HELD (if discrepancy) → SETTLED
                ↓
            DISPUTED → RESOLVED → SETTLED
```

---

## 16. Reconciliation Architecture

### 16.1 Triple-Match Logic

For a transaction to be "Reconciled," it must appear identically in three places:
1. **Internal Ledger:** What Spring Boot services recorded
2. **Terminal Journal:** What the physical POS machine says
3. **Network Statement (PSR):** What PayNet says it cleared

**Hierarchy of Truth:** Network Statement (PSR) is the "Final Word" because it represents actual interbank movement.

### 16.2 Validation Rules

| Rule | Description |
|------|-------------|
| Integrity Key Match | Transaction ID and Network Reference must match across all reports |
| Status Alignment | Internal=SUCCESS but PayNet=REVERSED → "Broken" → follow PayNet |
| Amount Zero-Tolerance | RM 100.00 vs RM 100.01 → entire batch halted |
| Cut-off Synchronization | Transactions at 11:59:59 PM validated against correct business day |

### 16.3 Discrepancy Categories

| Category | Definition | Potential Cause | Business Action |
|----------|-----------|----------------|-----------------|
| Ghost | Successful in Internal; Missing in PayNet | Network timeout after local success | Force Reverse: Recover float |
| Orphan | Missing in Internal; Successful in PayNet | Network timeout after switch approval | Force Success: Credit float |
| Mismatch | Amounts differ | Rounding error or corruption | Suspend Settlement: Manual audit |

---

## 17. Discrepancy Resolution Architecture

### 17.1 Maker-Checker Workflow

```
Reconciliation identifies discrepancy
    │
    ▼
┌─────────────────────────────────────────────┐
│ PENDING_MAKER                                │
│ - Maker reviews terminal journal             │
│ - Maker compares with PayNet PSR             │
│ - Maker selects action:                      │
│   FORCE_SUCCESS / FORCE_REVERSE / DUPLICATE  │
│ - Maker attaches reason code + evidence      │
│ - Maker submits                              │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│ PENDING_CHECKER                              │
│ - Checker reviews evidence                   │
│ - Checker verifies Maker != Checker          │
│ - Checker Approves or Rejects                │
└──────────────────┬──────────────────────────┘
                   │
        ┌──────────┴──────────┐
        ▼                     ▼
    APPROVED              REJECTED
        │                     │
        ▼                     ▼
    Ledger adjustment     Return to Maker
    ManualAdjustmentEntry with comment
    DiscrepancyCase = RESOLVED
```

### 17.2 Four-Eyes Principle (System Enforcement)

- Maker and Checker must be different user IDs
- Enforced at API level (self-approval returns ERR_SELF_APPROVAL_PROHIBITED)
- Enforced at RBAC level (separate MAKER and CHECKER roles)

### 17.3 Financial Integrity Rule

- Original broken transaction is NEVER deleted
- Resolution creates a secondary ManualAdjustmentEntry for perfect audit trail
- PayNet PSR is the ultimate "Source of Truth"

---

## 18. STP Architecture

### 18.1 STP Categories

| Category | Scope | Human Intervention | Mechanism |
|----------|-------|-------------------|-----------|
| 100% STP | Bill pay, top-up, cash-out (under limits), fund transfers | Zero | Cryptographic proof (PIN, biometric) |
| Conditional STP | Onboarding, micro-agent approval, float replenishment | Fallback to manual | Rules engine (data intelligence) |
| Non-STP | Super-agent onboarding, disputes, AML alerts, limit overrides | Mandatory | Maker-Checker (human judgment) |

### 18.2 100% STP Flow

```
Transaction initiated
    │
    ▼
┌─────────────────────────────────────────────┐
│ Pre-funded Float Verification                │
│ → Agent's available float must cover txn     │
│ → If insufficient: HARD DECLINE              │
│                                              │
│ Dual-Handshake Authentication                │
│ → Customer PIN (HSM encrypted) or Biometric  │
│ → Agent identity (device/OTP)                │
│ → Neither party can authorize for other      │
│                                              │
│ Velocity & Volume Limits                     │
│ → Hardcoded API limits at Gateway level      │
│ → Max RM 3,000/txn, 5 txns/hour/customer    │
│                                              │
│ Execution                                    │
│ → Deduct float, credit biller/switch         │
│ → SMS receipt for non-repudiation            │
└─────────────────────────────────────────────┘
```

### 18.3 Conditional STP Flow

```
Application submitted (e.g., micro-agent onboarding)
    │
    ▼
┌─────────────────────────────────────────────┐
│ Data Capture                                 │
│ → OCR extraction from MyKad/SSM              │
│ → Facial liveness check                      │
│                                              │
│ Concurrent API Verification                  │
│ → JPN identity check                         │
│ → SSM business registry                      │
│ → AML watchlists (LexisNexis, Dow Jones)     │
│                                              │
│ Rules Engine Decision Matrix                 │
│ → Score each parameter                       │
│ → All pass → AUTO_APPROVED (STP)             │
│ → Any fail → Graceful degradation            │
│   → Route to manual review queue             │
│   → NOT auto-reject (customer-friendly)      │
│                                              │
│ Post-Approval Monitoring                     │
│ → 30-day probationary tier                   │
│ → Enhanced velocity monitoring               │
│ → Catch synthetic identity fraud             │
└─────────────────────────────────────────────┘
```

### 18.4 Non-STP Flow (Maker-Checker)

```
Trigger (super-agent app, dispute, AML alert, limit override)
    │
    ▼
┌─────────────────────────────────────────────┐
│ Maker (Field Officer)                        │
│ → Reviews physical evidence                  │
│ → Visits shop, interviews owner              │
│ → Gathers physical signatures                │
│ → Submits application with evidence          │
│                                              │
│ Checker (Compliance/Risk Officer)            │
│ → Reviews Maker's notes                      │
│ → Reviews system risk flags                  │
│ → Approves, Rejects, or Unfreezes            │
│                                              │
│ RBAC Enforcement                             │
│ → Maker ≠ Checker (system-enforced)          │
│ → Every click, note, decision logged         │
│ → Immutable audit trail with timestamps      │
│                                              │
│ SLA Queues                                   │
│ → 24-hour SLA on manual queues               │
│ → Auto-escalate to supervisor on timeout     │
└─────────────────────────────────────────────┘
```

### 18.5 Geofencing Guardrails

- POS GPS coordinates pinged with every transaction
- If terminal > 100m from registered shop → auto-decline + lock terminal
- If GPS unavailable → reject with ERR_GPS_UNAVAILABLE

### 18.6 Anti-Smurfing (Structuring Detection)

- Velocity engine tracks transaction frequency
- Pattern detection: multiple transactions just below limit flag
- Example: 10 x RM 2,900 deposits in 5 minutes (avoiding RM 3,000 flag)
- Detection → instant freeze + compliance alert
