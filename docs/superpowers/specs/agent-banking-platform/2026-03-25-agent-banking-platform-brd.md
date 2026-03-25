# Business Requirements Document (BRD)
## Agent Banking Platform (Malaysia)

**Version:** 1.0
**Date:** 2026-03-25
**Status:** Draft — Pending Review

---

## 1. Project Overview & Goals

### Project Name
Agent Banking Platform (Malaysia)

### Business Purpose
Enable third-party retail agents (merchants) to offer banking services — withdrawals, deposits, transfers, bill payments, e-KYC onboarding — to customers via Android/Flutter POS terminals, in compliance with Bank Malaysia regulations.

### Target Users
- **Agents** (Micro / Standard / Premier tiers) — retail merchants who process transactions for customers
- **Customers** — individuals receiving banking services at agent locations
- **Bank Operations (Backoffice)** — bank staff managing operations, settlement, compliance, agent management

### Deliverables
1. **Backend Platform** — 5 microservices:
   - **Rules Service** — Fee engine, limits, velocity checks
   - **Ledger & Float Service** — Agent wallets, journal entries, real-time settlement
   - **Onboarding Service** — e-KYC, MyKad verification, JPN integration
   - **Switch Adapter Service** — ISO 8583 (card) and ISO 20022 (DuitNow) translation via PayNet
   - **Biller Service** — Utility payments, biller webhooks, telco top-ups
   - Clear separation of:
   - **External API (Gateway)** — Spring Cloud Gateway exposing REST endpoints for POS terminals (channel apps). Documented in public OpenAPI 3.0 spec. This is the contract channel apps consume.
   - **Internal APIs** — Inter-service communication via OpenFeign/Kafka. Internal API contracts may have separate OpenAPI specs per service. Implementation details (sync vs async, protocol choice) are decided in the Design phase.
2. **Backoffice UI** — Web application for bank staff:
   - **Business:** Agent management, transaction monitoring, settlement review, commission management, dispute handling
   - **Tech:** System health, configuration, audit logs, error investigation

### External API Consumers
- POS terminals (Android/Flutter channel apps)
- Potentially future channels (mobile banking, web portal)

### Internal Communication
- Service-to-service calls (OpenFeign with Resilience4j circuit breakers)
- Async events (Kafka for non-critical flows: SMS, commissions, EFM)
- Each service may expose its own internal API spec

### Business Goals
1. Expand banking access to underserved areas via retail agent network
2. Generate revenue through transaction fees and agent commissions
3. Comply with Bank Malaysia's Agent Banking guidelines
4. Real-time settlement for agent float management

### MVP Scope (Phase 1)
- Cash Withdrawal (EMV chip + PIN only — MyKad withdrawal is Phase 2)
- Cash Deposit (cash only — card deposit is Phase 2)
- Balance Inquiry
- e-KYC Onboarding (MyKad + JPN biometric verification)
- Backoffice: Agent management, transaction monitoring, settlement review

### Full Platform Scope (Phase 2+)
- DuitNow Fund Transfer (ISO 20022)
- Bill Payments (JomPAY, ASTRO RPN, TM RPN, EPF)
- Prepaid Top-up (CELCOM, M1)
- Sarawak Pay e-Wallet
- eSSP Purchase
- PIN Purchase, Cashless Payment, JomPAY ON-US/OFF-US
- Full backoffice capabilities (advanced analytics, dispute management, compliance reporting)

---

## 2. User Stories

### Cross-Cutting Services

#### Rules Service
| ID | User Story | Phase |
|----|-----------|-------|
| US-R01 | As a bank operator, I want to configure transaction fees (fixed/percentage) per agent tier so that Micro, Standard, and Premier agents are charged appropriately | MVP |
| US-R02 | As a bank operator, I want to set daily transaction limits per agent tier to control risk exposure | MVP |
| US-R03 | As the system, I want to enforce velocity checks (max transactions per MyKad per day) to prevent smurfing | MVP |
| US-R04 | As the system, I want to calculate Customer Fee, Agent Commission, and Bank Share per transaction | MVP |

