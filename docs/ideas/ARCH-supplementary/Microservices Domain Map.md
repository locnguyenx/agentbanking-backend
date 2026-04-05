# **ARCHITECTURAL MAP**

## Microservices Domain Map

We use **Domain-Driven Design (DDD)** - Hexagonal Architecture, to ensure each microservice is decoupled and independently scalable.

### List of Microservices services

1. **Onboarding Service:** Handles e-KYC, MyKad verification, and JPN/SSM integration.
2. **Rules & Parameter Service:** Centralized engine for fees, limits, and anti-smurfing (Velocity) checks.
3. **Ledger & Float Service:** Manages agent virtual wallets and transaction history. **(Mission Critical)**. Need to maintain an immutable audit log of every balance change.
4. **Biller Service:** Manages utility payments and aggregator (Fiuu/JomPAY) webhooks.
5. **Transaction Orchestrator:** Acts as the coordinator for complex financial flows. 
6. **Switch Adapter Service:** translates internal JSON to ISO 8583 (Card) and ISO 20022 (DuitNow) via the ISO Translation Engine.
---

### 🌟 The Architectural Pattern Choices

**1. Onboarding Service**
**State Machine:** Implement a strict state machine (e.g., `INITIATED` $\rightarrow$ `OTP_VERIFIED` $\rightarrow$ `MYKAD_SCANNED` $\rightarrow$ `JPN_VALIDATED` $\rightarrow$ `LIVENESS_CHECKED` $\rightarrow$ `APPROVED`). Using Spring State Machine prevents "state skipping" (e.g., an agent trying to create a float without passing the JPN check) and provides an exact audit trail for BNM.

**2. Rules & Parameter Service**
**Standard CRUD + Redis for Velocity Checks:**
an enterprise Agent Banking platform requires a centralized Functional Parameter Management System (often called a Parameter Engine). This is a decoupled microservice equipped with a UI dashboard that allows authorized business and compliance users to tweak system behavior on the fly, safely.

Here is a comprehensive breakdown of the core parameter categories you need to manage, and the control framework required to keep the system secure.
1. Risk, Limit & Velocity Parameters

These parameters act as the real-time circuit breakers for your transaction processing. They should be configured at three cascading levels: Global, Tier-Based (e.g., Micro vs. Super Agent), and Individual Agent.

    Transaction Caps (Min/Max): The absolute minimum and maximum allowed for a single transaction type (e.g., Max Cash-In = RM 3,000; Min Bill Payment = RM 5).

    Cumulative Time-Bound Limits: Maximum aggregated volume allowed per day, per week, or per rolling 30 days.

    Velocity/Frequency Rules (Anti-Smurfing): The maximum number of identical transactions allowed within a specific time window (e.g., Max 3 cash withdrawals per customer account per 60 minutes).

    Agent Float Thresholds: The minimum balance an agent must maintain, and the "Low Balance" warning trigger percentage.

2. Tariff & Commission Parameters (Revenue Engine)

Fees constantly change based on vendor agreements, national switch mandates (like PayNet), and marketing campaigns. The engine must support date-effective versioning (e.g., "Set fee to RM 0 from Dec 1 to Dec 31, reverting to RM 1 on Jan 1").

    Customer Fees: What the end-user pays. Can be fixed (RM 1.00 per GIRO) or percentage-based (1% for e-wallet top-up).

    Merchant Discount Rate (MDR) Splits: How the backend revenue is divided. (e.g., JomPAY transaction: 50% to Agent, 10% to Switch, 40% to Bank).

    Tax Configurations: Toggleable tax variables like the Malaysian Sales and Service Tax (SST), which must be calculated and itemized on the receipt separate from the base fee.

3. STP & Workflow Decision Parameters

This is where you control the "Zero Intervention" rules engine we discussed previously. By parameterizing these values, Risk Officers can tighten or loosen the onboarding pipeline dynamically.

    Biometric/e-KYC Thresholds: The minimum acceptable facial liveness score (e.g., 95.0%) or ID OCR match percentage (e.g., 98.0%).

    SLA Timers: The exact countdown duration before a manual Maker-Checker task breaches its Service Level Agreement and triggers an escalation (e.g., 24 hours).

    Risk Score Cut-offs: The threshold for routing an application to the manual queue based on aggregated API data.

4. Technical & System Operations Parameters

These control how the POS hardware interacts with your backend.

    Switch Cut-Off Times: Daily operational deadlines for batching (e.g., IBG GIRO cut-off at 17:00:00). Transactions submitted at 17:00:01 are automatically stamped for T+1 processing.

    Session & Security Timers: POS idle logout time (e.g., 300 seconds) and OTP expiry duration (e.g., 60 seconds).

    Whitelisted App Versions: A parameter defining the minimum secure version of the Android POS app. If an agent's terminal sends an API request with an older version, the Gateway forces an OTA (Over-The-Air) update.

---

**3. Ledger & Float Service**
**Standard CRUD + Locking:** for a "Mission Critical" ledger, concurrent transactions are the worst enemy. If two customers try to withdraw from the same Agent's float at the exact same millisecond, system might let both read the same balance, resulting in a negative float. We must have a locking mechanism to mitigate this problem.

