# **DETAILED SERVICE PROCESSING - BUSINESS CORE TIER**

To complete your **Business Core Tier** blueprint, here is the detailed functional breakdown for the remaining services. This ensures that every component in project has a defined "Job Description," making it much harder for the AI to hallucinate incorrect logic.

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

## 2. Switch Adapter Service (Tier 2 - Business Logic)
*The "Transaction Strategist." It manages the lifecycle of an external request without knowing the "language" of the target network.*

* **Purpose:** To decide which external rail to use (e.g., PayNet vs. Internal Card Switch) and manage the state of the "Financial Handshake" (Saga).
* **Trigger:** Called by the **Transaction Orchestrator** when a transaction requires external authorization.
* **Business Validation Rules:**
    * **Routing Logic:** Select the correct downstream Tier 3 engine based on `BIN` (Bank Identification Number) or `BillerCategory`.
    * **Duplicate Detection:** Check the `Idempotency-Key` to ensure the same request isn't sent to the network twice.
* **Processing:**
    1.  Maps the Orchestrator's generic JSON into a **Canonical Banking Request** (a standard JSON format your Tier 3 expects).
    2.  Calls the **Tier 3 ISO Translation Engine** (via gRPC or REST).
    3.  Handles "In-Flight" timeouts: If Tier 3 doesn't respond, the Switch Adapter initiates the "Reversal Strategy."
    4.  Translates the Tier 3 JSON response (e.g., `Result: 00`) into a Tier 2 business status (e.g., `Status: APPROVED`).
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
* **Processing:** 1.  Calls **JomPAY API** or the direct Biller API (e.g., Astro).
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

## 5. Transaction Orchestrator
*The "Conductor" of the symphony.*

### A. Workflow Coordination (`ExecuteSaga`)
* **Purpose:** To manage the multi-step "Handshake" (Reserve Float $\rightarrow$ Switch Auth $\rightarrow$ Commit Ledger).
* **Trigger:** Called by the **API Gateway** for every transaction request.
* **Processing:** 1.  **Orchestration:** Calls Ledger to `BlockFloat`. 
    2.  If Success, calls Switch to `Authorize`.
    3.  If Switch Success, calls Ledger to `Commit`.
    4.  If any step fails, calls `Rollback`.
* **Input/Output:** * *Input:* `ClientRequestJSON`.
    * *Output:* `FinalResponseJSON`.

### B. Error Handling (`Compensate`)
* **Purpose:** To ensure the system is "Clean" if a mid-way failure occurs.
* **Trigger:** Any exception or timeout during a workflow.
* **Processing:** 1.  Triggers the **Reverse/Undo** logic on the Ledger and Switch to ensure no "Zombie" locks remain.
* **Input/Output:** * *Input:* `FailedStepID`, `TxnContext`.
    * *Output:* `CleanupStatus`.

---



**Would you like me to draft the "Sequence Diagram" for a "Cash-In (Deposit)" transaction, showing exactly how these services exchange this data in order?**

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

### Summary of Ledger Impacts by Transaction Type

| Transaction | Float Impact | Dr (Debit) | Cr (Credit) |
| :--- | :--- | :--- | :--- |
| **Cash-In (Deposit)** | **Decrease (-)** | Agent Float A/c | Bank Suspense A/c |
| **Cash-Out (Withdrawal)**| **Increase (+)** | Bank Suspense A/c | Agent Float A/c |
| **Retail Sale** | **Increase (+)** | Customer Account | Agent Float A/c |
| **PIN Purchase** | **Decrease (-)** | Agent Float A/c | Digital Inventory A/c |

---

### 6. Ledger & Float Service (rewritten as Tier 2 - Business Logic)
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
    3.  Pushes this file to the **Tier 3 CBS Connector**.
* **Input / Output:**
    * **Input:** `Business_Date`.
    * **Output:** `Settlement_Data_Package` (Sent to Tier 3).

---

## **TIER 3 SERVICES**

Tier 3 is the "Dirty Work" layer. It exists to protect your clean, modern Business Core (Tier 2) from the messy, legacy, and often temperamental protocols of the outside world.

Here is the functional breakdown for the **Tier 3: Translation Layer** services.

---

### 1. ISO Translation Engine (Network Bridge)
*The "Linguist" that speaks binary and XML for the financial rails.*

* **Purpose:** To transform internal Business JSON into the strict binary bitmaps required by PayNet (ISO 8583) or the complex XML structures for DuitNow (ISO 20022).
* **Trigger:** Called by the **Tier 2 Switch Adapter** whenever an external authorization or network management (Echo/Logon) is required.
* **Business Validation Rules:**
    * **Field Presence:** Must ensure all "Mandatory" ISO fields for a specific MTI (Message Type Indicator) are present before transmission.
    * **STAN Management:** Must generate and track the System Trace Audit Number (STAN) for the external network.
