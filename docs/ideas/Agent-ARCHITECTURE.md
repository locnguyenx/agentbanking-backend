# ARCHITECTURE: Agent Banking Platform (Spring Edition)

## 1. Project Mission & Compliance
This project is an **Agent Banking Platform** facilitating financial services (Withdrawals, Deposits, Transfers, e-KYC) at third-party retail locations.
* **Regulatory Compliance:** Bank Negara Malaysia (BNM) standards.
* **Security:** Zero-trust architecture. No PII in logs. Hardware-level encryption for PINs.

## 2. Technical Stack

* **Scaffolding tool**: Seed4J (latest)
* **Language:** Java 21 (LTS)
* **Framework:** Spring Boot 4, Spring Cloud
* **Persistence:** Spring Data JPA (Hibernate) with PostgreSQL
* **Caching:** Redis (Spring Data Redis)
* **Messaging:** Apache Kafka (Spring Cloud Stream)
* **Gateway:** Spring Cloud Gateway (Reactive)
* **Testing:** JUnit 5, Mockito, ArchUnit
* **Backoffice UI:** React + TypeScript + Vite
* **IAM:** OAuth2 / Keycloak

## 3. The 5-Tier System Architecture

### 1. Tier 1: Channel Layer
- POS Terminals (Android/Flutter)
- Backoffice UI (React): for bank business users and IT Admin
- REST/HTTPS → single entry point

### 2. Tier 2: The Edge & Gateway (The Shield)
This is where the outside world connects. 
* **The Components:** We place a Web Application Firewall (WAF) in front of a **API Gateway**.
* **The Job:** 
    - Keep the API Gateway focused on: routing, JWT validation, rate‑limiting, logging, and tracing. API Gateway must be lightweight and handles thousands of concurrent socket connections perfectly. It strips the TLS encryption, validates the JWT session tokens, checks the `X-Idempotency-Key` to block duplicate requests, and enforces the strict API schemas we designed. 
    - It does *no* heavy business logic.
    - Use **OpenAPI / contracts** (e.g., via SpringDoc) and **Kong** or **Spring Cloud Gateway** just to enforce them.

### 3. Tier 3: The Business Core (The Brain)

In this tier, we use **Domain-Driven Design (DDD)** to ensure each microservice is decoupled and independently scalable.

1. **Onboarding Service:** Handles e-KYC, MyKad verification, and JPN/SSM integration.
2. **Ledger & Float Service:** Manages agent virtual wallets and transaction history. **(Mission Critical)**. Need to maintain an immutable audit log of every balance change.
3. **Biller Service:** Manages utility payments and aggregator (Fiuu/JomPAY) webhooks.
4. **Rules & Parameter Service:** Centralized engine for fees, limits, and anti-smurfing (Velocity) checks.
5. **Transaction Orchestrator:** Acts as the coordinator for complex financial flows. 
6. **Switch Adapter Service:** translates internal JSON to ISO 8583 (Card) and ISO 20022 (DuitNow) via the ISO Translation Engine.

Refer to `./ARCH-supplementary/Microservices Domain Map.md` for service dependencies

#### The Data & Async Layer (The Nervous System)
* **DB:** The absolute source of truth for your float balances, transaction logs, and parameter configurations. 
Candidate: PostgreSQL (when ACID is critical) or Redis (optimized for reading speed)
* **Caching:** Sitting in front of the database to cache high-read data. Every transaction needs to check the fee parameters; doing that against a SQL database will cause a bottleneck. Redis serves these in sub-milliseconds. 
Candidate: Redis
* **Messaging:** Decouples the system. If an agent does a transaction, the Java core publishes a `Transaction_Success` event to Kafka. Other services (like the SMS Notification sender or the Webhook Dispatcher) consume that event asynchronously so the POS terminal isn't kept waiting.
Candidate: Apache Kafka
* **Raw Bash/Shell Scripts:** As we designed earlier, rather than spinning up heavy JVM cron jobs for End-of-Day (EOD) settlement, lean, native bash scripts execute directly against the PostgreSQL instances to aggregate the `UNSETTLED` commissions and generate the CSV files for the core banking system.

### 4. Tier 4: The Translation Layer (The Diplomats)
Our modern JSON microservices cannot talk directly to legacy banking infrastructure.
This layer translates internal JSON to ISO 8583 (Card) and ISO 20022 (DuitNow).
* **HSM Connector:** Maintains persistent TCP/IP socket connections to the bank's physical Hardware Security Module to translate DUKPT PIN blocks.
* **Switch Connector:** Takes your internal JSON models and maps them into the strict ISO 8583 bitmap standards required by MEPS or PayNet.

### 5. Tier 5: Downstream Systems (The Destinations)
These are the external partners you depend on. Your system architecture isolates them so that if the ASTRO Biller API goes down, or the National ID (JPN) system undergoes maintenance, your core ledger and API gateway stay online, gracefully queuing trans

