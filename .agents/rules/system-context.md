# Core Context & System Constraints

> **Core Context & System Constraints**
> This document defines the non-negotiable architectural constraints and system context for AI agents working in this repository.

---

## 1. System Context

### 1.1. Project Overview
This project is an **Agent Banking Platform** that facilitates financial services at retail locations. 
* **Compliance:** Must strictly adhere to Bank Malaysia standards.
* **Security:** Employs a zero-trust architecture resulting in strict constraints (e.g., hardware-level encryption for PINs).

### 1.2. Architecture (5-Tier System)
*(For detailed design, refer to: `docs/superpowers/specs/agent-banking-platform/*-design.md`)*

1. **Tier 1: Channel Layer** — POS Terminals (Android/Flutter)
2. **Tier 2: Spring Cloud Gateway** — Handles JWT validation, rate limiting, and routing.
3. **Tier 3: Domain Core Services** — Manages Rules, Ledger & Float, Onboarding, Switch Adapter, and Biller.
4. **Tier 4: Translation Layer** — Contains HSM Connector, Switch Connector, and Biller Connector.
5. **Tier 5: Downstream Systems** — Sub-systems like HSM, PayNet, JPN, and Billers.

### 1.3. Documentation & Context Map
- **General Setup:** Use Context7 for any library/API documentation or setup.
- **Product Ideas:** `docs/ideas/**/**` (includes `ARCHITECTURE.md` and `BRD_SUMMARY.md`).
- **Formal Specifications:** 
  - **Platform Requirements:** `docs/superpowers/specs/agent-banking-platform/*brd.md`
  - **Platform BDD scenarios:** `docs/superpowers/specs/agent-banking-platform/*bdd.md`
  - **Platform Design:** `docs/superpowers/specs/agent-banking-platform/*design.md`
  - **Supplemental Features:** `docs/superpowers/specs/**/*.md`
- **External API:** `docs/api/openapi.yaml`.

---

## 2. Core Constraints (NON-NEGOTIABLE)

### 2.1. File Loading & Usage
* **Lazy Loading Only:** You MUST load external file references (like `@rules/general.md`) ONLY on a need-to-know basis. **Do NOT preemptively load.**
* **Enforcement:** Treat loaded content as mandatory, recursive instructions.

### 2.2. Technology Stack
**You MUST NOT use technologies outside of this exact list:**
* **Language:** Java 21 (LTS)
* **Framework:** Spring Boot 3.x, Spring Cloud
* **Persistence:** Spring Data JPA (Hibernate) with PostgreSQL
* **Caching:** Redis (Spring Data Redis)
* **Messaging:** Apache Kafka (Spring Cloud Stream)
* **Gateway:** Spring Cloud Gateway (Reactive)
* **Testing:** JUnit 5, Mockito, ArchUnit
* **UI:** React + TypeScript + Vite

### 2.3. Hexagonal Architecture Strict Enforcement
* **`domain/` Layer:** MUST have ZERO imports from Spring, JPA, Kafka, or any infrastructure framework. Contains only models (Java records) and ports.
* **`application/` Layer:** Used strictly for use case orchestration.
* **`infrastructure/` Layer:** Adapters implementing ports. Contains all entities (`infrastructure/persistence/entity/`) and repositories.
* **Validation:** ArchUnit tests MUST exist to verify compliance. Any JPA/Spring annotation in `domain/` will fail the build.
* **Template Pattern (MANDATORY):** Follow the pattern in `docs/templates/domain-model-template.md`.

### 2.4. Architectural Laws

* **Law 1 - Layering:** Controller → Service → Repository. 
  * Controllers accept/return DTOs ONLY. 
  * State changes exist ONLY in `@Service`.
* **Law 2 - Transactional Integrity:** 
  * All financial methods MUST be `@Transactional`. 
  * Updates to `AgentFloat` MUST use `PESSIMISTIC_WRITE` locks.
* **Law 3 - Idempotency:** Validate `X-Idempotency-Key` for every financial request (Redis cache TTL: 24h).
* **Law 4 - Error Handling:** MUST return the Global Error Schema. Never return a raw exception or generic 500. Code MUST come from the centralized registry.
   ```json
   {
     "status": "FAILED",
     "error": {
       "code": "ERR_xxx",
       "message": "Human-readable message",
       "action_code": "DECLINE | RETRY | REVIEW",
       "trace_id": "dist-trace-id",
       "timestamp": "2026-03-25T14:30:00+08:00"
     }
   }
   ```
* **Law 5 - Inter-service Comm:** Synchronous via Spring Cloud OpenFeign + Resilience4j. Asynchronous via Kafka. NO cross-service DB joins. DB-per-service ONLY.
* **Law 6 - Spring Bean Registration:** Domain services MUST be registered via `@Bean` in `DomainServiceConfig.java` (No `@Service` annotations allowed in `domain/`).
* **Law 7 - Adapter Annotations:** 
  * `infrastructure/persistence/` → `@Repository`
  * `infrastructure/web/` → `@RestController`
  * `infrastructure/external/` → `@FeignClient`
* **Law 8 - Feign URLs:** Every `@FeignClient(url = "${property}")` MUST have a matching property in `application.yaml` (using Docker service names, e.g., `http://service-name:port`).
* **Law 9 - Dependencies:** AVOID direct service dependencies. If absolutely required, use `compileOnly`. Shared models MUST reside in the `common` module.
* **Law 10 - Component Scanning:** Main class MUST include `@ComponentScan(basePackages = {"com.agentbanking.servicename", "com.agentbanking.common"})`.
* **Law 11 - Pre-Commit Check:** ALL services MUST strictly compile and start cleanly before committing code.
* **Law 12 - Docker Builds:** Use `--no-cache` for cache invalidation when containers fail to reflect code changes.
* **Law 13 - Temporal Sagas:** Workflows use `@WorkflowImpl(taskQueues = "task-queue")`; Activities use `@ActivityImpl(workers = "task-queue")`. Both require `@Component`. Configurations use `${TEMPORAL_ADDRESS}`. Reference: `docs/lessons-learned/2026-04-11-temporal-worker-registration-fix.md`.

### 2.5. Coding Constraints
* **Immutability:** MUST use Java Records for all DTOs.
* **Validation:** MUST apply `jakarta.validation` annotations (like `@NotNull`, `@Positive`) on ALL incoming DTOs.
* **Logging Exclusions (CRITICAL):**
  - **NEVER log:** Card numbers (PAN) (mask as `411111******1111`).
  - **NEVER log:** MyKad numbers (must be encrypted at rest).
  - **NEVER log:** PIN blocks (never decrypt outside HSM).
  - **NEVER log:** Any PII in plaintext.
* **Database Practices:** Flyway MUST be used for migrations. Migration files MUST have uniquely defined version prefixes (e.g., `V1_ledger_init.sql`).