#### Ledger & Float Service
| ID | User Story | Phase |
|----|-----------|-------|
| US-L01 | As an agent, I want to check my virtual wallet balance so I know how much cash I can process | MVP |
| US-L02 | As the system, I want to record every financial transaction as a double-entry journal entry for audit | MVP |
| US-L03 | As an agent, I want real-time settlement so my float reflects transactions immediately | MVP |
| US-L04 | As a customer, I want to check my account balance through the agent's POS terminal | MVP |

### Cash Transactions
| ID | User Story | Phase |
|----|-----------|-------|
| US-L05 | As a customer, I want to withdraw cash using my ATM card (EMV + PIN) at an agent location | MVP |
| US-L06 | As a customer, I want to withdraw cash using MyKad | Phase 2 |
| US-L07 | As a customer, I want to deposit cash at an agent location with account validation before funds move | MVP |
| US-L08 | As a customer, I want to deposit via card at an agent location | Phase 2 |

### e-KYC & Onboarding
| ID | User Story | Phase |
|----|-----------|-------|
| US-O01 | As an agent, I want to verify a customer's MyKad via JPN to confirm their identity | MVP |
| US-O02 | As an agent, I want to perform biometric (thumbprint) match-on-card for identity verification | MVP |
| US-O03 | As the system, I want to auto-approve KYC when match=yes AND AML=clean AND age>=18, or queue for manual review | MVP |
| US-O04 | As an agent, I want to open accounts for new and existing customers via MyKad | Phase 2 |

### Bill Payments (Phase 2)
| ID | User Story | Phase |
|----|-----------|-------|
| US-B01 | As a customer, I want to pay utility bills (JomPAY) via cash or card | Phase 2 |
| US-B02 | As a customer, I want to pay ASTRO RPN bills via cash or card | Phase 2 |
| US-B03 | As a customer, I want to pay TM RPN bills via cash or card | Phase 2 |
| US-B04 | As a customer, I want to pay EPF i-SARAAN/i-SURI/SELF EMPLOYED via cash or card | Phase 2 |
| US-B05 | As the system, I want to validate Ref-1 against biller DB before payment | Phase 2 |

### Prepaid Top-up (Phase 2)
| ID | User Story | Phase |
|----|-----------|-------|
| US-T01 | As a customer, I want to top-up CELCOM prepaid via cash or card | Phase 2 |
| US-T02 | As a customer, I want to top-up M1 prepaid via cash or card | Phase 2 |
| US-T03 | As the system, I want to validate phone number against telco before top-up | Phase 2 |

### DuitNow & JomPAY (Phase 2)
| ID | User Story | Phase |
|----|-----------|-------|
| US-D01 | As a customer, I want to transfer funds via DuitNow (mobile, MyKad, BRN proxies) | Phase 2 |
| US-D02 | As a customer, I want to make JomPAY payments (ON-US and OFF-US) via cash or card | Phase 2 |

### e-Wallet & eSSP (Phase 2)
| ID | User Story | Phase |
|----|-----------|-------|
| US-W01 | As a customer, I want to withdraw from Sarawak Pay e-Wallet via cash or card | Phase 2 |
| US-W02 | As a customer, I want to top-up Sarawak Pay e-Wallet via cash or card | Phase 2 |
| US-E01 | As a customer, I want to purchase eSSP certificates (BSN) via cash or card | Phase 2 |

### Other Services (Phase 2)
| ID | User Story | Phase |
|----|-----------|-------|
| US-X01 | As a customer, I want to make cashless payments | Phase 2 |
| US-X02 | As a customer, I want to make PIN purchases via cash or card | Phase 2 |

