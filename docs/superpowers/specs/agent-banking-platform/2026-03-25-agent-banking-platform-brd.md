# Business Requirements Document (BRD)
## Agent Banking Platform (Malaysia)

**Version:** 1.1
**Date:** 2026-03-26
**Status:** Revised — v1.0 + supplemental information integration

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
1. **Backend Platform** — 5+ microservices:
   - **Rules Service** — Fee engine, limits, velocity checks
   - **Ledger & Float Service** — Agent wallets, journal entries, real-time settlement
   - **Onboarding Service** — e-KYC, MyKad verification, JPN integration
   - **Switch Adapter Service** — ISO 8583 (card) and ISO 20022 (DuitNow) translation via PayNet
   - **Biller Service** — Utility payments, biller webhooks, telco top-ups
   - **Transaction Orchestrator** — Coordinator for multi-step financial flows (Saga)
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
- Merchant Services (Retail Sales, PIN Purchase, Cash-Back)
- EOD Net Settlement & Reconciliation
- Discrepancy Resolution (Maker-Checker)
- Agent Onboarding (Micro-Agent STP, Standard/Premier Non-STP)
- Full backoffice capabilities (advanced analytics, dispute management, compliance reporting)

---

## 2. User Stories

### STP Classification Legend
- **100% STP** — Automated, no human intervention
- **Conditional STP** — Rules engine, fallback to manual queue
- **Non-STP** — Human Maker-Checker required

### Cross-Cutting Services

#### Rules Service
| ID | User Story | STP Category | Phase |
|----|-----------|-------------|-------|
| US-R01 | As a bank operator, I want to configure transaction fees (fixed/percentage) per agent tier | N/A | MVP |
| US-R02 | As a bank operator, I want to set daily transaction limits per agent tier to control risk exposure | N/A | MVP |
| US-R03 | As the system, I want to enforce velocity checks (max transactions per MyKad per day) to prevent smurfing | N/A | MVP |
| US-R04 | As the system, I want to calculate Customer Fee, Agent Commission, and Bank Share per transaction | N/A | MVP |

#### Ledger & Float Service
| ID | User Story | STP Category | Phase |
|----|-----------|-------------|-------|
| US-L01 | As an agent, I want to check my virtual wallet balance so I know how much cash I can process | N/A | MVP |
| US-L02 | As the system, I want to record every financial transaction as a double-entry journal entry for audit | N/A | MVP |
| US-L03 | As an agent, I want real-time float settlement so my virtual balance reflects transactions immediately | N/A | MVP |
| US-L04 | As a customer, I want to check my account balance through the agent's POS terminal | 100% STP | MVP |

### Cash Transactions
| ID | User Story | STP Category | Phase |
|----|-----------|-------------|-------|
| US-L05 | As a customer, I want to withdraw cash using my ATM card (EMV + PIN) at an agent location | 100% STP | MVP |
| US-L06 | As a customer, I want to withdraw cash using MyKad at an agent location | 100% STP | Phase 2 |
| US-L07 | As a customer, I want to deposit cash at an agent location with account validation before funds move | 100% STP | MVP |
| US-L08 | As a customer, I want to deposit via card at an agent location | 100% STP | Phase 2 |

### e-KYC & Onboarding
| ID | User Story | STP Category | Phase |
|----|-----------|-------------|-------|
| US-O01 | As an agent, I want to verify a customer's MyKad via JPN to confirm their identity | Conditional STP | MVP |
| US-O02 | As an agent, I want to perform biometric (thumbprint) match-on-card for identity verification | Conditional STP | MVP |
| US-O03 | As the system, I want to auto-approve KYC when match=YES AND AML=CLEAN AND age>=18, or queue for manual review | Conditional STP | MVP |
| US-O04 | As an agent, I want to open accounts for new and existing customers via MyKad | Conditional STP | Phase 2 |
| US-O05 | As the system, I want to enforce a 30-day probationary monitoring period for STP-approved accounts to detect synthetic identity fraud | Conditional STP | Phase 2 |

