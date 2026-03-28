# AGENTS.md - Guidelines for Agentic Coding Agents

This document provides instructions and guidelines for AI coding agents working in this repository.

## Project Overview

This project is an **Agent Banking Platform** facilitating financial services at third-party retail locations.
* **Regulatory Compliance:** Bank Malaysia standards.
* **Security:** Zero-trust architecture. No PII in logs. Hardware-level encryption for PINs.

## Architecture

### 5-Tier System Architecture

All agents MUST understand this architecture before making any code changes:

1. **Tier 1: Channel Layer** — POS Terminals (Android/Flutter)
2. **Tier 2: Spring Cloud Gateway** — JWT validation, rate limiting, routing
3. **Tier 3: Domain Core Services** — Rules, Ledger & Float, Onboarding, Switch Adapter, Biller
4. **Tier 4: Translation Layer** — HSM Connector, Switch Connector, Biller Connector
5. **Tier 5: Downstream Systems** — HSM, PayNet, JPN, Billers

See `docs/superpowers/specs/agent-banking-platform/*-design.md` for full architecture details.

### Hexagonal Architecture (MANDATORY per service)

Every microservice MUST follow hexagonal (Ports & Adapters) pattern:

```
service-name/
├── domain/                    # ZERO framework imports
│   ├── model/                 # Entities, value objects (Java Records)
│   ├── port/
│   │   ├── in/                # Inbound ports (use cases)
│   │   └── out/               # Outbound ports (repository, gateway, messaging)
│   └── service/               # Business rules
├── application/               # Use case orchestration
├── infrastructure/            # Adapters (implement ports)
│   ├── web/                   # REST controllers
│   ├── persistence/           # JPA repositories
│   ├── messaging/             # Kafka producers/consumers
│   └── external/              # Feign clients
└── config/                    # Spring configuration
```

**ENFORCEMENT:**
- `domain/` must have ZERO imports from Spring, JPA, Kafka, or any infrastructure framework
- `infrastructure/` implements interfaces defined in `domain/port/`
- Controllers accept DTOs, call use cases, return DTOs — NEVER expose entities
- All financial calculations and state changes in `domain/service/`

### Hexagonal Architecture Enforcement (REQUIRED)

- Service: MUST include ArchUnit tests that verify hexagonal architecture compliance:
- Domain model: you MUST
1. Follow the pattern in the template exactly - records go in `domain/model`, entities in `infrastructure/persistence/entity/`
2. Create repository port in `domain/port/out/` and implementation in `infrastructure/persistence/repository/`

**Template:** See `docs/templates/domain-model-template.md` for correct pattern

**Common mistakes that will fail the build:**
- ❌ Adding `@Entity` or `@Table` in `domain/model/`
- ❌ Using `EntityManager` directly in domain service
- ❌ Skipping the record/entity separation

**If unsure, check the template first.**

**FAILURE TO COMPLY:** Any JPA/Spring annotation found in `domain/` layer will cause the build to fail.

## Technology Stack

**MUST NOT use technologies outside this list:**
* **Language:** Java 21 (LTS)
* **Framework:** Spring Boot 3.x, Spring Cloud
* **Persistence:** Spring Data JPA (Hibernate) with PostgreSQL
* **Caching:** Redis (Spring Data Redis)
* **Messaging:** Apache Kafka (Spring Cloud Stream)
* **Gateway:** Spring Cloud Gateway (Reactive)
* **Testing:** JUnit 5, Mockito, ArchUnit
* **Backoffice UI:** React + TypeScript + Vite

## Architectural Laws (NON-NEGOTIABLE)

### Law I: Layered Architecture
Each microservice must follow: Controller → Service → Repository.
* **DTOs:** Controllers must only accept and return DTOs, never Entities.
* **Logic Location:** All financial calculations and state changes must reside in the `@Service` layer.

