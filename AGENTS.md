# AGENTS.md - Guidelines for Agentic Coding Agents

This document provides instructions and guidelines for AI coding agents working in this repository.

## Project Overview

This project is an **Agent Banking Platform** facilitating financial services at third-party retail locations.
* **Regulatory Compliance:** Bank Malaysia standards.
* **Security:** Zero-trust architecture. No PII in logs. Hardware-level encryption for PINs.

## External File Loading

CRITICAL: When you encounter a file reference (e.g., @rules/general.md), use your Read tool to load it on a need-to-know basis. They're relevant to the SPECIFIC task at hand.

Instructions:

- Do NOT preemptively load all references - use lazy loading based on actual need
- When loaded, treat content as mandatory instructions that override defaults
- Follow references recursively when needed

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

### Hexagonal Architecture Enforcement (REQUIRED)
- `domain/` must have ZERO imports from Spring, JPA, Kafka, or any infrastructure framework
- `infrastructure/` implements interfaces defined in `domain/port/`
- Controllers accept DTOs, call use cases, return DTOs — NEVER expose entities
- All financial calculations and state changes in `domain/service/`
- Service: MUST include ArchUnit tests that verify hexagonal architecture compliance

#### Domain model
you MUST:
1. Follow the pattern in the template exactly - records go in `domain/model`, entities in `infrastructure/persistence/entity/`
2. Create repository port in `domain/port/out/` and implementation in `infrastructure/persistence/repository/`

**Template:** See `docs/templates/domain-model-template.md` for correct pattern

**Common mistakes that will fail the build:**
- ❌ Adding `@Entity` or `@Table` in `domain/model/`
- ❌ Using `EntityManager` directly in domain service
- ❌ Skipping the record/entity separation

**If unsure, check the template first.**

**FAILURE TO COMPLY:** Any JPA/Spring annotation found in `domain/` layer will cause the build to fail.

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

### Law V: Spring Bean Registration
**EVERY new domain service MUST be registered as a bean.**

When adding a class in `domain/service/`:
1. Add bean to `DomainServiceConfig.java` in same commit
2. Use constructor injection (no field injection)
3. Do NOT add `@Service` to domain classes (use `@Bean` in config)

Example in `DomainServiceConfig.java`:
```java
@Bean
public MyNewService myNewService(RequiredPort port) {
    return new MyNewService(port);
}
```

### Law VI: Infrastructure Adapter Annotations
**EVERY adapter MUST have the correct annotation:**
- `infrastructure/persistence/` classes → `@Repository`
- `infrastructure/web/` classes → `@RestController`
- `infrastructure/external/` classes → `@FeignClient` (for remote calls)

### Law VII: Feign URL Configuration
**EVERY `@FeignClient(url = "${property}")` MUST have matching entry in `application.yaml`.**

Before committing Feign client changes:
1. Check all `url = "${...}"` properties in Feign clients
2. Verify each property exists in `application.yaml`
3. For Docker deployment, use service names: `http://service-name:port`

### Law VIII: Cross-Service Dependencies
**AVOID direct service dependencies.** If unavoidable:
1. Use `compileOnly` scope (not `implementation`)
2. Move shared models to `common` module
3. Ensure Flyway migration files have unique version prefixes (e.g., `V1_ledger_init.sql`)

### Law IX: Component Scanning
**Components in `common` module MUST be scannable.**

Add to main application class:
```java
@ComponentScan(basePackages = {"com.agentbanking.servicename", "com.agentbanking.common"})
```

### Law X: Pre-Commit Startup Validation
**Before committing significant changes, validate ALL services start**

### Law XI: Docker Build Cache Invalidation
**When source code changes are not reflected in the container, force a clean build.**

When rebuilding Docker containers after code changes:
- Always use `--no-cache` flag: `docker compose build --no-cache <service>`
- If issues persist, check if the build context is correct and files are being copied
- For Java services: rebuild with Gradle first, then rebuild Docker container
- For React/Vite frontends: delete `dist/` folder before rebuild

## Coding Standards

**Coding Reference:** Always use Context7 when you need library/API documentation, code generation, setup or configuration steps without having to explicitly ask.

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

## Documentation
- `docs` - at project root
- `docs/ideas` - high level requirements (ARCHITECTURE.md, BRD_SUMMARY.md)
- `docs/superpowers/specs/agent-banking-platform/*.md` - formal specs (BRD, BDD, Design)
- `docs/superpowers/specs/auth-iam-service/*.md` - specs for auth & iam service
- `docs/api/openapi.yaml` - external API spec

## Development Guidelines

For REST API design and error handling: @rules/api-standards.md
For testing strategies and coverage requirements: @rules/testing-guidelines.md
For Banking Specific Guidelines: @rules/banking-guidelines.md