### Bill Payments
| ID | User Story | STP Category | Phase |
|----|-----------|-------------|-------|
| US-B01 | As a customer, I want to pay utility bills (JomPAY) via cash or card with automatic routing to biller | 100% STP | Phase 2 |
| US-B02 | As a customer, I want to pay ASTRO RPN bills via cash or card | 100% STP | Phase 2 |
| US-B03 | As a customer, I want to pay TM RPN bills via cash or card | 100% STP | Phase 2 |
| US-B04 | As a customer, I want to pay EPF i-SARAAN/i-SURI/SELF EMPLOYED via cash or card | 100% STP | Phase 2 |
| US-B05 | As the system, I want to validate Ref-1 against biller DB before payment | 100% STP | Phase 2 |

### Prepaid Top-up
| ID | User Story | STP Category | Phase |
|----|-----------|-------------|-------|
| US-T01 | As a customer, I want to top-up CELCOM prepaid via cash or card | 100% STP | Phase 2 |
| US-T02 | As a customer, I want to top-up M1 prepaid via cash or card | 100% STP | Phase 2 |
| US-T03 | As the system, I want to validate phone number against telco before top-up | 100% STP | Phase 2 |

### DuitNow & JomPAY
| ID | User Story | STP Category | Phase |
|----|-----------|-------------|-------|
| US-D01 | As a customer, I want to transfer funds via DuitNow (mobile, MyKad, BRN proxies) with real-time settlement | 100% STP | Phase 2 |
| US-D02 | As a customer, I want to make JomPAY payments (ON-US and OFF-US) via cash or card | 100% STP | Phase 2 |

### e-Wallet & eSSP
| ID | User Story | STP Category | Phase |
|----|-----------|-------------|-------|
| US-W01 | As a customer, I want to withdraw from Sarawak Pay e-Wallet via cash or card | 100% STP | Phase 2 |
| US-W02 | As a customer, I want to top-up Sarawak Pay e-Wallet via cash or card | 100% STP | Phase 2 |
| US-E01 | As a customer, I want to purchase eSSP certificates (BSN) via cash or card | 100% STP | Phase 2 |

### Agent Onboarding
| ID | User Story | STP Category | Phase |
|----|-----------|-------------|-------|
| US-A01 | As a prospective Micro-Agent, I want to self-onboard via the POS app (OCR + Liveness + SSM API) with auto-approval if all checks pass | Conditional STP | Phase 2 |
| US-A02 | As a prospective Standard/Premier Agent, I want my application routed to human bank officers for physical verification and approval | Non-STP | Phase 2 |

### Reversals & Disputes
| ID | User Story | STP Category | Phase |
|----|-----------|-------------|-------|
| US-V01 | As the system, I want to trigger an MTI 0400 reversal if a network timeout occurs after float was blocked (Store & Forward) | 100% STP | MVP |
| US-V02 | As a bank operator (Maker), I want to investigate disputed transactions (double-deduction claims) and propose manual adjustments | Non-STP | Phase 2 |
| US-V03 | As a bank operator (Checker), I want to review proposed dispute resolutions and approve/reject with evidence | Non-STP | Phase 2 |

### Merchant Services
| ID | User Story | STP Category | Phase |
|----|-----------|-------------|-------|
| US-M01 | As a customer, I want to pay for goods at the agent's shop using debit card or DuitNow QR so the agent's float is credited instantly | 100% STP | Phase 2 |
| US-M02 | As an agent (merchant), I want to sell digital PIN vouchers to customers for cash so I earn commission | 100% STP | Phase 2 |
| US-M03 | As a customer, I want to make cashless retail purchases at an agent location without carrying cash | 100% STP | Phase 2 |

### AML/Fraud & Velocity
| ID | User Story | STP Category | Phase |
|----|-----------|-------------|-------|
| US-EFM01 | As the system, I want to freeze an agent terminal instantly if the velocity engine detects structuring (smurfing) patterns | Non-STP | Phase 2 |
| US-EFM02 | As a bank operator (Checker), I want to review frozen terminals, unfreeze or file an STR to BNM | Non-STP | Phase 2 |

### EOD Settlement & Reconciliation
| ID | User Story | STP Category | Phase |
|----|-----------|-------------|-------|
| US-SM01 | As the system, I want to calculate net settlement per agent at EOD as (Withdrawals + Commissions) - (Deposits + Bill Payments) | N/A | Phase 2 |
| US-SM02 | As the system, I want to generate a settlement file for CBS upload at EOD | N/A | Phase 2 |
| US-SM03 | As the system, I want to reconcile internal ledger against PayNet PSR to identify Ghost/Orphan transactions | N/A | Phase 2 |
| US-SM04 | As the system, I want to pause settlement for agents with unresolved discrepancies until the issue is resolved | Non-STP | Phase 2 |