### Law II: Transactional Integrity
* All financial methods must be marked `@Transactional`.
* **Ledger Updates:** Must use `PESSIMISTIC_WRITE` locks on the `AgentFloat` entity.
* **Idempotency:** Every transaction request must check the `X-Idempotency-Key` before processing. Cache responses in Redis (TTL: 24h).

### Law III: Error Handling
**MUST use the Global Error Schema.** Never return a raw Exception or generic 500.
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
* Error codes MUST come from the centralized error code registry (shared common module).
* Categories: `ERR_VAL_xxx` (validation), `ERR_BIZ_xxx` (business), `ERR_EXT_xxx` (external), `ERR_AUTH_xxx` (auth), `ERR_SYS_xxx` (system).

### Law IV: Inter-service Communication
* **Synchronous:** Use `Spring Cloud OpenFeign` with Resilience4j circuit breakers.
* **Asynchronous:** Use Kafka (Spring Cloud Stream) for non-critical flows (SMS, Commission, EFM).
* **Database-per-service:** No shared databases. No cross-service joins.
* **Internal OpenAPI specs:** Per-service at `<service-root>/docs/openapi-internal.yaml`.

## Coding Standards

### Immutability
* Use Java Records for DTOs where possible.

### Validation
* Use `jakarta.validation` (`@NotNull`, `@Positive`, etc.) on ALL incoming DTOs.

### Logging
* Use SLF4J with `log.info` for lifecycle and `log.error` for failures.
* **NEVER log:**
  - Card numbers (PAN) — mask as `411111******1111`
  - MyKad numbers — encrypted at rest, never in logs
  - PIN blocks — NEVER log, never decrypt outside HSM
  - Any PII in plaintext

### Database
* One PostgreSQL database per microservice (database-per-service pattern).
* Use Flyway for migrations.
* No cross-service database access.

## API Contract Enforcement

**OpenAPI 3.0 Specification** is the single source of truth for all REST APIs.

### Rules
- **External API:** All backend REST endpoints exposed via Gateway MUST be documented in `docs/api/openapi.yaml`
- **Internal API:** Each service's internal endpoints documented in `<service-root>/docs/openapi-internal.yaml`
- **Frontend API clients and TypeScript types** MUST be generated from `openapi.yaml`
- **No manual hand-written API mocks** — use generated mocks from OpenAPI spec
- **CI validation**: Run `openapi-generator-cli validate` and diff check

## Documentation
- `docs` - at project root
- `docs/ideas` - high level requirements (ARCHITECTURE.md, BRD_SUMMARY.md)
- `docs/superpowers/specs/agent-banking-platform/` - formal specs (BRD, BDD, Design)
- `docs/api/openapi.yaml` - external API spec

## Testing Guidelines
* Unit tests: JUnit 5 + Mockito
* Architecture tests: ArchUnit (enforce hexagonal rules)
* Integration tests: Spring Boot Test + Testcontainers (PostgreSQL)
* BDD scenarios in `*-bdd.md` are the acceptance criteria

## Banking-Specific Guidelines

### Money Handling
* All monetary values use `BigDecimal` — NEVER use `float` or `double`.
* Rounding: `HALF_UP` to 2 decimal places.
* Currency: Always `MYR` — validate on all endpoints.

### Audit Trail
* Every financial transaction creates a JournalEntry (double-entry).
* AuditLog entity records who, what, when, where for all operations.
* Audit logs are immutable — append-only, no updates or deletes.

### Security
* PINs: Hardware-level encryption via HSM. DUKPT PIN blocks. Never decrypted outside HSM.
* PAN: Masked in all responses and logs (first 6, last 4 digits).
* MyKad: Encrypted at rest (AES-256). Never in plaintext logs.
* TLS 1.2+ for all external traffic.
* mTLS for internal service-to-service communication.

### Geofencing
* Transactions allowed only within 100m of registered Merchant GPS coordinate.
* If GPS unavailable: reject transaction with `ERR_GPS_UNAVAILABLE`.

### Velocity Checks
* Limit transactions per MyKad per day to prevent smurfing.
* Configurable via VelocityRule entity.