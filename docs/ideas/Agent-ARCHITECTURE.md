# ARCHITECTURE.MD: Agent Banking Platform (Spring Edition)

## 1. Project Mission & Compliance
This project is an **Agent Banking Platform** facilitating financial services (Withdrawals, Deposits, Transfers, e-KYC) at third-party retail locations.
* **Regulatory Compliance:** Bank Negara Malaysia (BNM) standards.
* **Security:** Zero-trust architecture. No PII in logs. Hardware-level encryption for PINs.

## 2. Strict Technical Stack
The AI MUST NOT suggest or use technologies outside this list:
* **Language:** Java 21 (LTS).
* **Framework:** Spring Boot 3.x, Spring Cloud.
* **Persistence:** Spring Data JPA (Hibernate) with PostgreSQL.
* **Caching:** Redis (Spring Data Redis).
* **Messaging:** Apache Kafka (Spring Cloud Stream).
* **Gateway:** Spring Cloud Gateway (Reactive).
* **Testing:** JUnit 5, Mockito, ArchUnit.

## 3. The 5-Tier System Architecture

### 1. Tier 1: Channel Layer
- POS Terminals (Android/Flutter)
- REST/HTTPS → single entry point

### 2. Tier 2: The Edge & Gateway (The Shield)
This is where the outside world connects. 
* **The Components:** We place a Web Application Firewall (WAF) in front of a **Golang API Gateway**.
* **The Job:** Golang is incredibly lightweight and handles thousands of concurrent socket connections perfectly. It strips the TLS encryption, validates the JWT session tokens, checks the `X-Idempotency-Key` to block duplicate requests, and enforces the strict API schemas we designed. It does *no* heavy business logic.

### 3. Tier 3: The Business Core (The Brain)

In this tier, we use **Domain-Driven Design (DDD)** to ensure each microservice is decoupled and independently scalable.

1. **Transaction Orchestrator (Spring Boot):** Acts as the coordinator for complex financial flows. It uses **Spring Cloud OpenFeign** to communicate with the Ledger and Switch services.
2. **e-KYC & Onboarding (Spring State Machine):** Manages the "Open Account" lifecycle. The State Machine ensures an application cannot skip steps (e.g., it can't move to `APPROVED` unless the `AML_CHECK` and `BIOMETRIC_MATCH` states are `SUCCESS`).
3. **Ledger & Float Service (Spring Data JPA):** This is the most critical service. It manages the agent "wallets." We use **Hibernate Envers** here to maintain an immutable audit log of every balance change (required for BNM audits).
4.  **Biller Service:** Manages utility payments and aggregator (Fiuu/JomPAY) webhooks.
5. **Rules & Parameter Service (Spring Cache + Redis):** Instead of Moqui's internal logic, we use a dedicated Spring service that loads business rules (fees, limits) into **Redis**. This allows Tier 2 to make decisions in under 5ms.
6. **Saga Pattern (Spring Cloud Sleuth/Zipkin):** Since we are in a microservices environment, we use Saga patterns to manage distributed transactions. If a "Fund Transfer" fails at the National Switch, the **Saga Coordinator** ensures the Ledger Service automatically rolls back the agent's float.

Refer to `./ARCH-supplementary/Microservices Domain Map.md` for service dependencies

#### The Data & Async Layer (The Nervous System)
* **PostgreSQL:** The absolute source of truth for your float balances, transaction logs, and parameter configurations. 
* **Redis:** Sitting in front of the database to cache high-read data. Every transaction needs to check the fee parameters; doing that against a SQL database will cause a bottleneck. Redis serves these in sub-milliseconds.
* **Apache Kafka:** Decouples the system. If an agent does a transaction, the Java core publishes a `Transaction_Success` event to Kafka. Other services (like the SMS Notification sender or the Webhook Dispatcher) consume that event asynchronously so the POS terminal isn't kept waiting.
* **Raw Bash/Shell Scripts:** As we designed earlier, rather than spinning up heavy JVM cron jobs for End-of-Day (EOD) settlement, lean, native bash scripts execute directly against the PostgreSQL instances to aggregate the `UNSETTLED` commissions and generate the CSV files for the core banking system.

### 4. Tier 4: The Translation Layer (The Diplomats)
Your modern JSON microservices cannot talk directly to legacy banking infrastructure.
* **HSM Connector:** Maintains persistent TCP/IP socket connections to the bank's physical Hardware Security Module to translate DUKPT PIN blocks.
* **Switch Connector:** Takes your internal JSON models and maps them into the strict ISO 8583 bitmap standards required by MEPS or PayNet.

### 5. Tier 5: Downstream Systems (The Destinations)
These are the external partners you depend on. Your system architecture isolates them so that if the ASTRO Biller API goes down, or the National ID (JPN) system undergoes maintenance, your core ledger and API gateway stay online, gracefully queuing trans

## 4. Architectural Guardrails (The "Laws")
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

## 5. Coding Standards for AI
* **No Moqui:** Do not use Moqui Framework entities or logic.
* **Immutability:** Use Java Records for DTOs where possible.
* **Logging:** Use SLF4J with `log.info` for lifecycle and `log.error` for failures. Never log card numbers (PAN) or MyKad numbers in plain text.
* **Validation:** Use `jakarta.validation` (`@NotNull`, `@Positive`, etc.) on all incoming DTOs.

## Reference

- Detailed processing for services: `./ARCH-supplementary/Detailed Service Processing.md`
