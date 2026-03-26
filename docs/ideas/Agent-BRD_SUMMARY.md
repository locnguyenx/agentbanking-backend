# BRD_SUMMARY: Agent Banking Functional Requirements

## **1. Project Overview**
A digital platform enabling third-party agents (retailers/merchants) to provide essential banking services to customers using a specialized Android/Flutter POS terminal.

## **2. Function List**
- Balance Inquiry
- eSSP- CASH
- eSSP- CARD
- PREPAID TOP-UP CELCOM - CASH
- PREPAID TOP-UP CELCOM - CARD
- PREPAID TOP-UP M1 - CASH
- PREPAID TOP-UP M1 - CARD
- Bill Payment - CARD
- Bill Payment - CASH
- Withdrawal
- Cash Deposit –CARD
- Cash Deposit –CASH
- Cashless Payment
- Opening Account - MyKad (New Customer/ Existing Customer)
- Bill Payment ASTRO RPN - CASH
- Bill Payment ASTRO RPN - CARD
- Bill Payment TM RPN - CASH
- Bill Payment TM RPN - CARD
- Bill Payment EPF i-SARAAN/i-SURI/SELF EMPLOYED – CARD
- Bill Payment EPF i-SARAAN/i-SURI/SELF EMPLOYED – CASH
- Withdrawal using MYKAD
- SARAWAK PAY e-WALLET Withdrawal - CASH
- SARAWAK PAY e-WALLET Withdrawal - CARD
- SARAWAK PAY e-WALLET TOP-UP -CASH
- SARAWAK PAY e-WALLET TOP-UP -CARD
- PIN PURCHASE using CASH
- PIN PURCHASE using CARD
- JOMPAY - Cash(OFF-US)
- JOMPAY - Card (OFF-US)
- JOMPAY - Cash(ON-US)
- JOMPAY - Card(ON-US)

## **3. Core Financial Products & Rules**

### A. Cash Withdrawal (Cash-Out)
* **Method:** Customer uses an ATM Card (EMV Chip + PIN).
* **Limit:** Standard max RM 5,000 per day (configurable via Rules Service).
* **Reversal Rule:** If the terminal printer fails or the network drops after the switch approves, an **MTI 0400 Reversal** must be triggered immediately (Store & Forward).

### B. Cash Deposit (Cash-In)
* **Method:** Customer hands physical cash/card to the agent; agent triggers a digital credit to the customer’s account.
* **Validation:** Must perform a `ProxyEnquiry` or `AccountEnquiry` to validate the destination before the money moves.
* **Money Source:** Cash, Card

### C. DuitNow Fund Transfer
* **Network:** PayNet (ISO 20022).
* **Proxies:** Supports Mobile Number, MyKad Number, and Business Registration Number (BRN).
* **Logic:** Real-time settlement ($<15$ seconds).

### D. JomPAY & Bill Payments
* **Process:** Agent accepts cash or card for utilities.
* **Validation:** Mandatory `Ref-1` validation against the biller’s database before payment.
* **Utilities:** JomPAY, ASTRO RPN, TM RPN, EPF i-SARAAN/i-SURI/SELF EMPLOYED
* **Money Source:** Cash, Card

### E. Prepaid top-up to telco
* **Process:** Agent accepts cash or card for telco Prepaid top-up.
* **Validation:** Mandatory `Phone Number` validation against Telco.
* **Telco:** CELCOM, M1
* **Money Source:** Cash, Card

### F. Open Account
* **Process:** Agent do Opening Account for customer with MyKad (New Customer/ Existing Customer).
* **Validation:** validate MyKad using Agent mobile device/POS.

### G. SARAWAK PAY e-WALLET transaction
* **Process:** Agent do withdrawal/topup with SARAWAK PAY e-WALLET
* **Money Source:** Cash, Card

### H. Purchase eSSP
* **Process:** Agent do purchase eSSP cerificate for customer
* **Validation:** follow Bank Simpanan Nasional (BSN) regulation
* **Money Source:** Cash, Card

### I. Others
* **Process:** Agent do other services for customer: balance inquiry

## 3. The "Parameter Engine" (Commission & Fees)
Every transaction must consult the **Rules Service** for the following:
* **Customer Fee:** A fixed (e.g., RM 1.00) or percentage (e.g., 0.5%) fee charged to the customer.
* **Agent Commission:** The portion of the fee the agent keeps (e.g., RM 0.20 per transaction).
* **Bank Share:** The portion the bank retains.
* **Tiering:** Fees must vary based on Agent Tier (Micro-agent vs. Premier-agent).

## 4. e-KYC & Onboarding (MyKad Logic)
* **Identity:** 12-digit MyKad is the primary key.
* **Verification:** Biometric (Match-on-Card thumbprint) is the "Happy Path." 
* **Conditional STP:** * **Auto-Approve:** Match = YES + AML = CLEAN + Age $\ge$ 18.
    * **Manual Queue:** Match = NO (Face AI fallback) OR High-Risk Flag.

## 5. Settlement & Reconciliation (EOD)
* **Cut-off:** 23:59:59 MYT.
* **Net Settlement:** Agents are settled "Net." (Total Deposits - Total Withdrawals + Total Commissions).
* **Output:** The system must generate a CSV/Flat-file for the Core Banking System (CBS) upload by 02:00 AM.
Refer to `./BRD-supplementary/Net-settlement-EOD.md` for more details and explanation

## 6. STRAIGHT-THROUGH PROCESSING (STP)
Refer to `./BRD-supplementary/STP-rules.md` for more details and explanation

## 7. Security & Fraud (EFM)
* **Velocity Checks:** Limit the number of transactions per MyKad per day to prevent "Smurfing."
* **Geofencing:** Transactions must only be allowed within 100 meters of the registered Merchant GPS coordinate.
* **Encryption:** PINs must never be logged. Card numbers (PAN) must be masked (e.g., `4111********1111`).
