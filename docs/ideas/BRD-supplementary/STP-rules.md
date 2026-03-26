## **STRAIGHT-THROUGH PROCESSING (STP)**

## Summary of all STP strategies

The rigid "Zero Trust" workflow we just discussed—specifically the dual-handshake and cryptographic proof—is the absolute baseline for **Category 1 (100% STP)**. 

However, as you move into Conditional STP and Non-STP, the workflow shifts from relying strictly on *cryptographic proof* (like a PIN) to relying on *data intelligence* (AI/API matching) and, finally, *human judgment*. 

Here is the complete, strategic guide on how the workflow, guardrails, and handling strategies adapt across all three distinct categories.

---

### Category 1: 100% STP (The Automated Core)
**The Objective:** Process high-volume, day-to-day financial transactions instantly with zero human intervention and zero settlement risk.
**Scope:** Bill payments, mobile top-ups, cash-in/cash-out (under limits), and fund transfers.

**The Workflow (Zero Trust & Cryptographic Proof)**
This category operates strictly on hardware and system-level verification.
1. **Initiation:** The agent logs into the terminal securely.
2. **Dual-Handshake:** For example: The customer inserts an EMV card and enters their PIN into an encrypted hardware module (HSM). The agent never sees the PIN.
3. **Float Verification:** The core banking system checks the customer's balance and the agent's pre-funded float ledger simultaneously.
4. **Execution:** The system deducts the agent's float, credits the biller (or dispenses cash instructions), and instantly sends an SMS receipt to the customer for non-repudiation.

**Strategic Breakdown & Guardrails**
* **Strict Liability via Pre-Funding:** The golden rule applies here. The system must never extend credit. Every transaction is backed by the agent's existing cash float.
* **Hard Velocity & Volume Limits:** Hardcoded API limits (e.g., maximum RM 3,000 per transaction, maximum 5 transactions per hour per customer) must be enforced. If a transaction breaches a limit, the system instantly hard-declines it. 
* **Fallback Strategy:** If the network drops or a switch times out mid-transaction, the system uses idempotent keys to automatically reverse the float deduction during the end-of-day batch. It never routes these to a human for "guessing."

---

### Category 2: Conditional STP (The Risk-Based Engine)
**The Objective:** Automate complex onboarding and compliance tasks by replacing the human "Maker" with AI and API integrations, but maintain a safety net for exceptions.
**Scope:** Customer account opening, micro-agent onboarding, and digital float replenishment.

**The Workflow (Data Intelligence & Rules Engine)**
This category relies on dynamic scoring rather than simple true/false PIN verification.
1. **Data Capture:** The applicant uses a device to scan their National ID (MyKad) and takes a live video selfie.
2. **The Invisible Checker:** The system fires concurrent API calls to extract OCR data, run facial liveness checks, and ping external databases (like SSM for business registry and LexisNexis for AML watchlists).
3. **Rules Engine Evaluation:** A backend decision matrix scores the data. (e.g., *Is the OCR match 100%? Is the liveness score > 95%? Are there zero AML hits?*)
4. **Execution or Routing:** If all conditions are perfectly met, the system auto-approves and provisions the account (STP). If *any* condition fails, it halts STP.

**Strategic Breakdown & Guardrails**
* **Graceful Degradation:** The primary guardrail here is the fallback mechanism. If the OCR is blurry or there is a partial AML match, the system does not auto-reject the customer. Instead, it gracefully routes the application data into a manual queue for a human bank officer to review.
* **Continuous Monitoring:** Accounts opened via STP are often placed in a "probationary" tier. The system monitors their first 30 days of transactions more aggressively to catch synthetic identity fraud that might have slipped past the initial API checks.

---

### Category 3: Strictly Non-STP (The Human Checkpoint)
**The Objective:** Manage high-risk, high-value, or legally complex scenarios where regulatory compliance mandates human intuition, physical verification, or manual investigation.
**Scope:** Super-agent onboarding, dispute resolution, AML/Fraud alerts (Suspicious Transaction Reports), and overriding daily limits.