### Discrepancy Resolution
| ID | User Story | STP Category | Phase |
|----|-----------|-------------|-------|
| US-DR01 | As a bank operator (Maker), I want to investigate discrepancies (Ghost/Orphan) and propose manual adjustments with reason codes | Non-STP | Phase 2 |
| US-DR02 | As a bank operator (Checker), I want to review Maker's proposed adjustment, compare evidence, and Approve/Reject | Non-STP | Phase 2 |
| US-DR03 | As the system, I want to enforce that Maker and Checker are different user IDs (Four-Eyes Principle) | Non-STP | Phase 2 |

### API Gateway & Backoffice
| ID | User Story | STP Category | Phase |
|----|-----------|-------------|-------|
| US-G01 | As a POS terminal, I want to send all requests through a single API Gateway endpoint | N/A | MVP |
| US-G02 | As the system, I want to authenticate all external API requests via token-based auth | N/A | MVP |
| US-BO01 | As a bank operator, I want to create/edit/deactivate agent accounts with tier assignment | N/A | MVP |
| US-BO02 | As a bank operator, I want to monitor real-time transaction activity | N/A | MVP |
| US-BO03 | As a bank operator, I want to review settlement reports | N/A | MVP |
| US-BO04 | As a bank operator, I want to manage system configuration (fees, limits, rules) | N/A | Phase 2 |
| US-BO05 | As a bank operator, I want to view audit logs and error investigation | N/A | Phase 2 |
| US-BO06 | As a bank operator, I want to manage the discrepancy resolution dashboard (Ghost/Orphan queue) | Non-STP | Phase 2 |

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
| FR-3.4 | System shall trigger MTI 0400 Reversal if terminal printer fails or network drops after switch approval | US-L05, US-L06, US-V01 |
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

### FR-14: STP Processing
| ID | Requirement | US |
|----|------------|-----|
| FR-14.1 | System shall classify each transaction type into one of three STP categories: 100% STP, Conditional STP, or Non-STP | US-S01, US-S02, US-S03 |
| FR-14.2 | 100% STP transactions shall process end-to-end with zero human intervention, relying solely on cryptographic proof (PIN, biometric) | US-S01 |
| FR-14.3 | Conditional STP transactions shall be evaluated by the Rules Engine and auto-approve only if all criteria pass; otherwise shall queue for manual review | US-S02 |
| FR-14.4 | Non-STP transactions shall require human Maker-Checker workflow with RBAC enforcement (Maker != Checker) | US-S03 |
| FR-14.5 | 100% STP transactions shall enforce hard velocity and volume limits at the API Gateway level (e.g., max RM 3,000 per transaction, max 5 transactions per hour per customer) | US-S01, US-R03 |

### FR-15: Merchant Services
| ID | Requirement | US |
|----|------------|-----|
| FR-15.1 | System shall process retail card/QR purchases where agent acts as merchant, crediting agent float instantly minus MDR fee | US-M01 |
| FR-15.2 | System shall support PIN purchase (digital voucher) where agent pays from float and receives physical cash from customer | US-M02 |
| FR-15.3 | System shall differentiate transaction type RETAIL_SALE (float increases via MDR) from PIN_PURCHASE (float decreases via commission) | US-M01, US-M02 |
| FR-15.4 | System shall calculate Merchant Discount Rate (MDR) for retail sales and Agent Commission for PIN purchases separately | US-M01, US-M02 |
| FR-15.5 | System shall support hybrid cash-back flow: single card swipe covering both product purchase and cash withdrawal with split accounting | US-M03 |

### FR-16: EOD Net Settlement
| ID | Requirement | US |
|----|------------|-----|
| FR-16.1 | System shall calculate net settlement per agent at 23:59:59 MYT using formula: (Withdrawals + Commissions + RetailSales) - (Deposits + BillPayments) | US-SM01 |
| FR-16.2 | If net result is positive, system shall mark as "Bank owes Agent" (Direct Credit); if negative, "Agent owes Bank" (Direct Debit) | US-SM01 |
| FR-16.3 | System shall generate a standardized settlement file (CSV/flat file) for CBS upload by 02:00 AM | US-SM02 |
| FR-16.4 | All transactions for the business day must be in a final state (SUCCESS, FAILED, or REVERSED) before settlement calculation begins | US-SM01 |
| FR-16.5 | Float top-ups (prefunding) shall be excluded from the daily net settlement calculation as they are settled in real-time | US-SM01 |