### API Gateway & Backoffice
| ID | User Story | Phase |
|----|-----------|-------|
| US-G01 | As a POS terminal, I want to send all requests through a single API Gateway endpoint | MVP |
| US-G02 | As the system, I want to authenticate all external API requests via token-based auth | MVP |
| US-BO01 | As a bank operator, I want to create/edit/deactivate agent accounts with tier assignment | MVP |
| US-BO02 | As a bank operator, I want to monitor real-time transaction activity | MVP |
| US-BO03 | As a bank operator, I want to review settlement reports | MVP |
| US-BO04 | As a bank operator, I want to manage system configuration (fees, limits, rules) | Phase 2 |
| US-BO05 | As a bank operator, I want to view audit logs and error investigation | Phase 2 |

---

## 3. Functional Requirements

### FR-1: Rules & Fee Engine
| ID | Requirement | US |
|----|------------|-----|
| FR-1.1 | System shall support configurable fee structures (fixed amount or percentage) per transaction type per agent tier | US-R01 |
| FR-1.2 | System shall support configurable daily transaction limits per agent tier per transaction type | US-R02 |
| FR-1.3 | System shall check transaction count per MyKad per day against configured velocity threshold before processing | US-R03 |
| FR-1.4 | System shall split each transaction fee into Customer Fee, Agent Commission, and Bank Share | US-R04 |

### FR-2: Ledger & Float
| ID | Requirement | US |
|----|------------|-----|
| FR-2.1 | System shall maintain a virtual wallet (float) balance per agent with real-time updates | US-L01, US-L03 |
| FR-2.2 | System shall record every financial transaction as a double-entry journal (debit + credit) | US-L02 |
| FR-2.3 | System shall use PESSIMISTIC_WRITE locks on AgentFloat during balance updates | US-L03 |
| FR-2.4 | System shall check X-Idempotency-Key header before processing any transaction | US-L05 |
| FR-2.5 | System shall validate destination account via ProxyEnquiry/AccountEnquiry before crediting | US-L07 |

### FR-3: Cash Withdrawal
| ID | Requirement | US |
|----|------------|-----|
| FR-3.1 | System shall process ATM card withdrawals with EMV chip + PIN verification | US-L05 |
| FR-3.2 | System shall process MyKad-based withdrawals | US-L06 |
| FR-3.3 | System shall enforce configurable daily withdrawal limit (default RM 5,000) | US-L05, US-L06 |
| FR-3.4 | System shall trigger MTI 0400 Reversal if terminal printer fails or network drops after switch approval | US-L05, US-L06 |
| FR-3.5 | System shall debit agent float and credit bank settlement account on withdrawal | US-L05 |

### FR-4: Cash Deposit
| ID | Requirement | US |
|----|------------|-----|
| FR-4.1 | System shall process cash deposits with account validation before funds move | US-L07 |
| FR-4.2 | System shall process card-based deposits | US-L08 |
| FR-4.3 | System shall credit agent float and debit customer account on deposit | US-L07 |

### FR-5: Balance Inquiry
| ID | Requirement | US |
|----|------------|-----|
| FR-5.1 | System shall return customer account balance via card + PIN authentication | US-L04 |
| FR-5.2 | System shall return agent wallet balance | US-L01 |

### FR-6: e-KYC & Onboarding
| ID | Requirement | US |
|----|------------|-----|
| FR-6.1 | System shall verify MyKad (12-digit) via JPN API | US-O01 |
| FR-6.2 | System shall perform biometric (thumbprint) match-on-card verification | US-O02 |
| FR-6.3 | System shall auto-approve when: match=YES AND AML=CLEAN AND age>=18 | US-O03 |
| FR-6.4 | System shall queue for manual review when: match=NO OR high-risk AML flag | US-O03 |
| FR-6.5 | System shall support account opening for new and existing customers via MyKad | US-O04 |