* **Processing:**
    1.  **Marshalling:** Converts JSON fields into binary/hex bitmaps.
    2.  **Socket Management:** Maintains a "Keep-Alive" TCP/IP connection with the PayNet/Card Switch.
    3.  **Echo/Heartbeat:** Automatically sends network "Echo" messages every 30-60 seconds to ensure the line isn't dead.
    4.  **Unmarshalling:** Parses the incoming binary response back into a clean JSON for Tier 2.
* **Input/Output:**
    * **Input (Internal):** `Canonical_Txn_JSON` (Amount, PAN, TraceID).
    * **Output (External):** `Binary_ISO_Bitmap` (MTI 0200).

---

### 2. CBS Connector (Core Banking Bridge)
*The "Legacy Liaison" for the bank's mainframe.*

* **Purpose:** To act as the single point of contact for the Core Banking System (CBS), shielding Tier 2 from legacy SOAP, MQ, or fixed-length string protocols.
* **Trigger:** Called by **Tier 2 Onboarding** (Account Inquiry) or **Tier 2 Ledger** (EOD Net Settlement upload).
* **Business Validation Rules:**
    * **Timeout Handling:** CBS is often slow; this connector must manage long timeouts without blocking Tier 2 threads.
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

* **Purpose:** To handle all communication with the physical **Hardware Security Module (HSM)**. Tier 2 should *never* see a raw PIN or a clear-text encryption key.
* **Trigger:** Called by the **ISO Translation Engine** (during message packing) or the **Switch Adapter** (during PIN validation).
* **Business Validation Rules:**
    * **Key Isolation:** No encryption keys should exist in Tier 2 application memory; they reside only in the HSM.
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
* **Trigger:** Called by **Tier 2 Biller Service**.
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

### Summary of Tier 3 Operations

| Service | Protocol In (Tier 2) | Protocol Out (Tier 4) | Key Responsibility |
| :--- | :--- | :--- | :--- |
| **ISO Engine** | gRPC / REST | **ISO 8583 / 20022** | Binary Bitmaps & Sockets |
| **CBS Connector** | REST / JSON | **SOAP / MQ / Fixed-Length** | Mainframe Communication |
| **HSM Wrapper** | gRPC / REST | **TCP Socket (Proprietary)** | Crypto Command Logic |
| **Biller Gateway**| gRPC / REST | **Varies (XML, REST)** | 3rd Party API Normalization |

---

### The Interaction: Tier 2 $\rightarrow$ Tier 3

| Tier 2 Service (Logic) | Calls | Tier 3 Service (Translator) | Data Exchanged |
| :--- | :--- | :--- | :--- |
| **Switch Adapter** | $\rightarrow$ | **ISO Translation Engine** | **JSON** (Logic) $\leftrightarrow$ **Binary/ISO** (Network) |
| **Ledger Service** | $\rightarrow$ | **CBS Connector** | **JSON** (Sub-ledger) $\leftrightarrow$ **SOAP/MQ** (Mainframe) |
| **Biller Service** | $\rightarrow$ | **Biller Gateway** | **JSON** (Unified) $\leftrightarrow$ **XML/REST** (Multiple Billers) |

### Why this is better for your "Off Track" AI problem:
By separating the **Logic (Tier 2)** from the **Translation (Tier 3)**, you give the AI much smaller "Context Windows." 
* If you ask it to work on the **Switch Adapter**, it only needs to know how to handle a JSON "Approval" or "Decline." 
* It doesn't get distracted by the complexity of ISO 8583 bitmaps, which is usually where AI agents start to hallucinate or get confused.

**Would you like me to draft the "JSON Contract" (the API definition) that the Tier 2 Switch Adapter uses to talk to the Tier 3 ISO Translation Engine?** This is the bridge that keeps your architecture clean.

---

# **API DEFINITION OF TIER 3 ISO TRANSLATION ENGINE**

To keep your architecture clean, the **Tier 2 to Tier 3 Contract** acts as an **Anti-Corruption Layer (ACL)**. It ensures that the Switch Adapter (Tier 2) only speaks "Banking Business," while the ISO Translation Engine (Tier 3) handles the "Network Protocol" heavy lifting.

This contract should be defined as a **REST or gRPC API**. Below is the JSON specification for this interface.

---

## 1. The Request Contract (Tier 2 $\rightarrow$ Tier 3)
When Tier 2 wants to authorize a transaction, it sends this **Canonical Request**. Notice there are no mentions of "MTI" or "Bitmaps"—only business data.

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

## 2. The Response Contract (Tier 3 $\rightarrow$ Tier 2)
Tier 3 receives the binary response from the bank, applies the **Error Mapping Table** we discussed, and returns this "Clean" JSON.

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