**The Workflow (Maker-Checker & Four-Eyes Principle)**
This category explicitly prevents the system from making the final decision. The system merely acts as a facilitator for human workflows.
1. **Trigger/Initiation:** A field officer inputs an application for a high-tier agent, OR the system's velocity engine freezes a terminal due to suspicious structuring.
2. **The Maker:** A bank officer reviews the physical evidence (e.g., visiting the physical shop, interviewing the owner, gathering physical signatures).
3. **The Checker:** A separate, higher-ranking compliance officer reviews the Maker's notes and the system's risk flags on a secure dashboard.
4. **Execution:** The Checker manually clicks "Approve," "Reject," or "Unfreeze," which then triggers the system to update the database state.

**Strategic Breakdown & Guardrails**
* **Segregation of Duties:** The most critical guardrail. The system must enforce Role-Based Access Control (RBAC) so that the user who inputs the data (Maker) can *never* be the same user who approves it (Checker).
* **Immutable Audit Trails:** Every click, note, and decision made by a human bank officer must be permanently logged with a timestamp and their user ID. If Bank Negara audits an approved high-risk agent, the bank must prove exactly who authorized it and why.
* **SLA Queues:** To prevent bottlenecks, the system must enforce Service Level Agreements (SLAs) on the manual queues. If a dispute sits in a Checker's queue for more than 24 hours, it should automatically escalate to a supervisor.

---

By structuring your Agent Banking solution around these three distinct categories, you ensure that the system runs at maximum operational speed for daily tasks, while keeping a heavy, compliant lid on actual financial risk.

## **STP Applying Rules**

In the highly regulated Malaysian banking sector, implementing Straight-Through Processing (STP) is not just a technology choice; it is governed heavily by **Bank Negara Malaysia (BNM)** guidelines, specifically regarding Anti-Money Laundering (AML), Countering Financing of Terrorism (CFT), and e-KYC policies.

To protect the bank while maximizing operational efficiency, an Agent Banking system should divide its functions into three distinct STP categories: **100% STP**, **Conditional STP (Risk-Based)**, and **Strictly Non-STP**. 

Here is the strategic breakdown of how these functions should be handled in your system.

### 1. 100% STP (The Transactional Core)
These are the high-volume, low-risk, day-to-day functions. For an agent network to be profitable and efficient, these **must** be fully automated with zero bank-user intervention. The system relies entirely on point-of-sale authentication (PIN or Biometric) and API responses.

