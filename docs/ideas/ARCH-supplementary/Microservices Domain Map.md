# **ARCHITECTURAL MAP**

## Microservices Domain Map

Internal & External Interface dependencies for all Business Core Tier - services below:
1.  **Onboarding Service:** Handles e-KYC, MyKad verification, and JPN/SSM integration.
2.  **Ledger & Float Service:** Manages agent virtual wallets and transaction history. **(Mission Critical)**.
3.  **Switch Adapter Service:** Translates internal JSON to ISO 8583 (Card) and ISO 20022 (DuitNow).
4.  **Biller Service:** Manages utility payments and aggregator (Fiuu/JomPAY) webhooks.
5.  **Rules & Parameter Service:** Centralized engine for fees, limits, and anti-smurfing (Velocity) checks.
6. **Transaction Orchestrator (Spring Boot):** Acts as the coordinator for complex financial flows. It uses **Spring Cloud OpenFeign** to communicate with the Ledger and Switch services.

---

## Suggested architectural map

This is a critical architectural map. In a high-stakes banking environment, your **Internal Interfaces** (Microservice-to-Microservice) ensure business logic consistency, while your **External Interfaces** (Service-to-Network) handle the actual movement of money and identity verification.

In a mature enterprise banking architecture, especially when dealing with legacy mainframes and ISO protocols, **Tier 3: The Translation Layer** (also known as the Integration or Anti-Corruption Layer) is essential.

This layer ensures that your **Spring Boot** services stay "clean" and only speak JSON, while Tier 3 handles the "heavy lifting" of binary protocols, socket management, and legacy security.

### Updated Architecture Hierarchy

* **Tier 1: Access Layer** (POS Terminal, Mobile App, API Gateway).
* **Tier 2: Business Core Tier** (Orchestrator, Ledger, Switch Adapter *Logic*, Biller *Logic*).
* **Tier 3: Translation Layer** (ISO Translator, CBS Connector, HSM Wrapper, Biller Gateway).
* **Tier 4: Legacy/External Systems** (PayNet, Core Banking, JomPAY).

---

This revised map provides the specific **Data Exchanged** for every connection, distinguishing strictly between your internal microservice mesh and the external Malaysian financial ecosystem. 

---

### 1. Onboarding Service
*The gateway for identity and business verification.*

| Interface Type | Connected System | Protocol | Data Exchanged |
| :--- | :--- | :--- | :--- |
| **Internal** | Rules & Parameter Service | Feign (Sync) | **Req:** Agent Type, Location. **Res:** KYC required fields, Age limits, Allowed business categories. |
| **Internal** | Transaction Orchestrator | Kafka (Async) | **Event:** `AGENT_READY`. **Payload:** Agent UUID, Default Tier, Branch Code. |
| **External** | **JPN (National ID)** | SOAP/REST | **Req:** NRIC, Biometric Template. **Res:** Match Score, Full Name, Address, Photo URL. |
| **External** | **SSM (Business Reg)** | REST | **Req:** SSM Number. **Res:** Registration Status, Director Names, Nature of Business. |
| **Downstream** | **Core Banking (CBS)** | REST/MQ | **Req:** NRIC. **Res:** Existing Customer Information (CIS), Internal Risk Rating (AML/CFT). |

---

### 2. Ledger & Float Service (Tier 2 Business)
*The source of truth for all balances. It never talks to the internet directly.*
Manages the virtual books. It only talks to the "Real" bank books at the end of the day via a connector.

| Interface Type | Connected System | Protocol | Data Exchanged |
| :--- | :--- | :--- | :--- |
| **Internal (Peer)** | Transaction Orchestrator | Feign (Sync) | **Req/Res:** Float Block/Commit commands (JSON). |
| **Internal** | Rules Service | Feign (Sync) | **Req:** Agent ID, Txn Amount. **Res:** Velocity Check Result (e.g., "Daily Limit RM 50k Exceeded"). |
| **Downstream (Tier 3)**| **CBS Connector** | REST / MQ | **Req:** Real-time Balance Inquiry. **Res:** Account Status (JSON). |
| **Downstream (Tier 3)**| **Batch File Generator** | Local File System | **Outbound:** Raw Transaction CSV for EOD Settlement. |

---

### 3. Switch Adapter Service
*The protocol translator for cards and interbank rails.*

This service decides **which** message to send and handles the Saga state, but it doesn't know "how" to format a binary ISO message.

| Interface Type | Connected System | Protocol | Data Exchanged |
| :--- | :--- | :--- | :--- |
| **Internal (Peer)** | Transaction Orchestrator | Feign (Sync) | **Req:** Txn Data (Amount, PAN, PIN). **Res:** Network Approval/Decline. |
| **Downstream (Tier 3)**| **ISO Translation Engine** | gRPC / REST | **Req:** Transaction JSON. **Res:** Decoded ISO Response (JSON format). |

---

### 4. Biller Service
*The hub for JomPAY and utility collections.*

| Interface Type | Connected System | Protocol | Data Exchanged |
| :--- | :--- | :--- | :--- |
| **Internal** | Transaction Orchestrator | Feign (Sync) | **Req:** Biller Code, Ref-1. **Res:** Validation Success, Biller Name, Outstanding Balance (if available). |
| **Internal** | Rules Service | Feign (Sync) | **Req:** Biller ID. **Res:** Convenience Fee (e.g., "TNB = RM 0.00, Astro = RM 1.00"). |
| **External** | **JomPAY (PayNet)** | REST / XML | **Req:** Bill Account Number (Ref-1). **Res:** Validation Status, Biller Status Code. |
| **External** | **Fiuu / Aggregators** | REST API | **Req:** Product ID (Mobile Reload). **Res:** 16-digit PIN code or Instant Top-up status. |