### FR-7: Bill Payments
| ID | Requirement | US |
|----|------------|-----|
| FR-7.1 | System shall process JomPAY bill payments (ON-US and OFF-US) via cash and card | US-B01, US-D02 |
| FR-7.2 | System shall process ASTRO RPN bill payments via cash and card | US-B02 |
| FR-7.3 | System shall process TM RPN bill payments via cash and card | US-B03 |
| FR-7.4 | System shall process EPF i-SARAAN/i-SURI/SELF EMPLOYED payments via cash and card | US-B04 |
| FR-7.5 | System shall validate Ref-1 against biller database before processing payment | US-B05 |

### FR-8: Prepaid Top-up
| ID | Requirement | US |
|----|------------|-----|
| FR-8.1 | System shall process CELCOM prepaid top-up via cash and card | US-T01 |
| FR-8.2 | System shall process M1 prepaid top-up via cash and card | US-T02 |
| FR-8.3 | System shall validate phone number against telco before processing top-up | US-T03 |

### FR-9: DuitNow Transfer
| ID | Requirement | US |
|----|------------|-----|
| FR-9.1 | System shall process DuitNow transfers via ISO 20022 (PayNet) | US-D01 |
| FR-9.2 | System shall support Mobile Number, MyKad Number, and BRN as DuitNow proxies | US-D01 |
| FR-9.3 | System shall complete DuitNow settlement in under 15 seconds | US-D01 |

### FR-10: e-Wallet & eSSP
| ID | Requirement | US |
|----|------------|-----|
| FR-10.1 | System shall process Sarawak Pay e-Wallet withdrawal via cash and card | US-W01 |
| FR-10.2 | System shall process Sarawak Pay e-Wallet top-up via cash and card | US-W02 |
| FR-10.3 | System shall process eSSP certificate purchase per BSN regulations | US-E01 |

### FR-11: Other Transactions
| ID | Requirement | US |
|----|------------|-----|
| FR-11.1 | System shall process cashless payments (card-based transactions without cash handling, where agent processes payment on behalf of customer) | US-X01 |
| FR-11.2 | System shall process PIN-based purchases where customer enters PIN on POS terminal for goods/services payment | US-X02 |

### FR-12: API Gateway
| ID | Requirement | US |
|----|------------|-----|
| FR-12.1 | Gateway shall route all external POS requests to appropriate backend service | US-G01 |
| FR-12.2 | Gateway shall authenticate all external requests via token-based auth | US-G02 |
| FR-12.3 | Gateway shall document all external endpoints in OpenAPI 3.0 spec | US-G01 |

### FR-13: Backoffice
| ID | Requirement | US |
|----|------------|-----|
| FR-13.1 | System shall provide agent CRUD operations with tier assignment | US-BO01 |
| FR-13.2 | System shall provide real-time transaction monitoring dashboard | US-BO02 |
| FR-13.3 | System shall provide settlement report viewing and export | US-BO03 |
| FR-13.4 | System shall provide configuration UI for fees, limits, and rules | US-BO04 |
| FR-13.5 | System shall provide audit log viewer with search and filtering | US-BO05 |

---

## 4. Entity Definitions

BDD scenarios reference these entities by name and field. All entities use UUID primary keys unless noted otherwise.

### ENT-1: Agent
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| agentId | UUID | Yes | Unique agent identifier |
| agentCode | String(20) | Yes | Human-readable agent code (e.g., "AGT-00123") |
| businessName | String(200) | Yes | Registered business name |
| tier | Enum | Yes | MICRO, STANDARD, PREMIER |
| status | Enum | Yes | ACTIVE, SUSPENDED, DEACTIVATED |
| merchantGpsLat | Decimal(9,6) | Yes | Registered GPS latitude |
| merchantGpsLng | Decimal(9,6) | Yes | Registered GPS longitude |
| mykadNumber | String(12) | Yes | Owner's MyKad number (encrypted at rest) |
| phoneNumber | String(15) | Yes | Contact phone |
| createdAt | Timestamp | Yes | Registration timestamp |
| updatedAt | Timestamp | Yes | Last modification timestamp |