**4. Biller Service**
**Circuit Breakers:** If PayNet or JomPAY is slow, you don't want the Orchestrator to hang and keep the Customer's funds in a "Locked" state.

**5. Transaction Orchestrator**
**SATA pattern:** the Orchestrator should coordinate via an Event Bus (Kafka/RabbitMQ) or a dedicated Saga framework (like Temporal) instead of blocking HTTP calls.

* **CONSIDERATION:** If synchronous HTTP calls (OpenFeign) is used, we **must** wrap it in **Resilience4j** (Circuit Breakers and Retries) and implement Idempotency Keys on the called services.
---

### The Business Core Service Flow
For example:
If an Agent initiates a **DuitNow Transfer**, the flow should look like this:
1.  **Orchestrator** receives the request.
2.  **Orchestrator** calls **Rules Service** to check Velocity limits.
3.  **Orchestrator** initiates a **Saga**.
4.  **Orchestrator** sends an asynchronous command to **Ledger**: *"Reserve RM 100 (Pessimistic Lock)."*
5.  **Orchestrator** sends a command to **Switch Adapter**: *"Send ISO 20022 to PayNet."*
6.  If PayNet fails, **Orchestrator** executes Saga Compensation $\rightarrow$ Tells Ledger to *"Release RM 100 reservation."*
7.  **Micrometer** run quietly in the background on all 6 services, stitching the logs together for your dashboard.

---

# **SERVICE DEPENDENCIES**

This is a critical architectural map. In a high-stakes banking environment, your **Internal Interfaces** (Microservice-to-Microservice) ensure business logic consistency, while your **External Interfaces** (Service-to-Network) handle the actual movement of money and identity verification.

---

## Business Core - Tier 3
### 1. Onboarding Service
*The gateway for identity and business verification.*

| Interface Type | Connected System | Protocol | Data Exchanged |
| :--- | :--- | :--- | :--- |
| **Internal** | Rules & Parameter Service | Feign (Sync) | **Req:** Agent Type, Location. **Res:** KYC required fields, Age limits, Allowed business categories. |
| **Internal** | Transaction Orchestrator | Kafka (Async) | **Event:** `AGENT_READY`. **Payload:** Agent UUID, Default Tier, Branch Code. |
| **External** | **JPN (National ID)** | SOAP/REST | **Req:** NRIC, Biometric Template. **Res:** Match Score, Full Name, Address, Photo URL. |
| **External** | **SSM (Business Reg)** | REST | **Req:** SSM Number. **Res:** Registration Status, Director Names, Nature of Business. |
| **Downstream (The Translation Layer)**| **CBS Connector** | REST / MQ | **Req:** NRIC. **Res:** Existing Customer Information (CIS), Internal Risk Rating (AML/CFT). |

---

### 2. Ledger & Float Service
*The source of truth for all balances. It never talks to the internet directly.*
Manages the virtual books. It only talks to the "Real" bank books at the end of the day via a connector.

| Interface Type | Connected System | Protocol | Data Exchanged |
| :--- | :--- | :--- | :--- |
| **Internal (Peer)** | Transaction Orchestrator | Feign (Sync) | **Req/Res:** Float Block/Commit commands (JSON). |
| **Internal** | Rules Service | Feign (Sync) | **Req:** Agent ID, Txn Amount. **Res:** Velocity Check Result (e.g., "Daily Limit RM 50k Exceeded"). |
| **Downstream (The Translation Layer)**| **CBS Connector** | REST / MQ | **Req:** Real-time Balance Inquiry. **Res:** Account Status (JSON). |
| **Downstream**| **Batch File Generator** | Local File System | **Outbound:** Raw Transaction CSV for EOD Settlement. |

---

### 3. Biller Service
*The hub for JomPAY and utility collections.*

| Interface Type | Connected System | Protocol | Data Exchanged |
| :--- | :--- | :--- | :--- |
| **Internal** | Transaction Orchestrator | Feign (Sync) | **Req:** Biller Code, Ref-1. **Res:** Validation Success, Biller Name, Outstanding Balance (if available). |
| **Internal** | Rules Service | Feign (Sync) | **Req:** Biller ID. **Res:** Convenience Fee (e.g., "TNB = RM 0.00, Astro = RM 1.00"). |
| **Downstream (The Translation Layer)**| **Biller Gateway - JomPAY (PayNet)** | REST / Webhooks | **Req:** Bill Account Number (Ref-1). **Res:** Validation Status, Biller Status Code. |
| **Downstream (The Translation Layer)**| **Biller Gateway - Fiuu / Aggregators** | REST API | **Req:** Product ID (Mobile Reload). **Res:** 16-digit PIN code or Instant Top-up status. |

---

### 4. Rules & Parameter Service
*The "Configuration Engine" for all other services.*