---

### 5. Rules & Parameter Service
*The "Configuration Engine" for all other services.*

| Interface Type | Connected System | Protocol | Data Exchanged |
| :--- | :--- | :--- | :--- |
| **Internal** | All Services | Feign (Sync) | **Req:** Parameter Key (e.g., `WITHDRAW_FEE_TIER_A`). **Res:** Value (e.g., `1.00`). |
| **Internal** | Admin Dashboard | REST (Inbound) | **Req:** Update Fee to `2.00`. **Res:** Update Success, Audit Log ID. |

---

### 6. Transaction Orchestrator
*The coordinator of the multi-step financial flow.*

| Interface Type | Connected System | Protocol | Data Exchanged |
| :--- | :--- | :--- | :--- |
| **Internal** | Ledger, Switch, Biller | Feign (Sync) | **Outbound:** Commands to execute parts of the flow. **Inbound:** Results to decide next step. |
| **External** | **Notification Gateway** | REST API | **Req:** Mobile Number, Message Template. **Res:** SMS/Push Delivery Status (Success/Fail). |

---

### Interface Summary Table

| Service | Primary Internal Peer | Primary External/Downstream Target | Protocol |
| :--- | :--- | :--- | :--- |
| **Onboarding** | Rules Service | JPN / SSM / Core Banking | REST / SOAP |
| **Ledger** | Orchestrator | Core Banking (EOD Settlement) | REST / File-Drop |
| **Switch** | Orchestrator | PayNet / Card Switch / HSM | ISO 8583 / 20022 |
| **Biller** | Rules Service | JomPAY / Fiuu | REST / Webhook |
| **Orchestrator** | All Core Services | Notification Gateway (SMS/Push) | REST / Feign |


### Critical Architectural Tip:
When you implement this in **Google Antigravity**, ensure that your **Switch Adapter** and **Biller Service** use **Circuit Breakers**. If PayNet or JomPAY is slow, you don't want the Orchestrator to hang and keep the Customer's funds in a "Locked" state.


### Why this structure prevents "Drift" in Antigravity:
By defining the **Data Exchanged** columns, you are giving the AI the exact "Contract" it needs to follow. For example, if it tries to make the **Ledger Service** talk to **PayNet**, you can point to this table and say: *"No, only the Switch Adapter talks to PayNet. The Ledger only talks to the Orchestrator and the CBS via Batch."*

## Services from other tiers

---

### 1. ISO Translation Engine (Tier 3 Translation)
This is the specialized "Translator" that converts your modern data into legacy banking "speak."

| Interface Type | Connected System | Protocol | Data Exchanged |
| :--- | :--- | :--- | :--- |
| **Upstream (Tier 2)** | Switch Adapter Service | gRPC / REST | **Inbound:** Transaction JSON. **Outbound:** Decoded JSON Response. |
| **Internal (Security)**| **HSM Wrapper (Tier 3)** | TCP Socket | **Req:** Encrypted PIN Block (ZPK). **Res:** Translated PIN Block (LMK). |
| **External (Tier 4)** | **PayNet / Card Switch** | **ISO 8583 / 20022** | **Outbound:** Binary Bitmaps (MTI 0200). **Inbound:** Network Response (MTI 0210). |

---

### 2. CBS Connector (Tier 3 Translation)
Protects the Business Core from the complexity of the Core Banking System's (CBS) legacy interface (often COBOL or SOAP-based).

| Interface Type | Connected System | Protocol | Data Exchanged |
| :--- | :--- | :--- | :--- |
| **Upstream (Tier 2)** | Ledger / Onboarding | REST / JSON | **Inbound:** JSON Account Request. **Outbound:** Unified JSON Response. |
| **External (Tier 4)** | **Core Banking (CBS)** | **SOAP / MQ / Fixed-Length** | **Outbound:** Legacy XML/Fixed-Format. **Inbound:** CBS Response String. |

---

### 3. Biller Gateway (Tier 3 Translation)
Handles the diverse and often messy connections to different biller aggregators.

| Interface Type | Connected System | Protocol | Data Exchanged |
| :--- | :--- | :--- | :--- |
| **Upstream (Tier 2)** | Biller Service | Feign (Sync) | **Inbound:** Biller ID & Ref-1. **Outbound:** Account Name & Balance. |
| **External (Tier 4)** | **JomPAY / Fiuu / TNB** | **Custom XML / REST** | **Outbound:** Biller-specific payload. **Inbound:** Provider-specific Response. |

---

### Why this Tier 3 is a Life-Saver:
1.  **Protocol Isolation:** If PayNet upgrades from ISO 8583 to ISO 20022, you **only** update the Tier 3 Translation Engine. Your Business Core services don't change at all.
2.  **Security:** Your Tier 2 services never touch a raw PIN or an HSM key. Tier 3 acts as a "Security DMZ," handling the sensitive encryption logic in isolation.
3.  **Stability:** Tier 3 can handle "Legacy Retries." If the Core Banking SOAP service is slow, the Tier 3 Connector can manage the wait-time/retries without blocking your Orchestrator's threads.