## 3. Key Field Logic (The "Magic" of Tier 3)

| Field Name | Logic Responsibility | Why it's in Tier 3 |
| :--- | :--- | :--- |
| **`stan`** | System Trace Audit Number | Tier 3 generates this incrementally for every network message (000001 to 999999). |
| **`rrn`** | Retrieval Reference Number | Tier 3 extracts this from ISO Field 37 to ensure Tier 2 has it for reversals. |
| **`pin_block`** | Security Translation | Tier 3 takes the `ZPK` encrypted PIN and asks the **HSM Wrapper** to translate it. |
| **`mti`** | Message Type Indicator | Tier 3 maps `CASH_WITHDRAWAL` to `0200` automatically. |

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
By enforcing this contract, you prevent the AI from trying to calculate "ISO Checksums" or "Binary Padding" inside your **Switch Adapter (Tier 2)**. 

If the AI tries to add a field like `iso_field_48`, you can immediately flag it: *"No, that belongs in the Tier 3 ISO Engine logic. Tier 2 only sees the business fields in this JSON contract."*

**Would you like me to draft the "Retry & Reversal Policy" for this interface?** This document tells the Switch Adapter (Tier 2) exactly how long to wait for a Tier 3 response before it must automatically trigger a "Safety Reversal."

This **Retry & Reversal Policy** is the "Seatbelt" of your banking platform. In a distributed 4-tier architecture, the most dangerous state is the "Unknown State"—where Tier 2 has sent a request, but the network drops before Tier 3 can return the response. 

Without this policy, you risk "Orphan Transactions" where a customer’s money is locked or deducted, but the agent never receives confirmation to hand over the cash.

---

## 1. The "30-Second Rule" (Timeout Policy)

In Malaysian banking (PayNet/MFRS standards), the total round-trip for a financial transaction should ideally stay under **15 seconds**. We set a hard limit at **30 seconds** to account for legacy mainframe latency.

| Stage | Timeout Limit | Action on Timeout |
| :--- | :--- | :--- |
| **Tier 2 $\rightarrow$ Tier 3** | 25 Seconds | Tier 2 stops waiting and initiates **Automatic Reversal**. |
| **Tier 3 $\rightarrow$ Tier 4** | 20 Seconds | Tier 3 sends a `NETWORK_TIMEOUT` error to Tier 2. |
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

If Tier 2 hits the 25-second timeout, it must execute the **Automatic Reversal Saga**:

1.  **Mark Internal State:** Tier 2 updates the `transaction_history` status from `PENDING` to `REVERSAL_INITIATED`.
2.  **Trigger Network Reversal:** Tier 2 calls the **Tier 3 ISO Engine** with a `REVERSAL_REQUEST` (ISO MTI 0400). This tells PayNet: *"If you approved the previous RM 100, cancel it now."*
3.  **Release Virtual Float:** Once Tier 3 confirms the 0400 was sent (or if Tier 3 itself is down), Tier 2 calls the **Ledger Service** to `Rollback` the float lock.
4.  **Notify Terminal:** The POS terminal displays: *"Transaction Timeout. Funds Reversed. Please try again."*

---

## 4. Reversal Persistence (The "Never Give Up" Rule)

A Reversal is a **mandatory** message. If the network is down and the 0400 message fails to reach PayNet, your system cannot just "forget" about it.

* **Store-and-Forward (SAF):** Tier 3 must save failed Reversal messages in a local **Persistent Queue (Redis/PostgreSQL)**.
* **Background Worker:** A background process in Tier 3 will keep attempting to send that 0400 message every 60 seconds until a `SUCCESS` is received from the Switch. 
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

In a high-stakes banking environment, you cannot let raw legacy error codes like `ISO-05` or `CBS-E999` bubble up to your **Business Core (Tier 2)**. If you do, your Orchestrator will become a mess of `if-else` statements trying to handle every possible mainframe glitch.

Instead, **Tier 3 (The Translation Layer)** must "Normalize" these errors. It takes the "Legacy Noise" and maps it to a **Clean Business Exception** that Tier 2 understands and can act upon (e.g., triggering a Saga Rollback).

---

### 1. The Error Mapping Table: Legacy to Business Core

This table defines the "Normalization" logic within your **ISO Translation Engine** and **CBS Connector**.

| Legacy Source | External Code | Legacy Description | **Business Tier 2 Error** | **Action Category** |
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
In the **Transaction Orchestrator (Tier 2)**, we don't care that the error was `AB05` or `91`. We only care about the **Category**:

* **Notify Customer:** These are "Clean" failures. Just stop the transaction and tell the customer why (e.g., Wrong PIN).
* **Trigger Reversal:** These are "Technical" failures. Since the network timed out, you don't know if the money moved. You **must** call the `Rollback` logic in the Ledger to release the agent's float.
* **Stop / Alert:** This indicates potential fraud or a major configuration error. You should block the agent terminal and alert the security team.

---

### 3. The "Error Object" Contract
When Tier 3 sends an error back to Tier 2, it should use a standardized JSON structure. This ensures your the AI Agent knows exactly where to find the error details.

**Example Response from Tier 3 to Tier 2:**
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
* **Tier 2 (Business Core):** Receives `INSUFFICIENT_FUNDS`, marks the txn as `FAILED`, and releases the float lock.
* **Tier 3 (Translation):** Receives `ISO-51` from PayNet and translates it using the Mapping Table.
* **Tier 4 (External):** Sends the raw binary `51` code.

**Would you like me to generate the Java `enum` and a `MappingUtility` class for Tier 3 that implements this exact table?** It will make your "Saga Error Handling" much more robust.

# **SPECIALIZED TRANSACTION PROCESSING**

## **DuitNow transaction**

A DuitNow transaction at an Agent location is the perfect example of the "Cash-to-Digital" bridge. In this scenario, a customer provides physical cash to the Agent, and the Agent uses their digital "Float" to send that value across the national real-time payment rails (**PayNet**) to a recipient's bank account or e-wallet.

Using our **4-Tier Architecture**, here is exactly how that RM 100 moves from a customer's pocket to a recipient's phone in under 15 seconds.

---

### Phase 1: The Counter Interaction (Access Layer)

1.  **Initiation:** The customer gives the Agent RM 100 in physical cash and provides a **DuitNow Proxy** (Mobile Number, NRIC, or Account Number).
2.  **Input:** The Agent enters the details into the POS Terminal (Tier 1). The Terminal sends a **JSON Request** to the **API Gateway**, which routes it to the **Transaction Orchestrator (Tier 2)**.

---

### Phase 2: Internal Validation (Business Core Tier)

3.  **Risk & Rules Check:** The Orchestrator calls the **Rules & Parameter Service**. 
    * *Check:* Is the Agent active? Is the RM 100 within the daily limit for this customer’s NRIC? 
4.  **Float Reservation (The "Block"):** The Orchestrator calls the **Ledger & Float Service**.
    * *Action:* The Ledger performs a **Pessimistic Lock** on the Agent’s float. It subtracts RM 100 from the `available_balance` but keeps the `actual_balance` unchanged. This "locks" the money so the Agent cannot spend it twice.



---

### Phase 3: External Clearing (Translation Layer & PayNet)

5.  **ISO 20022 Translation:** The Orchestrator calls the **Switch Adapter**. The Switch Adapter passes the JSON to the **ISO Translation Engine (Tier 3)**.
    * *Action:* Tier 3 converts the modern JSON into a complex **ISO 20022 XML message** (pain.001/pacs.008). 
6.  **National Switch Handshake:** Tier 3 sends this XML to **PayNet (Tier 4)** via a secure socket.
    * *Action:* PayNet talks to the Recipient’s Bank in real-time. The Recipient’s Bank confirms the account is valid and accepts the funds. PayNet sends back a "Success" XML response to our Tier 3 engine.

---

### Phase 4: Finalization & Settlement

7.  **Response Normalization:** The **ISO Translation Engine (Tier 3)** receives the XML, maps the PayNet success code to our internal `SUCCESS` status, and sends a clean JSON back to the Orchestrator.
8.  **Ledger Commitment:** The Orchestrator calls the **Ledger Service** to `Commit`.
    * *Action:* The "Block" is removed, and the Agent’s `actual_balance` is permanently reduced by RM 100. The system generates an accounting entry: **Debit Agent Float** / **Credit Bank Settlement Account**.
9.  **Receipt & Notification:** The Terminal prints a **Banking Slip** for the customer. Simultaneously, the **Notification Gateway** sends an SMS or Push Notification to the recipient.

---

### What happens if it fails at Step 6? (The "Safety Net")

If PayNet is down or the recipient's account is closed, the **Retry & Reversal Policy** kicks in:

* **Tier 3** receives an error (e.g., `AC04` - Closed Account).
* **Tier 2 Orchestrator** receives the normalized `ACCOUNT_INACTIVE` error.
* **The Rollback:** The Orchestrator tells the Ledger to **unblock** the RM 100. The Agent’s `available_balance` is restored instantly.
* **The Result:** The Agent returns the physical cash to the customer and explains the error. No money is lost.



---

**Next Step:** Since we've mapped the happy path and the failure path, would you like me to generate a **DuitNow Transaction Log Sample**? This will show you exactly what the database record looks like at each stage (Blocked, Committed, or Reversed) for your audit trail.