| Interface Type | Connected System | Protocol | Data Exchanged |
| :--- | :--- | :--- | :--- |
| **Internal** | All Services | Feign (Sync) | **Req:** Parameter Key (e.g., `WITHDRAW_FEE_TIER_A`). **Res:** Value (e.g., `1.00`). |
| **Internal** | Admin Dashboard | REST (Inbound) | **Req:** Update Fee to `2.00`. **Res:** Update Success, Audit Log ID. |

---

### 5. Transaction Orchestrator
*The coordinator of the multi-step financial flow.*

| Interface Type | Connected System | Protocol | Data Exchanged |
| :--- | :--- | :--- | :--- |
| **Internal** | Ledger, Switch, Biller | Feign (Sync) | **Outbound:** Commands to execute parts of the flow. **Inbound:** Results to decide next step. |
| **External** | **Notification Gateway** | REST API | **Req:** Mobile Number, Message Template. **Res:** SMS/Push Delivery Status (Success/Fail). |

---

### 6. Switch Adapter Service
*The protocol translator for cards and interbank rails.*

This service is in charge of card / HSM processing, but it doesn't know "how" to format a binary ISO message.

| Interface Type | Connected System | Protocol | Data Exchanged |
| :--- | :--- | :--- | :--- |
| **Internal (Peer)** | Transaction Orchestrator | Feign (Sync) | **Req:** Txn Data (Amount, PAN, PIN). **Res:** Network Approval/Decline. |
| **Downstream (The Translation Layer)**| **ISO Translation Engine** | gRPC / REST | **Req:** Transaction JSON. **Res:** Decoded ISO Response (JSON format). |

---

### Interface Summary Table

| Service | Primary Internal Peer | Primary External/Downstream Target | Protocol |
| :--- | :--- | :--- | :--- |
| **Onboarding** | Rules Service | JPN / SSM / Core Banking | REST / SOAP |
| **Ledger** | Orchestrator | CBS Connector - Core Banking (EOD Settlement) | REST / File-Drop |
| **Rules & Parameter** | All Services | - | REST |
| **Biller** | Rules Service | Biller Gateway - JomPAY / Fiuu | REST / Webhook |
| **Orchestrator** | All Core Services | Notification Gateway (SMS/Push) | REST / Feign |
| **Switch** | Orchestrator | ISO Translation Engine / HSM | gRPC / REST |

## The Translation Layer - tier 4
In a mature enterprise banking architecture, especially when dealing with legacy mainframes and ISO protocols, **Tier 4: The Translation Layer** (also known as the Integration or Anti-Corruption Layer) is essential.

This layer ensures that your **Spring Boot** services stay "clean" and only speak JSON, while The Translation Layer handles the "heavy lifting" of binary protocols, socket management, and legacy security.

### 1. ISO Translation Engine
This is the specialized "Translator" that converts your modern data into legacy banking "speak."

| Interface Type | Connected System | Protocol | Data Exchanged |
| :--- | :--- | :--- | :--- |
| **Upstream** | Switch Adapter Service | gRPC / REST | **Inbound:** Transaction JSON. **Outbound:** Decoded JSON Response. |
| **External (Security)**| **HSM (Security Module)** | TCP Socket | **Req:** Encrypted PIN Block (ZPK). **Res:** Translated PIN Block (LMK). |
| **External** | **PayNet / Card Switch** | **ISO 8583 / 20022** | **Outbound:** Binary Bitmaps (MTI 0200). **Inbound:** Network Response (MTI 0210). |

### 2. CBS Connector
Protects the Business Core from the complexity of the Core Banking System's (CBS) legacy interface (often COBOL or SOAP-based).

| Interface Type | Connected System | Protocol | Data Exchanged |
| :--- | :--- | :--- | :--- |
| **Upstream (Business Core Tier)** | Ledger / Onboarding | REST / JSON | **Inbound:** JSON Account Request. **Outbound:** Unified JSON Response. |
| **External** | **Core Banking (CBS)** | **SOAP / MQ / Fixed-Length** | **Outbound:** Legacy XML/Fixed-Format. **Inbound:** CBS Response String. |

---

### 3. Biller Gateway
Handles the diverse and often messy connections to different biller aggregators.

| Interface Type | Connected System | Protocol | Data Exchanged |
| :--- | :--- | :--- | :--- |
| **Upstream (Business Core Tier)** | Biller Service | Feign (Sync) | **Inbound:** Biller ID & Ref-1. **Outbound:** Account Name & Balance. |
| **External** | **JomPAY / Fiuu / TNB** | **Custom XML / REST** | **Outbound:** Biller-specific payload. **Inbound:** Provider-specific Response. |

---

### Why this Tier 4 is a Life-Saver:
1.  **Protocol Isolation:** If PayNet upgrades from ISO 8583 to ISO 20022, you **only** update the Tier 4 Translation Layer. Your Business Core services don't change at all.
2.  **Security:** Your Business Core Tier services never touch a raw PIN or an HSM key. Tier 4 acts as a "Security DMZ," handling the sensitive encryption logic in isolation.
3.  **Stability:** Tier 4 can handle "Legacy Retries." If the Core Banking SOAP service is slow, the Tier 4 Connector can manage the wait-time/retries without blocking your Orchestrator's threads.
