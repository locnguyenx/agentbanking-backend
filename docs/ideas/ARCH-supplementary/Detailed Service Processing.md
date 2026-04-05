# **DETAILED SERVICE PROCESSING - BUSINESS CORE TIER**

To complete your **Business Core Tier** blueprint, here is the detailed functional breakdown for the remaining services. This ensures that every component in project has a defined "Job Description," making it much harder for the AI to hallucinate incorrect logic.

---

## **The Dual-Leg Nature of the Transaction**

In an Agent Banking architecture, we have a critical dimension: **The Dual-Leg Nature of the Transaction**, and **Value-Added Services (VAS)**. 

### The Concept: The "Dual-Leg" Transaction
In Agent Banking, a transaction never just hits the CBS or the External Switch. It **always** hits your Business Core Tier **Ledger & Float Service** first. 

Every transaction has two legs that must balance:
1.  **The Agent Leg:** Handled by your internal Agent Ledger (deducting or adding to the Agent's digital float).
2.  **The Customer/Target Leg:** Handled by either CBS (On-Us), PayNet (Off-Us), or a Biller.

---

Based on the boundary between internal Bank systems (CBS) and external networks (PayNet), following is the complete, refined grouping of transactions:

### Category 1: "On-Us" (Inside the Bank)
**Scenario:** A BSN customer interacts with a BSN Agent.
* **The Fund Source:** Customer's CASA (Current/Savings Account) at BSN.
* **Authentication Method:** MyKad (Biometric) or Mobile App OTP.
* **The Processing Path:**
    * **Agent Leg:** Orchestrator calls the internal `Ledger Service` to block/update the Agent Float.
    * **Customer Leg:** Orchestrator makes a direct API call to the **CBS (Core Banking System)** to instantly debit/credit the customer's account.
* **Special case:** If the On-Us customer uses a **BSN ATM Card** instead of MyKad, the Orchestrator usually routes this to the bank's **Card Management System (CMS) or internal EFT Switch** (like Base24 or Way4), *not* directly to the CBS. The CMS then talks to the CBS.

### Category 2: "Off-Us" / Interbank (Outside the Bank)
**Scenario:** A Maybank customer goes to a BSN Agent to withdraw cash or send money.
* **The Fund Source:** Customer's account at Maybank.
* **Authentication Method:** Maybank ATM Card (PIN) or DuitNow QR.
* **The Processing Path:**
    * **Agent Leg:** Orchestrator calls `Ledger Service` to block/update the Agent Float.
    * **Customer Leg:** Orchestrator sends the transaction to your **Switch Adapter Service**, which translates it to ISO 8583 / ISO 20022 and sends it to **PayNet**.
* **The Networks (Malaysia Context):**
    * **MEPS (ISO 8583):** Used if the customer inserts a Maybank ATM Card and enters a PIN.
    * **DuitNow / IBG (ISO 20022):** Used if the transaction is an account-to-account transfer or dynamic QR scan.

### Category 3: Value-Added Services / Billers (The Missing Category)
**Scenario:** A cash-paying walk-in customer (unbanked) goes to the Agent to pay their Tenaga Nasional Berhad (TNB) electricity bill or top up their Celcom prepaid.
* **The Fund Source:** Physical Cash (handed to the Agent). No customer bank account is involved.
* **The Processing Path:**
    * **Agent Leg:** Orchestrator calls `Ledger Service` to **debit (block)** the Agent's Float (since the Agent collected physical cash, they now owe the bank).
    * **Target Leg:** Orchestrator calls the **Biller Service**, which routes via API to aggregators (like Fiuu), direct Telco APIs, or **JomPAY** (via PayNet).
* **Why this matters:** This is often the highest volume transaction type for rural agents. It bypasses the CBS entirely during the day, only hitting the CBS during the End-of-Day (EOD) settlement when the Bank pays the Biller.

---

### Summary of the Orchestrator's Routing Logic

To visualize how your **Transaction Orchestrator** handles this, think of it as a router making decisions based on the transaction payload:

| Transaction Type | Who handles the Agent Leg? | Who handles the Customer/Target Leg? |
| :--- | :--- | :--- |
| **Cash Deposit (MyKad)** | Ledger Service (Business Core Tier) | **CBS API** (Directly credits BSN Account) |
| **Cash Withdrawal (BSN Card)**| Ledger Service (Business Core Tier) | **Internal CMS / Switch** (Validates PIN, debits BSN Account) |
| **Interbank Transfer (DuitNow)**| Ledger Service (Business Core Tier) | **Switch Adapter $\rightarrow$ PayNet** (DuitNow network) |
| **Bill Payment (JomPAY)** | Ledger Service (Business Core Tier) | **Biller Service $\rightarrow$ JomPAY / Aggregator** |

**Your key takeaway:** You didn't miss the external vs. internal concept, but you must remember that **CBS is only invoked if a specific BSN retail account is being debited or credited.** If it's a card, an external bank, or a utility bill, the Orchestrator routes the second leg to specialized adapters (CMS, PayNet, Biller APIs), while the Agent Float is managed purely within your new microservice architecture.

---

## 1. Onboarding Service
*The "Identity & Compliance" engine.*

### A. Digital KYC Verification (`VerifyIdentity`)
* **Purpose:** To verify that the person applying is a real citizen and matches their government ID.
* **Trigger:** Called by the **Agent App** when a new customer or agent is registered.
* **Business Validation Rules:** * **Age Check:** Applicant must be $\ge$ 18 years old.
    * **Duplicate Check:** NRIC must not already exist in the `active_agents` or `customers` table.
* **Processing:** 1.  Calls **JPN API** for Match-on-Card (biometric).
    2.  Calls **SSM API** (if Agent) to verify business registration.
    3.  Runs AML/CFT screening against the bank's internal blacklist.
* **Input/Output:** * *Input:* `NRIC`, `BiometricData`, `BusinessRegNo`.
    * *Output:* `KYC_Status (APPROVED/REJECTED/MANUAL_REVIEW)`.

### B. Agent Profile Activation (`ActivateAgent`)
* **Purpose:** To officially "Open for Business" an agent after they pass all checks.
* **Trigger:** Called by an **Admin User** (Checker) in the Admin Portal.
* **Processing:** 1.  Updates status to `ACTIVE`.
    2.  **Internal Event:** Publishes `AGENT_ACTIVATED` to Kafka. 
    3.  This event triggers the **Ledger Service** to initialize the float account at RM 0.00.
* **Input/Output:** * *Input:* `ApplicationID`, `ApproverID`.
    * *Output:* `AgentID`, `ActivationTimestamp`.

---

## 2. Switch Adapter Service (Business Core Tier)

* **Purpose:** To decide which external rail to use (e.g., PayNet vs. Internal Card Switch).
* **Trigger:** Called by the **Transaction Orchestrator** when a transaction requires external authorization.
* **Business Validation Rules:**
    * **Routing Logic:** Select the correct downstream Tier 4 engine based on `BIN` (Bank Identification Number) or `BillerCategory`.
    * **Duplicate Detection:** Check the `Idempotency-Key` to ensure the same request isn't sent to the network twice.
* **Processing:**
    1.  Maps the Orchestrator's generic JSON into a **Canonical Banking Request** (a standard JSON format your Tier 4 expects).
    2.  Calls the **Tier 4 ISO Translation Engine** (via gRPC or REST).
    3.  Handles "In-Flight" timeouts: If Tier 4 doesn't respond, the Switch Adapter initiates the "Reversal Strategy."
    4.  Translates the Tier 4 JSON response (e.g., `Result: 00`) into a Business Core Tier business status (e.g., `Status: APPROVED`).
* **Input / Output:**
    * **Input:** `Transaction_JSON` (Amount, PAN, PIN_Block, Terminal_ID).
    * **Output:** `Network_Response_JSON` (Approval_Code, RRN, Trace_Number).

---

## 3. Biller Service
*The "Utility Hub" for collections.*

### A. Account Inquiry (`ValidateBill`)
* **Purpose:** To ensure the customer is paying the correct bill and show the outstanding amount.
* **Trigger:** Called by the **Orchestrator** when an agent enters a Biller Code and Ref-1.
* **Business Validation Rules:** * **Format Check:** Ref-1 must match the biller's specific regex (e.g., TNB is 12 digits).
* **Processing:** 1.  Calls **Biller Gateway - JomPAY API** or the direct Biller API (e.g., Astro).
    2.  Retrieves the "Account Holder Name" for verification.
* **Input/Output:** * *Input:* `BillerCode`, `Ref1`.
    * *Output:* `AccountName`, `AmountDue`, `ValidationStatus`.

### B. Payment Notification (`NotifyBiller`)
* **Purpose:** To tell the utility provider that the money has been collected.
* **Trigger:** Called by the **Orchestrator** *after* the Ledger successfully commits the funds.
* **Processing:** 1.  Sends a "Payment Successful" callback to the Biller.
    2.  Retrieves the Biller's internal receipt number.
* **Input/Output:** * *Input:* `TxnID`, `AmountPaid`, `Timestamp`.
    * *Output:* `BillerReceiptRef`.

---

## 4. Rules & Parameter Service
*The "Brain" that holds the bank's settings.*

### A. Parameter Retrieval (`GetConfig`)
* **Purpose:** To provide real-time settings (fees, limits) to other services.
* **Trigger:** Called by **all services** during transaction processing.
* **Processing:** 1.  Checks **Redis Cache** first for speed.
    2.  If miss, queries **PostgreSQL** and updates Redis.
* **Input/Output:** * *Input:* `ConfigKey` (e.g., `MAX_WITHDRAW_DAILY`).
    * *Output:* `ConfigValue` (e.g., `5000.00`).

### B. Risk Scoring (`EvaluateVelocity`)
* **Purpose:** To detect fraud (e.g., "Smurfing") by checking transaction frequency.
* **Trigger:** Called by **Ledger** or **Orchestrator**.
* **Business Validation Rules:** * Max 3 withdrawals per NRIC per day.
    * Max RM 10,000 total per Agent per day.
* **Input/Output:** * *Input:* `AgentID/NRIC`, `CurrentTxnAmount`.
    * *Output:* `RiskStatus (PASS/FAIL)`.

---

## 5. Transaction Orchestrator (Temporal Workflow Engine)

Since we are using **Temporal** as the framework and handling **Dual-Leg Transactions** (Agent Leg + Target Leg), the Orchestrator is no longer just a simple generic "Saga runner." It is a highly intelligent **Workflow Engine and Router**. 

*The "Air Traffic Controller" of the bank.* Powered by Temporal, it manages the lifecycle, routing, and state of all Dual-Leg transactions. It contains no business logic regarding *how* to update a balance or parse an ISO message; its sole responsibility is ensuring that distributed steps execute reliably, in the correct order, and roll back safely if a failure occurs.

### A. Transaction Routing (`DetermineWorkflow`)
* **Purpose:** To analyze the incoming API request and route it to the correct specialized Temporal Workflow based on the "Target Leg" of the transaction.
* **Trigger:** Called by the **API Gateway** when an Agent initiates any financial transaction.
* **Processing:**
    1.  Inspects the `TransactionType` and `TargetBIN` (Bank Identification Number) or Biller Code.
    2.  **Routes to On-Us Workflow:** If Target == BSN CBS (e.g., MyKad Cash Deposit).
    3.  **Routes to Off-Us Workflow:** If Target == External Bank (e.g., DuitNow / MEPS).
    4.  **Routes to VAS Workflow:** If Target == JomPAY or Utility Biller.
* **Input/Output:** * *Input:* `ClientRequestJSON`
    * *Output:* `WorkflowExecutionID` (Instantly returned to the client so the mobile app can poll or listen via WebSockets for the final status).

### B. Dual-Leg Workflow Execution (`ExecuteTemporalWorkflow`)
* **Purpose:** To execute the specific step-by-step sequence required for the chosen transaction type, ensuring the Agent's Ledger and the External Target stay perfectly synced.
* **Trigger:** Invoked internally by the Router.
* **Processing (Example: Off-Us DuitNow Transfer):**
    1.  **Activity 1:** Call `RulesService` to verify Agent velocity limits.
    2.  **Activity 2:** Call `LedgerService.BlockFloat` to reserve funds. *(Registers `RollbackTransaction` as the compensation action).*
    3.  **Activity 3:** Call `SwitchAdapter.SendISO20022` to forward the request to PayNet.
    4.  **Activity 4:** Await asynchronous confirmation from PayNet.
    5.  **Activity 5:** Call `LedgerService.CommitTransaction` to finalize the accounting.
* **Input/Output:** * *Input:* `ParsedTransactionContext`
    * *Output:* `FinalResponseJSON` (Success or Failure).

### C. Automated Resilience & Compensation (`HandleFailure`)
* **Purpose:** To ensure the system remains "Clean" without manual intervention when networks drop, timeouts occur, or external systems decline a request.
* **Trigger:** Thrown exceptions, PayNet timeouts, or HTTP 500s from the CBS/Biller.
* **Processing:**
    1.  **Transient Errors:** Uses Temporal's native retry policies to automatically retry `Activity 3` (e.g., if the Switch Adapter briefly loses connection to PayNet).
    2.  **Terminal Errors:** If the Switch returns `HTTP 402 Declined` or retries are exhausted, Temporal automatically triggers the registered compensation.
    3.  **The Rollback:** Executes `LedgerService.RollbackTransaction` to unblock the Agent's float.
* **Input/Output:** * *Input:* `ActivityFailureException`
    * *Output:* `ReversalStatus` and `AuditLogEntry`.

### D. Human-in-the-Loop Signaling (`ManualIntervention`)
* **Purpose:** To handle rare edge cases where a transaction gets stuck in an ambiguous state (e.g., PayNet goes offline for 4 hours, and the Agent's float is stuck in `PENDING`).
* **Trigger:** An authorized Bank Admin clicks "Force Resolve" in the Backoffice UI.
* **Processing:**
    1.  Sends a **Temporal Signal** to the running, paused workflow.
    2.  The workflow intercepts the signal, logs the Admin's ID for audit purposes, and forces the workflow to either execute `Commit` or `Rollback` based on the Admin's command.
* **Input/Output:** * *Input:* `WorkflowID`, `AdminAction (COMMIT/REVERSE)`, `AdminID`.
    * *Output:* `WorkflowTerminatedStatus`.

---

This rewrite clearly defines the Orchestrator's role using modern workflow concepts, making it obvious to developers *where* the routing happens and *how* the Agent's ledger is protected.

---

## **6. Ledger & Float Service**

The **Ledger & Float Service** is the "Heart of the Bank" in your microservices architecture. While the Orchestrator manages the workflow, the Ledger Service is the only component allowed to physically "move" value between accounts and maintain the immutable audit trail.

Below is the comprehensive functional breakdown of the Ledger & Float Service.

---

### 1. Float Reservation (`BlockFloat`)
* **Purpose:** To prevent "Double Spending." It temporarily locks a specific amount from the Agent's Float while the transaction is being authorized by the External Switch (PayNet).
* **Trigger:** Called by the **Orchestrator** at the start of a "Cash-In," "Bill Pay," or "PIN Purchase" (transactions where the agent owes the bank).
* **Business Validation Rules:**
    * **Balance Check:** `Available Float >= (Txn Amount + Estimated Fee)`.
    * **Status Check:** Agent must be in `ACTIVE` status (not suspended or closed).
    * **Velocity Check:** Txn must not exceed `Daily_Max_Amount` or `Single_Txn_Limit` (queried from Rules Service).
* **Processing:**
    1.  Uses **Pessimistic Locking** (`SELECT FOR UPDATE`) on the `agent_floats` table.
    2.  Subtracts from `available_balance` but keeps `actual_balance` the same.
    3.  Creates a record in `transaction_history` with status `PENDING`.
* **Input/Output:** * *Input:* `AgentID`, `Amount`, `TxnType`, `CorrelationID`.
    * *Output:* `ReservationID`, `Status (SUCCESS/INSUFFICIENT_FUNDS)`.

---

### 2. Transaction Finalization (`CommitTransaction`)
* **Purpose:** To permanently move the reserved funds once the external authorization is successful.
* **Trigger:** Called by the **Orchestrator** after a "Success" response from the Switch Adapter.
* **Business Validation Rules:**
    * **Matching:** `ReservationID` must exist and be in `PENDING` state.
    * **Integrity:** The `Amount` must match the original reservation.
* **Processing:**
    1.  Updates `actual_balance` to match the new lower amount.
    2.  Moves `transaction_history` from `PENDING` to `SUCCESS`.
    3.  **Accounting Entry Generation:**
        * **Dr** (Debit): `Agent_Float_Liability_A/c`
        * **Cr** (Credit): `Bank_Control_A/c` (Funds moving to the Bank).
* **Input/Output:** * *Input:* `ReservationID`, `NetworkAuthCode`.
    * *Output:* `FinalTxnRef`, `NewActualBalance`.

---

### 3. Transaction Reversal (`RollbackTransaction`)
* **Purpose:** To release "Locked" funds back to the available balance if a transaction fails at the Switch or the Customer cancels.
* **Trigger:** Called by the **Orchestrator** on timeout, network error, or `DECLINED` response.
* **Business Validation Rules:**
    * Cannot rollback a transaction that is already `COMMITTED`.
* **Processing:**
    1.  Adds the reserved amount back to the `available_balance`.
    2.  Updates `transaction_history` to `FAILED` or `REVERSED`.
    3.  Creates an **Audit Log** entry for the failure reason.
* **Input/Output:** * *Input:* `ReservationID`, `ReasonCode`.
    * *Output:* `RollbackStatus`.

---

### 4. Direct Credit (`CreditAgentFloat`)
* **Purpose:** To handle "Cash-Out" (Withdrawals) where the Agent hands cash to the customer, so the Bank must credit the Agent’s digital float.
* **Trigger:** Called by the **Orchestrator** after a successful Card/PIN authentication for a Withdrawal.
* **Business Validation Rules:**
    * Check for `Max_Float_Limit` (to prevent an agent from accumulating too much digital value and not enough physical cash).
* **Processing:**
    1.  Instantly increases both `available_balance` and `actual_balance`.
    2.  **Accounting Entry:**
        * **Dr**: `Bank_Control_A/c`
        * **Cr**: `Agent_Float_Liability_A/c`
* **Input/Output:** * *Input:* `AgentID`, `Amount`, `AuthCode`.
    * *Output:* `SuccessStatus`, `UpdatedBalance`.

---

### 5. Commission Calculation & Accrual
* **Purpose:** To reward the agent for their service.
* **Trigger:** Automatically triggered inside the Ledger Service after a successful `CommitTransaction` or `CreditAgentFloat`.
* **Business Validation Rules:**
    * Commission rates are queried from the **Rules Service** based on `Agent_Tier`.
* **Processing:**
    1.  Calculates the split (e.g., Bank keeps RM 0.50, Agent gets RM 0.50).
    2.  Posts to a separate `agent_earnings` sub-ledger (not added to Float immediately; usually settled at EOD).
    3.  **Accounting Entry:**
        * **Dr**: `Bank_Commission_Expense_A/c`
        * **Cr**: `Agent_Commission_Payable_A/c`
* **Input/Output:** * *Input:* `TxnID`.
    * *Output:* `CommissionAmount`.

---

### 6. EOD Net Settlement Generation
* **Purpose:** To prepare the final file for the Core Banking System (CBS) to move "Real Money" into the Agent's bank account.
* **Trigger:** Scheduled Batch Job (Cron) at 23:59:59 MYT.
* **Business Validation Rules:**
    * All transactions for the day must be in a final state (`SUCCESS` or `FAILED`), not `PENDING`.
* **Processing:**
    1.  Aggregates all `SUCCESS` transactions: `(Withdrawals + Earnings) - (Deposits + BillPays)`.
    2.  Generates a **Flat File (CSV/ISO 20022)** for CBS upload.
    3.  Zeroes out the "Daily Settlement" flags in the local ledger.
* **Input/Output:** * *Input:* `BusinessDate`.
    * *Output:* `SettlementFile`, `TotalSettledAmount`.

---

### 7. Summary of Ledger Impacts by Transaction Type

| Transaction | Float Impact | Dr (Debit) | Cr (Credit) |
| :--- | :--- | :--- | :--- |
| **Cash-In (Deposit)** | **Decrease (-)** | Agent Float A/c | Bank Suspense A/c |
| **Cash-Out (Withdrawal)**| **Increase (+)** | Bank Suspense A/c | Agent Float A/c |
| **Retail Sale** | **Increase (+)** | Customer Account | Agent Float A/c |
| **PIN Purchase** | **Decrease (-)** | Agent Float A/c | Digital Inventory A/c |

---

### 8. Ledger & Float Service (rewritten as Business Core Tier - Business Logic)
*The "Virtual Accountant." It manages the bank’s internal liability to the agents.*

#### A. Virtual Float Management (`Block/Commit/Credit`)
* **Purpose:** To maintain real-time digital balances for agents, ensuring they never spend more than their allocated "Float."
* **Trigger:** Called by the **Orchestrator** at the start and end of every financial flow.
* **Business Validation Rules:**
    * **Locking:** Must use **Pessimistic Locking** on the Agent ID row during a "Block" to prevent race conditions.
    * **Thresholds:** Must trigger an alert if the agent's float drops below the "Minimum Safety Buffer" (e.g., RM 100).
* **Processing:**
    1.  Updates the internal PostgreSQL tables (`agent_floats`, `transaction_history`).
    2.  Generates internal **Double-Entry Accounting Records** in the sub-ledger.
    3.  **Note:** It does **not** call the Core Banking System (CBS) for every transaction; it trusts its own virtual records for speed.
* **Input / Output:**
    * **Input:** `Agent_ID`, `Amount`, `Action_Type` (Debit/Credit).
    * **Output:** `New_Available_Balance`, `Transaction_Reference`.

#### B. EOD Net Settlement Preparation
* **Purpose:** To aggregate the entire day's activity into a single "Net" figure per agent for the actual bank books.
* **Trigger:** Internal Scheduled Job (Cron) at the daily cut-off (e.g., 23:59:59).
* **Business Validation Rules:**
    * **Zero-State Check:** All transactions for that `Business_Date` must be in a final state (`SUCCESS` or `REVERSED`).
* **Processing:**
    1.  Calculates: `(Withdrawals + Earnings) - (Deposits + BillPays)`.
    2.  Produces a **Standardized Settlement File** (JSON or CSV).
    3.  Pushes this file to the **Tier 4 CBS Connector**.
* **Input / Output:**
    * **Input:** `Business_Date`.
    * **Output:** `Settlement_Data_Package` (Sent to Tier 4).

---

# **Tier 4 SERVICES**

Tier 4 is the "Dirty Work" layer. It exists to protect your clean, modern Business Core (Business Core Tier) from the messy, legacy, and often temperamental protocols of the outside world.

Here is the functional breakdown for the **Tier 4: Translation Layer** services.

---

### 1. ISO Translation Engine (Network Bridge)
*The "Linguist" that speaks binary and XML for the financial rails.*

* **Purpose:** To transform internal Business JSON into the strict binary bitmaps required by PayNet (ISO 8583) or the complex XML structures for DuitNow (ISO 20022).
* **Trigger:** Called by the **Business Core Tier Switch Adapter** whenever an external authorization or network management (Echo/Logon) is required.
* **Business Validation Rules:**
    * **Field Presence:** Must ensure all "Mandatory" ISO fields for a specific MTI (Message Type Indicator) are present before transmission.
    * **STAN Management:** Must generate and track the System Trace Audit Number (STAN) for the external network.
* **Processing:**
    1.  **Marshalling:** Converts JSON fields into binary/hex bitmaps.
    2.  **Socket Management:** Maintains a "Keep-Alive" TCP/IP connection with the PayNet/Card Switch.
    3.  **Echo/Heartbeat:** Automatically sends network "Echo" messages every 30-60 seconds to ensure the line isn't dead.
    4.  **Unmarshalling:** Parses the incoming binary response back into a clean JSON for Business Core Tier.
* **Input/Output:**
    * **Input (Internal):** `Canonical_Txn_JSON` (Amount, PAN, TraceID).
    * **Output (External):** `Binary_ISO_Bitmap` (MTI 0200).

---

### 2. CBS Connector (Core Banking Bridge)
*The "Legacy Liaison" for the bank's mainframe.*

* **Purpose:** To act as the single point of contact for the Core Banking System (CBS), shielding Business Core Tier from legacy SOAP, MQ, or fixed-length string protocols.
* **Trigger:** Called by **Business Core Tier Onboarding** (Account Inquiry) or **Business Core Tier Ledger** (EOD Net Settlement upload).
* **Business Validation Rules:**
    * **Timeout Handling:** CBS is often slow; this connector must manage long timeouts without blocking Business Core Tier threads.
    * **Format Strictness:** Mainframes are unforgiving with spaces/padding; this service ensures 100% alignment with the CBS spec.
* **Processing:**
    1.  **XML Marshalling:** Wraps JSON data into SOAP envelopes or MQ message headers.
    2.  **MQ Orchestration:** Places messages in the "Request Queue" and listens for responses on the "Reply Queue."
    3.  **File Staging:** For EOD, it collects the JSON settlement data and converts it into the specific "Flat-File" format the bank's batch engine requires.
* **Input/Output:**
    * **Input (Internal):** `Settlement_JSON` / `Account_Inquiry_JSON`.
    * **Output (External):** `SOAP_XML` / `Fixed-Length_Flat_File` / `MQ_Message`.

---

### 3. HSM Wrapper (Security Bridge)
*The "Vault Guardian" for sensitive encryption.*

* **Purpose:** To handle all communication with the physical **Hardware Security Module (HSM)**. Business Core Tier should *never* see a raw PIN or a clear-text encryption key.
* **Trigger:** Called by the **ISO Translation Engine** (during message packing) or the **Switch Adapter** (during PIN validation).
* **Business Validation Rules:**
    * **Key Isolation:** No encryption keys should exist in Business Core Tier application memory; they reside only in the HSM.
* **Processing:**
    1.  **Command Formatting:** Formats proprietary HSM commands (e.g., Thales "CA" or "BA" commands).
    2.  **PIN Translation:** Takes a PIN block encrypted under a Zone Personal Key (ZPK) and asks the HSM to re-encrypt it under the Local Master Key (LMK).
* **Input/Output:**
    * **Input:** `Encrypted_PIN_Block (ZPK)`, `Source_Key_ID`.
    * **Output:** `Translated_PIN_Block (LMK)`, `Verification_Result`.

---

### 4. Biller Gateway (Aggregator Bridge)
*The "Normalizer" for diverse utility APIs.*

* **Purpose:** To provide a unified interface for multiple different biller providers (TNB, Astro, JomPAY, Fiuu) who all use different API standards.
* **Trigger:** Called by **Business Core Tier Biller Service**.
* **Business Validation Rules:**
    * **Biller Routing:** Directs requests to the correct 3rd party URL based on the `Biller_ID`.
    * **Security Header Injection:** Automatically injects 3rd party API keys and OAuth tokens.
* **Processing:**
    1.  **Adapter Pattern:** Uses specific "Adapters" to convert our internal JSON into the Biller's specific XML or REST format.
    2.  **Idempotency Wrapping:** Ensures that if we retry a bill payment, we don't accidentally charge the customer twice at the Biller's end.
* **Input/Output:**
    * **Input (Internal):** `Generic_Bill_Payment_JSON`.
    * **Output (External):** `Biller_Specific_REST_or_XML_Payload`.

---

### Summary of Tier 4 Operations

| Service | Protocol In (Business Core Tier) | Protocol Out (Tier 4) | Key Responsibility |
| :--- | :--- | :--- | :--- |
| **ISO Engine** | gRPC / REST | **ISO 8583 / 20022** | Binary Bitmaps & Sockets |
| **CBS Connector** | REST / JSON | **SOAP / MQ / Fixed-Length** | Mainframe Communication |
| **HSM Wrapper** | gRPC / REST | **TCP Socket (Proprietary)** | Crypto Command Logic |
| **Biller Gateway**| gRPC / REST | **Varies (XML, REST)** | 3rd Party API Normalization |

---

### The Interaction: Business Core Tier $\rightarrow$ Tier 4

| Business Core Tier Service (Logic) | Calls | Tier 4 Service (Translator) | Data Exchanged |
| :--- | :--- | :--- | :--- |
| **Switch Adapter** | $\rightarrow$ | **ISO Translation Engine** | **JSON** (Logic) $\leftrightarrow$ **Binary/ISO** (Network) |
| **Ledger Service** | $\rightarrow$ | **CBS Connector** | **JSON** (Sub-ledger) $\leftrightarrow$ **SOAP/MQ** (Mainframe) |
| **Biller Service** | $\rightarrow$ | **Biller Gateway** | **JSON** (Unified) $\leftrightarrow$ **XML/REST** (Multiple Billers) |

> **Notes:** Ledger Service & Biller Service connect directly to the downstream/external interfaces in Tier 4, no need to thru the ISO Translation Engine

### Why this is better for your "Off Track" AI problem:
By separating the **Logic (Business Core Tier)** from the **Translation (Tier 4)**, you give the AI much smaller "Context Windows." 
* If you ask it to work on the **Switch Adapter**, it only needs to know how to handle a JSON "Approval" or "Decline." 
* It doesn't get distracted by the complexity of ISO 8583 bitmaps, which is usually where AI agents start to hallucinate or get confused.

---

# **API DEFINITION ("JSON Contract") OF Tier 4 ISO TRANSLATION ENGINE**

This section is about the "JSON Contract" (the API definition) that the **Business Core Tier Switch Adapter** uses to talk to the **Tier 4 ISO Translation Engine**. This is the bridge that keeps your architecture clean.

To keep your architecture clean, the **Business Core Tier to Tier 4 Contract** acts as an **Anti-Corruption Layer (ACL)**. It ensures that the Switch Adapter (Business Core Tier) only speaks "Banking Business," while the ISO Translation Engine (Tier 4) handles the "Network Protocol" heavy lifting.

This contract should be defined as a **REST or gRPC API**. Below is the JSON specification for this interface.

---

## 1. The Request Contract (Business Core Tier $\rightarrow$ Tier 4)
When Business Core Tier wants to authorize a transaction, it sends this **Canonical Request**. Notice there are no mentions of "MTI" or "Bitmaps"—only business data.

### Endpoint: `POST /api/v3/network/authorize`

```json
{
  "header": {
    "transaction_id": "TXN-20260326-99283",
    "network_target": "PAYNET_MY",
    "timestamp": "2026-03-26T12:55:00Z"
  },
  "payload": {
    "transaction_type": "CASH_WITHDRAWAL",
    "amount": 100.00,
    "currency": "MYR",
    "terminal": {
      "id": "POS-88271",
      "location": "Ahmad Mini Mart, KL",
      "merchant_id": "AG-5512"
    },
    "card_instrument": {
      "pan": "411122******0019",
      "expiry": "1228",
      "sequence_number": "01",
      "track_2_data": "411122...=..."
    },
    "security": {
      "pin_block": "88D233F10A92B3C1",
      "key_id": "ZPK_PAYNET_01"
    }
  }
}
```

---

## 2. The Response Contract (Tier 4 $\rightarrow$ Business Core Tier)
Tier 4 receives the binary response from the bank, applies the **Error Mapping Table** we discussed, and returns this "Clean" JSON.

```json
{
  "header": {
    "transaction_id": "TXN-20260326-99283",
    "status": "FAILED"
  },
  "error": {
    "code": "INSUFFICIENT_FUNDS",
    "message": "Customer account balance too low.",
    "action": "NOTIFY_CUSTOMER"
  },
  "network_metadata": {
    "auth_code": null,
    "rrn": "662718293041",
    "stan": "001283",
    "response_time_ms": 1420
  }
}
```

---

## 3. Key Field Logic (The "Magic" of Tier 4)

| Field Name | Logic Responsibility | Why it's in Tier 4 |
| :--- | :--- | :--- |
| **`stan`** | System Trace Audit Number | Tier 4 generates this incrementally for every network message (000001 to 999999). |
| **`rrn`** | Retrieval Reference Number | Tier 4 extracts this from ISO Field 37 to ensure Business Core Tier has it for reversals. |
| **`pin_block`** | Security Translation | Tier 4 takes the `ZPK` encrypted PIN and asks the **HSM Wrapper** to translate it. |
| **`mti`** | Message Type Indicator | Tier 4 maps `CASH_WITHDRAWAL` to `0200` automatically. |

---

## 4. OpenAPI / Swagger Snippet
For our project, we can use this YAML to define the service interface:

```yaml
paths:
  /authorize:
    post:
      summary: "Translate and Transmit Transaction to Financial Switch"
      description: "Converts JSON to ISO8583/20022 and handles socket comms."
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/NetworkRequest'
      responses:
        '200':
          description: "Mapped Network Response"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/NormalizedResponse'
```

---

## 5. Why This Stops AI "Off-Track" Issues
By enforcing this contract, you prevent the AI from trying to calculate "ISO Checksums" or "Binary Padding" inside your **Switch Adapter (Business Core Tier)**. 

If the AI tries to add a field like `iso_field_48`, you can immediately flag it: *"No, that belongs in the Tier 4 ISO Engine logic. Business Core Tier only sees the business fields in this JSON contract."*

---

# **RETRY & REVERSAL POLICY FOR THE Tier 4 TRANSLATION ENGINE -> TIER2**

This is **Retry & Reversal Policy** for the Tier 4 ISO Translation Engine. This document tells the Switch Adapter (Business Core Tier) exactly how long to wait for a Tier 4 response before it must automatically trigger a "Safety Reversal."

This **Retry & Reversal Policy** is the "Seatbelt" of your banking platform. In a distributed 4-tier architecture, the most dangerous state is the "Unknown State"—where Business Core Tier has sent a request, but the network drops before Tier 4 can return the response. 

Without this policy, you risk "Orphan Transactions" where a customer’s money is locked or deducted, but the agent never receives confirmation to hand over the cash.

---

## 1. The "30-Second Rule" (Timeout Policy)

In Malaysian banking (PayNet/MFRS standards), the total round-trip for a financial transaction should ideally stay under **15 seconds**. We set a hard limit at **30 seconds** to account for legacy mainframe latency.

| Stage | Timeout Limit | Action on Timeout |
| :--- | :--- | :--- |
| **Business Core Tier $\rightarrow$ Tier 4** | 25 Seconds | Business Core Tier stops waiting and initiates **Automatic Reversal**. |
| **Tier 4 $\rightarrow$ Tier 4** | 20 Seconds | Tier 4 sends a `NETWORK_TIMEOUT` error to Business Core Tier. |
| **Database Lock** | 5 Seconds | If the Ledger cannot lock the float in 5s, fail the txn immediately. |

---

## 2. Retry Logic (Non-Financial vs. Financial)

We treat "Inquiry" and "Advice" differently from "Financial Authorizations."

* **Non-Financial (e.g., Echo, Account Inquiry):** * **Strategy:** 3 Retries with **Exponential Backoff** (1s, 2s, 4s).
    * **Reason:** These are "Idempotent"—requesting them twice doesn't move money.
* **Financial (e.g., Withdrawal, Bill Pay):**
    * **Strategy:** **ZERO RETRIES** at the Orchestrator level.
    * **Reason:** If a financial request times out, you **must not** try again. You must assume it *might* have succeeded at the bank and move immediately to the **Reversal Flow**.

---

## 3. The "Safety Reversal" Flow (The 0400 Message)

If Business Core Tier hits the 25-second timeout, it must execute the **Automatic Reversal Saga**:

1.  **Mark Internal State:** Business Core Tier updates the `transaction_history` status from `PENDING` to `REVERSAL_INITIATED`.
2.  **Trigger Network Reversal:** Business Core Tier calls the **Tier 4 ISO Engine** with a `REVERSAL_REQUEST` (ISO MTI 0400). This tells PayNet: *"If you approved the previous RM 100, cancel it now."*
3.  **Release Virtual Float:** Once Tier 4 confirms the 0400 was sent (or if Tier 4 itself is down), Business Core Tier calls the **Ledger Service** to `Rollback` the float lock.
4.  **Notify Terminal:** The POS terminal displays: *"Transaction Timeout. Funds Reversed. Please try again."*

---

## 4. Reversal Persistence (The "Never Give Up" Rule)

A Reversal is a **mandatory** message. If the network is down and the 0400 message fails to reach PayNet, your system cannot just "forget" about it.

* **Store-and-Forward (SAF):** Tier 4 must save failed Reversal messages in a local **Persistent Queue (Redis/PostgreSQL)**.
* **Background Worker:** A background process in Tier 4 will keep attempting to send that 0400 message every 60 seconds until a `SUCCESS` is received from the Switch. 
* **Audit Trail:** Every attempt must be logged in the `reversal_audit` table for the EOD reconciliation.

---

## 5. Implementation Guardrail for AI Agent

Add this logic to your **Transaction Orchestrator**:

> "Use the `@CircuitBreaker` and `@TimeLimiter` annotations from **Resilience4j**. Set the `timeoutDuration` to 25s. In the `fallbackMethod`, trigger the `ReversalService.initiate(txnId)` to ensure the Saga is closed correctly."

---

### Why this protects your "Off Track" AI:
By defining this policy, you prevent the AI from suggesting "Just try the withdrawal again" if it fails. In banking, "Trying again" on a timeout is how you end up with double-deductions and angry customers. The AI must now follow the **Strict Reversal Path**.

---

## Error Mapping Table

In a high-stakes banking environment, you cannot let raw legacy error codes like `ISO-05` or `CBS-E999` bubble up to your **Business Core (Business Core Tier)**. If you do, your Orchestrator will become a mess of `if-else` statements trying to handle every possible mainframe glitch.

Instead, **Tier 4 (The Translation Layer)** must "Normalize" these errors. It takes the "Legacy Noise" and maps it to a **Clean Business Exception** that Business Core Tier understands and can act upon (e.g., triggering a Saga Rollback).

---

### 1. The Error Mapping Table: Legacy to Business Core

This table defines the "Normalization" logic within your **ISO Translation Engine** and **CBS Connector**.

| Legacy Source | External Code | Legacy Description | **Business Business Core Tier Error** | **Action Category** |
| :--- | :--- | :--- | :--- | :--- |
| **ISO 8583** | `00` | Approved or Completed | `SUCCESS` | Finalize |
| **ISO 8583** | `51` | Insufficient Funds | `INSUFFICIENT_FUNDS` | Notify Customer |
| **ISO 8583** | `05` | Do Not Honor (Generic) | `DECLINED_BY_ISSUER` | Notify Customer |
| **ISO 8583** | `13` | Invalid Amount | `INVALID_TRANSACTION` | Stop / Alert |
| **ISO 8583** | `91` | Issuer or Switch Inoperative | `NETWORK_TIMEOUT` | **Trigger Reversal** |
| **ISO 20022** | `AB05` | Timeout at Clearing | `NETWORK_TIMEOUT` | **Trigger Reversal** |
| **ISO 20022** | `AC04` | Closed Account | `ACCOUNT_INACTIVE` | Notify Customer |
| **CBS (Core)** | `E102` | Hold on Account | `ACCOUNT_FROZEN` | Notify Customer |
| **CBS (Core)** | `E999` | System Error / DB Down | `DOWNSTREAM_UNAVAILABLE` | Retry / Alert |
| **HSM** | `15` | PIN Block Mismatch | `INVALID_PIN` | Block / Security Alert |

---

### 2. Why "Action Categories" Matter
In the **Transaction Orchestrator (Business Core Tier)**, we don't care that the error was `AB05` or `91`. We only care about the **Category**:

* **Notify Customer:** These are "Clean" failures. Just stop the transaction and tell the customer why (e.g., Wrong PIN).
* **Trigger Reversal:** These are "Technical" failures. Since the network timed out, you don't know if the money moved. You **must** call the `Rollback` logic in the Ledger to release the agent's float.
* **Stop / Alert:** This indicates potential fraud or a major configuration error. You should block the agent terminal and alert the security team.

---

### 3. The "Error Object" Contract
When Tier 4 sends an error back to Business Core Tier, it should use a standardized JSON structure. This ensures your the AI Agent knows exactly where to find the error details.

**Example Response from Tier 4 to Business Core Tier:**
```json
{
  "status": "FAILED",
  "business_error": {
    "code": "INSUFFICIENT_FUNDS",
    "message": "The customer does not have enough balance for this withdrawal.",
    "action": "NOTIFY_CUSTOMER"
  },
  "legacy_context": {
    "source": "PAYNET_ISO_8583",
    "raw_code": "51",
    "trace_id": "99283741"
  }
}
```

---

### 4. Final 4-Tier Architecture Summary
* **Tier 1 (Access):** Displays "Insufficient Funds" to the user.
* **Business Core Tier (Business Core):** Receives `INSUFFICIENT_FUNDS`, marks the txn as `FAILED`, and releases the float lock.
* **Tier 4 (Translation):** Receives `ISO-51` from PayNet and translates it using the Mapping Table.
* **Tier 4 (External):** Sends the raw binary `51` code.

**Would you like me to generate the Java `enum` and a `MappingUtility` class for Tier 4 that implements this exact table?** It will make your "Saga Error Handling" much more robust.

# **TRANSACTION PROCESSING STEP-BY-STEP**

This section will deep-dive to the PROCESSING STEP-BY-STEP of specific transaction that required special handling.

## **DuitNow transaction**

A DuitNow transaction at an Agent location is the perfect example of the "Cash-to-Digital" bridge. In this scenario, a customer provides physical cash to the Agent, and the Agent uses their digital "Float" to send that value across the national real-time payment rails (**PayNet**) to a recipient's bank account or e-wallet.

Using our **4-Tier Architecture**, here is exactly how that RM 100 moves from a customer's pocket to a recipient's phone in under 15 seconds.

---

### Phase 1: The Counter Interaction (Access Layer)

1.  **Initiation:** The customer gives the Agent RM 100 in physical cash and provides a **DuitNow Proxy** (Mobile Number, NRIC, or Account Number).
2.  **Input:** The Agent enters the details into the POS Terminal (Tier 1). The Terminal sends a **JSON Request** to the **API Gateway**, which routes it to the **Transaction Orchestrator (Business Core Tier)**.

---

### Phase 2: Internal Validation (Business Core Tier)

3.  **Risk & Rules Check:** The Orchestrator calls the **Rules & Parameter Service**. 
    * *Check:* Is the Agent active? Is the RM 100 within the daily limit for this customer’s NRIC? 
4.  **Float Reservation (The "Block"):** The Orchestrator calls the **Ledger & Float Service**.
    * *Action:* The Ledger performs a **Pessimistic Lock** on the Agent’s float. It subtracts RM 100 from the `available_balance` but keeps the `actual_balance` unchanged. This "locks" the money so the Agent cannot spend it twice.

---

### Phase 3: External Clearing (Translation Layer & PayNet)

5.  **ISO 20022 Translation:** The Orchestrator calls the **Switch Adapter**. The Switch Adapter passes the JSON to the **ISO Translation Engine (Tier 4)**.
    * *Action:* Tier 4 converts the modern JSON into a complex **ISO 20022 XML message** (pain.001/pacs.008). 
6.  **National Switch Handshake:** Tier 4 sends this XML to **PayNet (Tier 4)** via a secure socket.
    * *Action:* PayNet talks to the Recipient’s Bank in real-time. The Recipient’s Bank confirms the account is valid and accepts the funds. PayNet sends back a "Success" XML response to our Tier 4 engine.

---

### Phase 4: Finalization & Settlement

7.  **Response Normalization:** The **ISO Translation Engine (Tier 4)** receives the XML, maps the PayNet success code to our internal `SUCCESS` status, and sends a clean JSON back to the Orchestrator.
8.  **Ledger Commitment:** The Orchestrator calls the **Ledger Service** to `Commit`.
    * *Action:* The "Block" is removed, and the Agent’s `actual_balance` is permanently reduced by RM 100. The system generates an accounting entry: **Debit Agent Float** / **Credit Bank Settlement Account**.
9.  **Receipt & Notification:** The Terminal prints a **Banking Slip** for the customer. Simultaneously, the **Notification Gateway** sends an SMS or Push Notification to the recipient.

---

### What happens if it fails at Step 6? (The "Safety Net")

If PayNet is down or the recipient's account is closed, the **Retry & Reversal Policy** kicks in:

* **Tier 4** receives an error (e.g., `AC04` - Closed Account).
* **Business Core Tier Orchestrator** receives the normalized `ACCOUNT_INACTIVE` error.
* **The Rollback:** The Orchestrator tells the Ledger to **unblock** the RM 100. The Agent’s `available_balance` is restored instantly.
* **The Result:** The Agent returns the physical cash to the customer and explains the error. No money is lost.

---

**Next Step:** Since we've mapped the happy path and the failure path, would you like me to generate a **DuitNow Transaction Log Sample**? This will show you exactly what the database record looks like at each stage (Blocked, Committed, or Reversed) for your audit trail.