---

## ⚠️ The Architectural Decision
1. Clean Architecture (Invasiveness)
2. Low Overhead (Cognitive and Infrastructure)
3. Ease of Maintenance (The Versioning Nightmare)

## Other architecture components & policies

### Supporting components

| Components            | Goal / responsibilities                                                                 |
|------------------|------------------------------------------------------------------------------------------|
| **Platform**     | Start **locally** with Docker‑Compose (Kubernetes is only for production deployment), CI/CD, secrets, logging, monitoring, tracing, RBAC for infra.  |
| **Foundation**   | API Gateway, OAuth2/Keycloak, Temporal, Kafka, Redis, PostgreSQL, shared configs, Micrometer/logging/exporters.  |

**Temporal policy:**  
- Only allowed in `core-service` bounded contexts where event‑sourcing and audit trails are explicitly required.  
- Not allowed in `api-gateway`, `integration`, or `config`‑style services.

**Kafka policy:**  
- All services can publish/consume from Kafka, but **only `core-service` services may use Temporal‑style event‑sourcing on Kafka**.  
- Other services use Kafka for notifications and async work only.

**Redis policy:**  
- API Gateway: rate‑limiting.  
- Core services: read‑side caches from Temporal trackers.  
- Integration services: short‑lived state, retry‑related data.  

### Technology decisions (TDR skeleton)

* **Optimal approach:**
  - **Communication strategy: REST‑plus‑events** → keep Kafka as “async notification only” and keep commands synchronous
  - Use **Temporal + Kafka only for bounded contexts** that really need:
    - audit trails,
    - temporal replay,
    - complex state transitions (e.g., order lifecycle, payment flows).  
  - For “read‑only” or “config” services, use **plain Spring Boot + JPA + Kafka for notifications**, not full event‑sourcing.
* **Temporal**:
  - Usage: only applied to the `Transaction Orchestrator` service to follow SAGA pattern
  - Version discipline: lock to one Temporal major version across all services.  
  - Event‑schemas are versioned and documented in `docs/events/`. 
* **Kafka**:  
  - Topics: `order.events`, `payment.events`, `user.events`, `notification.*`.  
  - Retention, replicas, and partitions centrally defined. 
* **Keycloak**:  
  - Single realm per environment; JWTs contain `realm_access`, `resource_access`, and custom claims.  
  - API Gateway validates JWTs; backend services trust the Gateway or validate JWTs locally if required. 
* **PostgreSQL**:  
  - Each core service owns its schema/database; no cross‑service DB sharing.best-practices-for-microservices-architecture/
  - Schema changes via Flyway/Liquibase only, no `ddl-auto` in production. 
* **Kubernetes**: for production deployment only
  - One deployment per service; `ClusterIP` services; Ingress at API Gateway.  
  - Helm charts for Kafka, Redis, PostgreSQL, Keycloak, and Prometheus/Jaeger.
* **React**:  
  - SPA generated via `create‑react‑app` or similar, `Docker‑build` behind Nginx, served via Ingress, talking only to API Gateway `/api/**`. 

***

### Code quality & architecture rules (ArchUnit style)

- **Folder structure per service** must follow the template:
  - `domain` → domain logic + ports (no framework code).  
  - `application` → use‑case orchestration, Temporal handlers.  
  - `infrastructure` → web, persistence, Kafka, Redis, security adapters.
- These rules are enforced via:
  - **ArchUnit** tests in CI/CD (e.g., “domain must not depend on Spring”).
  - Project scaffolding: every service is generated from the same service template.

***

## 5. Architectural Guardrails (The "Laws")
### Law I: Layered Architecture
Each microservice must follow the strict path: 
`Controller` (Web/REST) $\rightarrow$ `Service` (Business Logic) $\rightarrow$ `Repository` (Data Access). 
* **DTOs:** Controllers must only accept and return DTOs, never Entities.
* **Logic Location:** All financial calculations and state changes must reside in the `@Service` layer.

### Law II: Transactional Integrity
* All financial methods must be marked `@Transactional`.
* **Ledger Updates:** Must use `PESSIMISTIC_WRITE` locks on the `AgentFloat` entity to prevent race conditions during high-concurrency withdrawals.
* **Idempotency:** Every transaction request must check the `X-Idempotency-Key` before processing.

### Law III: Error Handling
The AI must strictly implement the **Global Error Schema**. Never return a raw Exception or generic 500.
* **Structure:** `{ "status": "FAILED", "error": { "code": "...", "message": "...", "action_code": "..." } }`

### Law IV: Inter-service Communication
* **Synchronous:** Use `Spring Cloud OpenFeign` with Resilience4j circuit breakers.
* **Asynchronous:** Use `TransactionSuccessEvent` published to Kafka for non-critical flows (SMS, Commission, EFM).


## Reference

- Detailed processing for services: `./ARCH-supplementary/Detailed Service Processing.md`