### ENT-2: AgentFloat
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| floatId | UUID | Yes | Unique float identifier |
| agentId | UUID (FK) | Yes | References Agent.agentId |
| balance | BigDecimal(15,2) | Yes | Current available balance in RM |
| reservedBalance | BigDecimal(15,2) | Yes | Balance reserved for pending transactions |
| currency | String(3) | Yes | Always "MYR" |
| version | Long | Yes | Optimistic lock version |
| updatedAt | Timestamp | Yes | Last balance update |

### ENT-3: Transaction
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| transactionId | UUID | Yes | Unique transaction identifier |
| idempotencyKey | String(64) | Yes | X-Idempotency-Key from request (unique) |
| agentId | UUID (FK) | Yes | References Agent.agentId |
| transactionType | Enum | Yes | CASH_WITHDRAWAL, CASH_DEPOSIT, BALANCE_INQUIRY, ... |
| amount | BigDecimal(15,2) | Conditional | Transaction amount (null for balance inquiry) |
| customerFee | BigDecimal(15,2) | Yes | Fee charged to customer |
| agentCommission | BigDecimal(15,2) | Yes | Commission earned by agent |
| bankShare | BigDecimal(15,2) | Yes | Share retained by bank |
| status | Enum | Yes | PENDING, COMPLETED, FAILED, REVERSED |
| errorCode | String(20) | Conditional | Error code if status=FAILED |
| customerMykad | String(12) | Conditional | Customer MyKad (encrypted, for velocity checks) |
| customerCardMasked | String(19) | Conditional | Masked PAN (e.g., "411111******1111") |
| switchReference | String(50) | Conditional | External switch reference ID |
| geofenceLat | Decimal(9,6) | Yes | Transaction GPS latitude |
| geofenceLng | Decimal(9,6) | Yes | Transaction GPS longitude |
| createdAt | Timestamp | Yes | Transaction initiation timestamp |
| completedAt | Timestamp | Conditional | Transaction completion timestamp |

### ENT-4: JournalEntry
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| journalId | UUID | Yes | Unique journal entry identifier |
| transactionId | UUID (FK) | Yes | References Transaction.transactionId |
| entryType | Enum | Yes | DEBIT, CREDIT |
| accountCode | String(20) | Yes | Chart of accounts code |
| amount | BigDecimal(15,2) | Yes | Entry amount |
| description | String(200) | Yes | Human-readable description |
| createdAt | Timestamp | Yes | Entry timestamp |

### ENT-5: FeeConfig
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| feeConfigId | UUID | Yes | Unique fee config identifier |
| transactionType | Enum | Yes | Transaction type this fee applies to |
| agentTier | Enum | Yes | Agent tier (MICRO, STANDARD, PREMIER) |
| feeType | Enum | Yes | FIXED, PERCENTAGE |
| customerFeeValue | BigDecimal(15,4) | Yes | Fee value (RM for FIXED, % for PERCENTAGE) |
| agentCommissionValue | BigDecimal(15,4) | Yes | Commission value |
| bankShareValue | BigDecimal(15,4) | Yes | Bank share value |
| dailyLimitAmount | BigDecimal(15,2) | Yes | Max daily transaction amount for this tier/type |
| dailyLimitCount | Integer | Yes | Max daily transaction count for this tier/type |
| effectiveFrom | Date | Yes | Config effective start date |
| effectiveTo | Date | Conditional | Config effective end date (null = indefinite) |

### ENT-6: VelocityRule
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| ruleId | UUID | Yes | Unique rule identifier |
| ruleName | String(100) | Yes | Human-readable rule name |
| maxTransactionsPerDay | Integer | Yes | Max transactions per MyKad per day |
| maxAmountPerDay | BigDecimal(15,2) | Yes | Max total amount per MyKad per day |
| scope | Enum | Yes | GLOBAL, PER_TRANSACTION_TYPE |
| transactionType | Enum | Conditional | Required if scope=PER_TRANSACTION_TYPE |
| isActive | Boolean | Yes | Whether rule is currently enforced |
| createdAt | Timestamp | Yes | Rule creation timestamp |

