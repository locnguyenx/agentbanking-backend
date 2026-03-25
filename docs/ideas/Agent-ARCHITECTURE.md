# ARCHITECTURE.MD: Agent Banking Platform (Spring Edition)

## 1. Project Mission & Compliance
This project is an **Agent Banking Platform** facilitating financial services (Withdrawals, Deposits, Transfers, e-KYC) at third-party retail locations.
* **Regulatory Compliance:** Bank Malaysia standards.
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

## 3. Microservices Domain Map
The system is divided into independent services, each with its own PostgreSQL database:
1.  **Onboarding Service:** Handles e-KYC, MyKad verification, and JPN/SSM integration.
2.  **Ledger & Float Service:** Manages agent virtual wallets and transaction history. **(Mission Critical)**.
3.  **Switch Adapter Service:** Translates internal JSON to ISO 8583 (Card) and ISO 20022 (DuitNow).
4.  **Biller Service:** Manages utility payments and aggregator (Fiuu/JomPAY) webhooks.
5.  **Rules Service:** Centralized engine for fees, limits, and anti-smurfing (Velocity) checks.

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