### FR-17: Reconciliation & Discrepancy Resolution
| ID | Requirement | US |
|----|------------|-----|
| FR-17.1 | System shall reconcile internal ledger against PayNet PSR using Triple-Match Logic (Internal Ledger + Terminal Journal + Network Statement) | US-SM03 |
| FR-17.2 | System shall categorize discrepancies as: Ghost (Internal Only), Orphan (PayNet Only), or Mismatch (Amount Differs) | US-SM03, US-DR01 |
| FR-17.3 | System shall pause settlement for agents with unresolved discrepancies until issue is resolved | US-SM04 |
| FR-17.4 | Maker shall propose adjustment with mandatory reason code and evidence attachment | US-DR01 |
| FR-17.5 | Checker shall review Maker's proposed adjustment and Approve or Reject; only upon Approval shall the Ledger Service update the balance | US-DR02 |
| FR-17.6 | System shall enforce Four-Eyes Principle: Maker and Checker must be different user IDs | US-DR03 |
| FR-17.7 | All discrepancy resolutions shall create a Manual Adjustment Entry in the ledger (original broken transaction is never deleted, only corrected by secondary entry) | US-DR01, US-DR02 |
| FR-17.8 | Resolution deadline: all discrepancies should be resolved by 10:00 AM the following business day | US-SM04 |

### FR-18: Reversals & Disputes
| ID | Requirement | US |
|----|------------|-----|
| FR-18.1 | System shall trigger MTI 0400 reversal automatically if network timeout occurs after float was blocked (Store & Forward) | US-V01 |
| FR-18.2 | Failed reversal messages shall be persisted in a Store & Forward queue and retried every 60 seconds until SUCCESS from Switch | US-V01 |
| FR-18.3 | Transaction disputes (double-deduction claims) shall route to Non-STP Maker-Checker workflow | US-V02, US-V03 |
| FR-18.4 | Financial authorizations shall use ZERO retries at the Orchestrator level; timeout shall trigger immediate reversal | US-V01 |
| FR-18.5 | Non-financial requests (echo, inquiry) shall use exponential backoff retry (1s, 2s, 4s) with max 3 retries | N/A |

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

### ENT-9: MerchantTransaction
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| merchantTxnId | UUID | Yes | Unique merchant transaction identifier |
| transactionId | UUID (FK) | Yes | References Transaction.transactionId |
| agentId | UUID (FK) | Yes | References Agent.agentId |
| merchantType | Enum | Yes | RETAIL_SALE, PIN_PURCHASE |
| customerPanMasked | String(19) | Conditional | Masked PAN for card payments |
| customerDuitNowProxy | String(20) | Conditional | DuitNow proxy (mobile/NRIC) for QR payments |
| productDescription | String(200) | Conditional | Description of goods/services sold |
| mdrRate | BigDecimal(6,4) | Conditional | MDR rate applied (for RETAIL_SALE) |
| mdrAmount | BigDecimal(15,2) | Conditional | MDR amount deducted |
| grossAmount | BigDecimal(15,2) | Yes | Gross transaction amount |
| netCreditToFloat | BigDecimal(15,2) | Yes | Net amount credited to agent float (gross - MDR for sales, or 0 - commission for PIN) |
| receiptType | Enum | Yes | SALES_RECEIPT, PIN_SLIP |
| createdAt | Timestamp | Yes | Transaction timestamp |

### ENT-10: SettlementSummary
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| settlementId | UUID | Yes | Unique settlement record identifier |
| agentId | UUID (FK) | Yes | References Agent.agentId |
| businessDate | Date | Yes | Business date (cutoff 23:59:59 MYT) |
| totalWithdrawals | BigDecimal(15,2) | Yes | Sum of all successful cash withdrawals |
| totalDeposits | BigDecimal(15,2) | Yes | Sum of all successful cash deposits |
| totalBillPayments | BigDecimal(15,2) | Yes | Sum of all successful bill payments |
| totalCommissions | BigDecimal(15,2) | Yes | Sum of all commissions earned |
| totalRetailSales | BigDecimal(15,2) | Yes | Sum of all net retail sales (gross - MDR) |
| netSettlementAmount | BigDecimal(15,2) | Yes | (Withdrawals + Commissions + RetailSales) - (Deposits + BillPayments) |
| settlementDirection | Enum | Yes | BANK_OWES_AGENT, AGENT_OWES_BANK |
| status | Enum | Yes | PENDING, SETTLED, DISPUTED, HELD |
| settledAt | Timestamp | Conditional | Settlement completion timestamp |
| cbsFileGenerated | Boolean | Yes | Whether CBS file was generated |