### ENT-7: KycVerification
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| verificationId | UUID | Yes | Unique verification identifier |
| mykadNumber | String(12) | Yes | Customer MyKad (encrypted) |
| fullName | String(200) | Yes | Full name from JPN |
| dateOfBirth | Date | Yes | DOB from JPN |
| age | Integer | Yes | Calculated age |
| amlStatus | Enum | Yes | CLEAN, FLAGGED, BLOCKED |
| biometricMatch | Enum | Yes | MATCH, NO_MATCH, NOT_ATTEMPTED |
| verificationStatus | Enum | Yes | AUTO_APPROVED, MANUAL_REVIEW, REJECTED |
| rejectionReason | String(500) | Conditional | Reason if rejected |
| verifiedAt | Timestamp | Yes | Verification timestamp |
| reviewedBy | String(100) | Conditional | Manual reviewer ID (if manual review) |

### ENT-8: AuditLog
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| auditId | UUID | Yes | Unique audit entry identifier |
| entityType | String(50) | Yes | Entity type affected (e.g., "Agent", "Transaction") |
| entityId | UUID | Yes | Entity ID affected |
| action | Enum | Yes | CREATE, UPDATE, DELETE, PROCESS, FAIL |
| performedBy | String(100) | Yes | User or system that performed the action |
| changes | JSON | Conditional | Field changes (before/after) for updates |
| ipAddress | String(45) | Yes | Source IP (IPv4/IPv6) |
| timestamp | Timestamp | Yes | Action timestamp |

---

## 5. Non-Functional Requirements

### NFR-1: Performance
| ID | Requirement |
|----|------------|
| NFR-1.1 | API Gateway response time: < 500ms (p95) for transaction requests |
| NFR-1.2 | DuitNow transfer completion: < 15 seconds end-to-end |
| NFR-1.3 | Balance inquiry response: < 200ms |

### NFR-2: Availability & Reliability
| ID | Requirement |
|----|------------|
| NFR-2.1 | System uptime: 99.9% (8.76 hours downtime/year) |
| NFR-2.2 | Circuit breaker on all inter-service calls (Resilience4j) |
| NFR-2.3 | Store & Forward for reversals when network is down |

### NFR-3: Security
| ID | Requirement |
|----|------------|
| NFR-3.1 | Zero-trust architecture — every request authenticated, every service authorized |
| NFR-3.2 | PINs must never be logged in plaintext — hardware-level encryption |
| NFR-3.3 | PAN masking: display only first 6 and last 4 digits (e.g., `411111******1111`) |
| NFR-3.4 | MyKad numbers must never appear in logs in plaintext |
| NFR-3.5 | All external API traffic over TLS 1.2+ |

### NFR-4: Compliance
| ID | Requirement |
|----|------------|
| NFR-4.1 | Comply with Bank Malaysia Agent Banking guidelines |
| NFR-4.2 | Geofencing: transactions allowed only within 100m of registered Merchant GPS |
| NFR-4.3 | Full audit trail for every financial transaction (who, what, when, where) |

### NFR-5: Scalability
| ID | Requirement |
|----|------------|
| NFR-5.1 | Each microservice independently scalable |
| NFR-5.2 | Support horizontal scaling via Kubernetes (future) |

### NFR-6: Observability
| ID | Requirement |
|----|------------|
| NFR-6.1 | Structured logging (SLF4J) — lifecycle at INFO, failures at ERROR |
| NFR-6.2 | Distributed tracing across services |
| NFR-6.3 | Health check endpoints on every service |

---

## 6. Constraints & Assumptions