* **Bill Payments & JomPAY:** Routing customer funds to utility companies (ASTRO, TM, SESB) or through the PayNet switch.
* **Prepaid Top-Ups & e-Wallet Reloads:** Pinging telco aggregators or e-wallet gateways (like S Pay Global or Touch 'n Go) for instant airtime/balance crediting.
* **PIN Purchases:** Generating secure e-vouchers for government services (UPU, PTPTN, SSM) from the bank's pre-loaded inventory.
* **Internet Banking Registration:** Generating and printing the temporary myBSN/portal activation PIN after successful ATM card and PIN validation at the terminal.
* **Cash-In & Cash-Out (Within Limits):** Standard deposits and withdrawals. *Note:* STP applies only if the transaction is under the daily limit (e.g., RM 3,000 per transaction) and authenticated securely via the customer's ATM PIN or registered MyKad biometrics.
* **Fund Transfers (Internal & GIRO):** Routing the transfer to the respective batch queue or instant switch. 

**When is 100% STP Allowed? (The Decision Matrix):**
You cannot allow STP for everyone. A robust system uses a dynamic decision matrix to determine if the application can skip the human Checker. Zero intervention is typically allowed only if **all** of the following conditions are met:

* **Tier 1 / Micro-Agents:** The applicant is applying for the lowest tier of agency (e.g., strict transaction limits, maximum RM 5,000 daily float).
* **Perfect Data Match:** The OCR name extracted from the ID matches the SSM API data exactly, character for character.
* **Low-Risk Geography:** The shop's GPS coordinates are in a designated low-risk or highly unbanked zone.
* **Zero Watchlist Hits:** The AML screening returns absolutely zero potential matches.

### 2. Conditional STP (Risk-Based & e-KYC Dependent)
These functions can be STP, but **only if** they pass a rigorous, multi-layered rules engine (like the one we designed earlier). If they fail even one parameter, they must instantly fall back to a manual Maker-Checker queue.

* **Customer Onboarding (Opening a Basic Account):** Under BNM's e-KYC framework, you can open a basic savings account without human intervention if the system successfully performs:
    * A high-confidence OCR read of the MyKad.
    * A successful biometric liveness check (facial recognition matching the MyKad).
    * A completely clean hit (zero matches) on all local and global AML/Sanctions watchlists.
* **Micro-Agent Onboarding:** Approving the lowest tier of agents (e.g., a rural hawker stall wanting to only sell mobile top-ups and UPU PINs). They can be auto-approved if their SSM API check is active and they request a very low daily float limit.
* **Agent Float Replenishment (Digital):** If an agent tops up their virtual float by transferring money from their personal bank account via DuitNow/FPX, the float ledger is credited via STP. *(If they deposit physical cash at a branch counter, it requires a human teller).*

### 3. Strictly Non-STP (Requires Human Intervention)
These functions carry high financial, regulatory, or reputational risk. BNM compliance dictates that these processes must have human eyes (Maker-Checker) on them.

* **Standard & Super Agent Onboarding:** Enrolling high-tier agents who will process massive amounts of physical cash requires a bank officer to physically verify the shop premises, review the business model, and manually approve the liquidity limits.
* **Transaction Reversals & Dispute Resolution:** If an agent claims a system timeout caused a double-deduction on their float, a Finance Officer must review the switch logs and manually authorize the refund. Auto-refunding edge cases leads to massive reconciliation holes.
* **AML/Fraud Alerts (Suspicious Transaction Reports):** If the velocity engine flags an agent for structuring (e.g., doing ten RM 2,900 cash deposits in an hour to avoid the RM 3,000 limit flag), the system must instantly freeze the terminal. Only a Compliance Officer can review the logs and unfreeze the account or file an STR to BNM.
* **Daily Transaction Limit Increases:** If an agent requests their daily processing limit to be raised from RM 10,000 to RM 50,000, a Risk Officer must manually review their historical transaction cleanliness before approving.

***

By strictly enforcing STP on Category 1, utilizing your Rules Engine for Category 2, and reserving your bank staff for Category 3, you create a highly scalable platform that remains completely compliant with Malaysian banking regulations.


## The best-practice workflow for 100% STP

To achieve a near-zero risk environment where both the **Agent** and the **Customer** are involved, the system must be designed so that neither party can defraud the other, and neither can defraud the bank. 

Here is the regulatory best-practice workflow and the specific controls required to secure 100% STP agent banking transactions.

### 1. The Golden Rule: The Pre-Funded Float (Eliminating Credit Risk)
For STP to exist safely, the bank must never extend real-time credit to an agent. 
* **The Rule:** The agent must maintain a pre-funded virtual account (the Float Ledger). 
* **How it mitigates risk:** If a customer walks in to pay a RM 200 electricity bill with physical cash, the STP system instantly deducts RM 200 from the agent's pre-funded float. If the agent's float only has RM 150, the terminal hard-declines the transaction before it even reaches the utility company. The bank assumes **zero settlement risk** because the bank already holds the agent's money.

### 2. The "Dual-Handshake" Workflow (Eliminating Fraud & Repudiation)
In a zero-trust STP workflow, the system must independently verify the intent of *both* external parties for every single transaction. One party cannot authorize on behalf of the other.

Here is the strict step-by-step workflow for a transaction:

#### 1. Scenario: Cash Withdrawal using ATM Card

1. **Agent Initiation (Device & Identity Binding):** * The system validates that the request is coming from a whitelisted MAC address/Device ID (preventing hackers from spoofing the API). 
   * The agent logs into the active session using their biometric fingerprint or a dynamic OTP.
2. **Customer Intent & Authentication (Air-Gapped from Agent):** * The customer inserts their physical ATM card into the EMV chip reader. 
   * **Crucial Step:** The customer types their 6-digit ATM PIN into a secure, encrypted PIN pad (or uses a biometric scanner). The agent *never* sees or handles this PIN. The PIN is encrypted at the hardware level (Hardware Security Module - HSM) before being sent to the bank.
3. **Real-Time Guardrails (The System Check):**
   * The core system checks the customer's balance.
   * The system checks the agent's float capacity.
   * The system runs a millisecond velocity check (e.g., *Has this customer done 5 withdrawals in the last hour?*).
4. **Irrefutable Proof (Non-Repudiation):** * The system processes the ledger updates. 
   * The terminal prints a physical receipt. 
   * Simultaneously, the core system fires an automated SMS alert directly to the customer's registered mobile number (*"RM 100 withdrawn at Agent XYZ"*). This prevents the agent from claiming a transaction failed and pocketing the cash, as the customer has immediate digital proof on their own device.

#### 2. Scenario: Cash Deposit (Source of Fund = Physical Cash)
In this scenario, the "Authentication" is actually reversed. The bank must authenticate the **Agent** (to ensure they have enough float) and the **Customer** (to ensure the money goes to the right place).

* **Identification:** The customer provides their **MyKad** (which the agent scans to auto-fill the KYC data) or keys in their **Account Number**.
* **Authentication (The Validation):**
    * **Low Value:** The system performs a "Name Inquiry." The POS displays the account holder's name (e.g., `MOHD A***D BIN AL*`). The customer confirms this is correct.
    * **High Value (BNM Anti-Money Laundering):** The customer must perform a **MyKad Biometric Match** (thumbprint) on the agent's terminal. This proves the person depositing the cash is the legitimate owner or an authorized representative, preventing "mule" deposits.
* **The Handshake:** The "Physical Handshake" is the agent literally counting the cash and clicking "Confirm" on the POS, which triggers the digital "Financial Handshake" to credit the customer's account.

---

#### 3. Scenario: Digital Fund Transfer (Source of Fund = Mobile App/DuitNow)
If a customer has money in their bank account but forgot their ATM card, the Agent POS acts as a "Request Hub." This utilizes the **DuitNow Request (Request-to-Pay)** protocol.

* **Identification:** The customer gives the agent their **DuitNow ID** (usually their Mobile Number or IC Number).
* **The "Push" Authentication:**
    1.  The Agent keys in the transfer amount on the POS.
    2.  The API Gateway fires a **DuitNow Request** to the customer's bank.
    3.  The customer receives a **Push Notification** on their own smartphone (e.g., Maybank MAE or CIMB OCTO).
    4.  The customer approves the transaction *on their own phone* using their phone's biometrics (FaceID/TouchID).
* **The Handshake:** The "Proof of Intent" isn't a PIN entered on the POS; it is a **Digitally Signed Token** sent from the customer's mobile app directly to the National Switch, which then notifies your Agent Gateway that the funds are cleared.

---

#### Comparison of Authentication Handshakes

| Fund Source | Identification (ID) | Authentication (Proof) | Security Protocol |
| :--- | :--- | :--- | :--- |
| **ATM Card** | EMV Chip Data | 6-Digit PIN on POS | ISO 8583 / DUKPT |
| **Cash** | MyKad / Account No. | **Agent Verification** + Optional MyKad Biometrics | Ledger Float Deduction |
| **Digital Account**| DuitNow Proxy | **Mobile App Approval** (FaceID/PIN) | ISO 20022 / RTP |
| **e-Wallet** | Dynamic QR Code | **Mobile App Approval** | OAuth 2.0 / JWS |

---

### 3. Strict Boundary Controls for STP
To legally allow these transactions to flow without human review, you must enforce hard, unchangeable limits at the API Gateway level:

#### **Customer-Side Guardrails**
* **Hard Caps:** Implement strict single-transaction limits (e.g., Max RM 1,000 per transaction) and daily cumulative limits (e.g., Max RM 3,000 per day). 
* **Card-Present Mandate:** For high-risk STP like cash withdrawals, the physical EMV chip card must be present. Magnetic stripe fallbacks or "keyed-in" card numbers are strictly prohibited in modern STP to prevent cloning.

#### **Agent-Side Guardrails**
* **Geo-Fencing:** The POS device’s GPS coordinates are pinged with every transaction. If the terminal suddenly processes a transaction 50km away from its registered shop address, the STP engine auto-declines it and locks the terminal.
* **Velocity & Structuring Rules (Anti-Smurfing):** The STP engine must track the frequency of transactions. If an agent tries to process ten RM 900 deposits in 5 minutes to avoid a RM 1,000 limit flag (a money-laundering tactic known as smurfing), the STP rules engine instantly breaks the circuit, freezes the float, and alerts the compliance team.

---

### Example: The Self-Service UI/UX Workflow (Zero Bank Intervention)

Here is exactly how a "Zero Intervention" UI/UX workflow functions, and the strict business rules required to allow it safely.

Instead of a bank field officer (Maker) entering the data, the prospective agent performs a self-guided onboarding via a mobile app or web portal.

* **Step 1: OCR Data Extraction.** The user uploads or takes a live photo of their National ID (MyKad) and Business Registration (SSM) certificate. The system uses Optical Character Recognition (OCR) to instantly extract the text (Name, ID Number, Address, SSM Number) and auto-fill the application form.
* **Step 2: Biometric Liveness Detection.** The user is prompted to take a video selfie (e.g., "blink twice" or "turn your head"). The system uses AI to match the 3D live face against the 2D photo on the uploaded ID to ensure they are the real person and not holding up a photograph.
* **Step 3: Automated API Verification (The Invisible Checker).** The moment the user hits submit, the backend fires concurrent API calls:
    * **Identity:** Pings the National Registration Department (e.g., JPN in Malaysia) or a verified digital ID provider to confirm the MyKad is authentic.
    * **Business Legitimacy:** Pings the SSM e-Info API to confirm the business is active, not bankrupt, and the applicant is the registered owner.
    * **AML/Sanctions:** Pings global watchlists (e.g., LexisNexis, Dow Jones) to ensure the applicant is not a Politically Exposed Person (PEP) or a known financial criminal.
* **Step 4: Instant Activation.** If all APIs return a 100% positive match and a "Clear" risk status, the system instantly provisions the Agent ID, creates the virtual Float Ledger, and displays the login credentials on the screen. **Time elapsed: ~3 minutes. Bank staff involved: 0.**

The **"Dual-Handshake"** remains the gold standard for security, but the "Card + PIN" combo is just one way to achieve it. In Agent Banking, the system must be flexible enough to authenticate the customer using whatever "Proof of Intent" they have available.

When the source of funds shifts from a physical card to **Cash** or **Digital Accounts**, the first handshake (Step 1) adapts to a different set of credentials.

---

### Summary
In an STP environment, you substitute human judgment with cryptographic proof. By enforcing **pre-funded liquidity**, **hardware-encrypted dual authentication**, and **hard-coded velocity limits**, you create a mathematically secure channel where it is practically impossible for the agent or the customer to manipulate the financial outcome without immediate detection.