### ENT-11: DiscrepancyCase
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| caseId | UUID | Yes | Unique discrepancy case identifier |
| transactionId | UUID (FK) | Yes | References Transaction.transactionId |
| agentId | UUID (FK) | Yes | References Agent.agentId |
| discrepancyType | Enum | Yes | GHOST, ORPHAN, MISMATCH |
| internalStatus | String(20) | Conditional | Transaction status in internal ledger |
| paynetStatus | String(20) | Conditional | Transaction status in PayNet PSR |
| internalAmount | BigDecimal(15,2) | Conditional | Amount in internal ledger |
| paynetAmount | BigDecimal(15,2) | Conditional | Amount in PayNet PSR |
| makerId | String(100) | Conditional | Maker who proposed adjustment |
| makerAction | Enum | Conditional | FORCE_SUCCESS, FORCE_REVERSE, MARK_AS_DUPLICATE |
| reasonCode | String(50) | Conditional | SYSTEM_TIMEOUT, NETWORK_FAILURE, POS_SYNC_ISSUE, MANUAL_OVERRIDE |
| evidenceUrl | String(500) | Conditional | URL to uploaded evidence (PayNet PSR screenshot) |
| checkerId | String(100) | Conditional | Checker who reviewed |
| checkerAction | Enum | Conditional | APPROVED, REJECTED |
| checkerComment | String(500) | Conditional | Checker's comment |
| status | Enum | Yes | PENDING_MAKER, PENDING_CHECKER, RESOLVED, CANCELLED |
| createdAt | Timestamp | Yes | Case creation timestamp |
| resolvedAt | Timestamp | Conditional | Case resolution timestamp |

### ENT-12: ManualAdjustmentEntry
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| adjustmentId | UUID | Yes | Unique adjustment identifier |
| caseId | UUID (FK) | Yes | References DiscrepancyCase.caseId |
| transactionId | UUID (FK) | Yes | References the original broken transaction |
| agentId | UUID (FK) | Yes | References Agent.agentId |
| adjustmentType | Enum | Yes | CREDIT_AGENT, DEBIT_AGENT |
| amount | BigDecimal(15,2) | Yes | Adjustment amount |
| accountCode | String(20) | Yes | Chart of accounts code for the adjustment entry |
| approvedBy | String(100) | Yes | Checker user ID who approved |
| createdAt | Timestamp | Yes | Adjustment entry timestamp |

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
| A-4 | **Real-time virtual float settlement** — each transaction posts journal entries and updates agent virtual balance immediately |
| A-5 | **EOD net settlement batch** — at 23:59:59 MYT, the system aggregates daily activity into a single net figure per agent and generates a settlement file for CBS upload. Actual money movement to the agent's real bank account happens via this EOD batch |
| A-6 | Three agent tiers: Micro, Standard, Premier |
| A-7 | Backoffice UI framework is not yet decided — to be determined in Design phase |
| A-8 | Infrastructure (Kubernetes, CI/CD) is out of scope for initial specs |

---

## 7. Traceability Matrix: User Stories → Functional Requirements