### Constraints
| ID | Constraint |
|----|-----------|
| C-1 | Language: Java 21 (LTS) only |
| C-2 | Framework: Spring Boot 3.x, Spring Cloud only — no Moqui or other frameworks |
| C-3 | Database: PostgreSQL per microservice (database-per-service pattern) |
| C-4 | Caching: Redis (Spring Data Redis) |
| C-5 | Messaging: Apache Kafka (Spring Cloud Stream) |
| C-6 | Gateway: Spring Cloud Gateway (Reactive) |
| C-7 | Testing: JUnit 5, Mockito, ArchUnit |
| C-8 | Architecture: Hexagonal (Ports & Adapters) per service |
| C-9 | Inter-service sync: OpenFeign with Resilience4j circuit breakers |
| C-10 | DTOs: Java Records where possible |
| C-11 | Validation: jakarta.validation on all incoming DTOs |
| C-12 | No PII in logs. Ever. |

### Assumptions
| ID | Assumption |
|----|-----------|
| A-1 | PayNet is the sole payment switch provider for both card (ISO 8583) and DuitNow (ISO 20022) |
| A-2 | JPN provides direct API access for MyKad verification and biometric match |
| A-3 | POS terminals run Android/Flutter and communicate via REST/HTTPS |
| A-4 | Real-time settlement (no EOD batch) — each transaction settles immediately |
| A-5 | Three agent tiers: Micro, Standard, Premier |
| A-6 | Backoffice UI framework is not yet decided — to be determined in Design phase |
| A-7 | Infrastructure (Kubernetes, CI/CD) is out of scope for initial specs |

---

## 7. Traceability Matrix: User Stories → Functional Requirements

| User Story | Functional Requirements | Phase |
|-----------|------------------------|-------|
| US-R01 | FR-1.1 | MVP |
| US-R02 | FR-1.2 | MVP |
| US-R03 | FR-1.3 | MVP |
| US-R04 | FR-1.4 | MVP |
| US-L01 | FR-2.1, FR-5.2 | MVP |
| US-L02 | FR-2.2 | MVP |
| US-L03 | FR-2.1, FR-2.3 | MVP |
| US-L04 | FR-5.1 | MVP |
| US-L05 | FR-2.4, FR-3.1, FR-3.3, FR-3.4, FR-3.5 | MVP |
| US-L06 | FR-3.2, FR-3.3, FR-3.4, FR-3.5 | Phase 2 |
| US-L07 | FR-2.5, FR-4.1, FR-4.3 | MVP |
| US-L08 | FR-4.2, FR-4.3 | Phase 2 |
| US-O01 | FR-6.1 | MVP |
| US-O02 | FR-6.2 | MVP |
| US-O03 | FR-6.3, FR-6.4 | MVP |
| US-O04 | FR-6.5 | Phase 2 |
| US-B01 | FR-7.1 | Phase 2 |
| US-B02 | FR-7.2 | Phase 2 |
| US-B03 | FR-7.3 | Phase 2 |
| US-B04 | FR-7.4 | Phase 2 |
| US-B05 | FR-7.5 | Phase 2 |
| US-T01 | FR-8.1 | Phase 2 |
| US-T02 | FR-8.2 | Phase 2 |
| US-T03 | FR-8.3 | Phase 2 |
| US-D01 | FR-9.1, FR-9.2, FR-9.3 | Phase 2 |
| US-D02 | FR-7.1 | Phase 2 |
| US-W01 | FR-10.1 | Phase 2 |
| US-W02 | FR-10.2 | Phase 2 |
| US-E01 | FR-10.3 | Phase 2 |
| US-X01 | FR-11.1 | Phase 2 |
| US-X02 | FR-11.2 | Phase 2 |
| US-G01 | FR-12.1, FR-12.3 | MVP |
| US-G02 | FR-12.2 | MVP |
| US-BO01 | FR-13.1 | MVP |
| US-BO02 | FR-13.2 | MVP |
| US-BO03 | FR-13.3 | MVP |
| US-BO04 | FR-13.4 | Phase 2 |
| US-BO05 | FR-13.5 | Phase 2 |