### Existing Stories (Revised)
| User Story | Functional Requirements | STP Category | Phase |
|-----------|------------------------|-------------|-------|
| US-R01 | FR-1.1 | N/A | MVP |
| US-R02 | FR-1.2 | N/A | MVP |
| US-R03 | FR-1.3, FR-14.5 | N/A | MVP |
| US-R04 | FR-1.4 | N/A | MVP |
| US-L01 | FR-2.1, FR-5.2 | N/A | MVP |
| US-L02 | FR-2.2 | N/A | MVP |
| US-L03 | FR-2.1, FR-2.3 | N/A | MVP |
| US-L04 | FR-5.1 | 100% STP | MVP |
| US-L05 | FR-2.4, FR-3.1, FR-3.3, FR-3.4, FR-3.5, FR-18.1 | 100% STP | MVP |
| US-L06 | FR-3.2, FR-3.3, FR-3.4, FR-3.5 | 100% STP | Phase 2 |
| US-L07 | FR-2.5, FR-4.1, FR-4.3 | 100% STP | MVP |
| US-L08 | FR-4.2, FR-4.3 | 100% STP | Phase 2 |
| US-O01 | FR-6.1 | Conditional STP | MVP |
| US-O02 | FR-6.2 | Conditional STP | MVP |
| US-O03 | FR-6.3, FR-6.4 | Conditional STP | MVP |
| US-O04 | FR-6.5 | Conditional STP | Phase 2 |
| US-O05 | FR-6.3 | Conditional STP | Phase 2 |
| US-B01 | FR-7.1 | 100% STP | Phase 2 |
| US-B02 | FR-7.2 | 100% STP | Phase 2 |
| US-B03 | FR-7.3 | 100% STP | Phase 2 |
| US-B04 | FR-7.4 | 100% STP | Phase 2 |
| US-B05 | FR-7.5 | 100% STP | Phase 2 |
| US-T01 | FR-8.1 | 100% STP | Phase 2 |
| US-T02 | FR-8.2 | 100% STP | Phase 2 |
| US-T03 | FR-8.3 | 100% STP | Phase 2 |
| US-D01 | FR-9.1, FR-9.2, FR-9.3 | 100% STP | Phase 2 |
| US-D02 | FR-7.1 | 100% STP | Phase 2 |
| US-W01 | FR-10.1 | 100% STP | Phase 2 |
| US-W02 | FR-10.2 | 100% STP | Phase 2 |
| US-E01 | FR-10.3 | 100% STP | Phase 2 |
| US-G01 | FR-12.1, FR-12.3 | N/A | MVP |
| US-G02 | FR-12.2 | N/A | MVP |
| US-BO01 | FR-13.1 | N/A | MVP |
| US-BO02 | FR-13.2 | N/A | MVP |
| US-BO03 | FR-13.3 | N/A | MVP |
| US-BO04 | FR-13.4 | N/A | Phase 2 |
| US-BO05 | FR-13.5 | N/A | Phase 2 |

### New Stories
| User Story | Functional Requirements | STP Category | Phase |
|-----------|------------------------|-------------|-------|
| US-A01 | FR-6.1, FR-6.2, FR-6.3, FR-14.3 | Conditional STP | Phase 2 |
| US-A02 | FR-14.4 | Non-STP | Phase 2 |
| US-V01 | FR-18.1, FR-18.2, FR-18.4 | 100% STP | MVP |
| US-V02 | FR-18.3 | Non-STP | Phase 2 |
| US-V03 | FR-18.3 | Non-STP | Phase 2 |
| US-M01 | FR-15.1, FR-15.3, FR-15.4 | 100% STP | Phase 2 |
| US-M02 | FR-15.2, FR-15.3, FR-15.4 | 100% STP | Phase 2 |
| US-M03 | FR-15.5 | 100% STP | Phase 2 |
| US-EFM01 | FR-14.5 | Non-STP | Phase 2 |
| US-EFM02 | FR-14.4 | Non-STP | Phase 2 |
| US-SM01 | FR-16.1, FR-16.2, FR-16.4, FR-16.5 | N/A | Phase 2 |
| US-SM02 | FR-16.3 | N/A | Phase 2 |
| US-SM03 | FR-17.1, FR-17.2 | N/A | Phase 2 |
| US-SM04 | FR-17.3 | Non-STP | Phase 2 |
| US-DR01 | FR-17.4, FR-17.7 | Non-STP | Phase 2 |
| US-DR02 | FR-17.5 | Non-STP | Phase 2 |
| US-DR03 | FR-17.6 | Non-STP | Phase 2 |
| US-BO06 | FR-13.5 | Non-STP | Phase 2 |
| US-S01 | FR-14.1, FR-14.2, FR-14.5 | 100% STP | MVP |
| US-S02 | FR-14.1, FR-14.3 | Conditional STP | Phase 2 |
| US-S03 | FR-14.1, FR-14.4 | Non-STP | Phase 2 |
