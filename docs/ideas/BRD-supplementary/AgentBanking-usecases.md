# **REALISTIC USECASES**

## Cashless Purchasing

Absolutely. In the industry, this is often called **Merchant Acquiring** or **Retail Purchase via Agent**, and it is one of the most powerful ways to move toward a "Cashless Society" in rural areas.

In this scenario, the agent wears two hats: they are a **Shopkeeper** selling you a bag of rice, and they are a **Banking Agent** using the terminal to settle that sale digitally.

---

### 1. Common Scenarios for Non-Cash Purchases

Here are the three ways a customer pays for a service/product using your Agent Banking solution:

#### **A. The "Card-Purchase" (Debit Card)**
Instead of the customer withdrawing RM 50 (Cash-Out) and then handing that RM 50 back to the agent to pay for groceries, they simply perform a **Purchase Transaction**.
* **The Flow:** Customer inserts card $\rightarrow$ enters PIN $\rightarrow$ the system deducts RM 50 from the Customer's Account and **instantly credits** the Agent's Virtual Float.
* **Why?** It’s safer for the agent (no cash to store) and faster for the customer.

#### **B. DuitNow QR (Dynamic or Static)**
The Agent Banking terminal generates a **Dynamic QR Code** on its screen for the exact amount of the purchase (e.g., RM 12.50).
* **The Flow:** Customer opens their banking app (MAE, CIMB OCTO, etc.) $\rightarrow$ scans the terminal screen $\rightarrow$ confirms payment.
* **The Backend:** Your **Spring Boot API Gateway** receives a notification from PayNet and updates the Agent's Ledger in real-time.

#### **C. DuitNow Request (Request-to-Pay)**
This is the "Digital Invoice" method.
* **The Flow:** The agent enters the customer’s mobile number into the terminal. The customer gets a pop-up on their phone: *"Ahmad Mini Mart requests RM 45.00 for Grocery Bill. Approve?"*
* **The Benefit:** The customer doesn't even need to be standing at the counter; they could be at the back of the shop or even at home.

---

### 2. The Unique Benefits of This Scenario

| Feature | For the Customer | For the Agent | For the Bank |
| :--- | :--- | :--- | :--- |
| **Security** | No need to carry a wallet full of cash to the shop. | **Reduced Cash-on-Hand.** Less risk of robbery or theft. | Less physical cash to move/reconcile. |
| **Liquidity** | Funds stay in their interest-bearing account until the second of purchase. | **Instant Float Top-up.** The sale money becomes "Float" they can use for other withdrawals. | Higher **Interchange Fees** (MDR) compared to cash withdrawals. |
| **Record Keeping** | Digital receipt in the app for every purchase. | Automated EOD sales reports. No "Short-change" errors. | Better data on rural spending patterns (useful for credit scoring). |

---

### 3. How the Architecture Handles This

In your **Spring Boot** backend, this is handled by a **"Purchase Strategy"**.

Unlike a "Withdrawal" (where the bank owes the agent money), a **Purchase** means the **Agent owes the Bank** (or the bank keeps the money for the agent).

* **Ledger Impact:**
    1.  Customer Account $\rightarrow$ Debit RM 100.
    2.  Agent Virtual Float $\rightarrow$ **Credit** RM 100.
* **Settlement Logic:**
    At the end of the day, these purchases are **Netted** against the withdrawals. If an agent did RM 1,000 in card sales and gave out RM 1,000 in cash withdrawals, their **Net Settlement is RM 0**. They don't need to move any money—the system has perfectly "recirculated" the cash.

---

### 4. The "Hybrid" Scenario: Cash-Back
This is very popular in the UK and Australia, and starting to grow in Malaysia. A customer buys RM 20 worth of bread but asks for **"RM 50 Cash Back."**
1.  Terminal swipes for **RM 70**.
2.  RM 20 pays for the bread.
3.  Agent hands RM 50 in physical cash to the customer.
4.  The system handles the split accounting automatically.

**Would you like me to update the `DATABASE_ERD.md` to include a `MERCHANT_SALES` table to track these non-cash retail purchases separately from the banking transactions?**

# **AGENTBANKING FUNCTIONALITY COVERAGE**

**Questions:** are these cases covered in the functionalities of our Agent Banking?

That is a sharp catch. Currently, our `BRD_SUMMARY.md` focuses heavily on **Banking Services** (moving money for the customer), but it hasn't explicitly defined **Merchant Acquiring** (the agent acting as a retail store). 

To answer your question: **Technically, the pipes are there, but the business logic needs to be added.**

If we don't differentiate these cases, your reconciliation and commission logic will break because a "Withdrawal" (where the bank owes the agent) is the mathematical opposite of a "Purchase" (where the agent owes the bank/merchant).

---
## Feature Specifications

Each feature should have description about:
- **Purpose**: answer question why?
- **Transaction Type**: Withdrawal / Deposit / Bill Pay / Goods & Services Purchase
- **Payment Method**: Customer pays by **Card/QR**, pays or **Cash** to the agent
- **Agent's Role**: Why we need the Agent in this case? What the Agent do?
- **Output / Receipt Type**: e.g Banking Slip / sales receipt / A printed slip with a **16-digit PIN code**.
- **Ledger Impact**: e.g Agent's Float **Increases** (Credit) / **Decreases** (Debit) / Float increases (Instant Settlement)
- **Fee Structure**: e.g Agent earns a Commission / Agent pays a **MDR** (Merchant Discount Rate)

---

This is the definitive "Feature Map" for your platform. Having these descriptions in a central document is vital for keeping your **Google Antigravity** agent aligned and ensuring your **Spring Boot** service logic (specifically the `TransactionType` and `LedgerService`) correctly handles the accounting for every scenario.

---

### 1. Cash Withdrawal (Cash-Out)
* **Purpose**: Allows customers in rural or underserved areas to access physical cash without traveling to a distant bank branch or ATM.
* **Transaction Type**: **Withdrawal**
* **Payment Method**: Customer uses **ATM Card (EMV Chip + PIN)**.
* **Agent's Role**: The Agent acts as a "Human ATM." They authenticate the customer, verify the digital approval, and hand over physical cash from their own shop's drawer.
* **Output / Receipt Type**: **Banking Slip** (showing the withdrawal amount and remaining bank balance).
* **Ledger Impact**: **Agent's Float Increases (Credit)**. The bank owes the agent for the cash they handed out.
* **Fee Structure**: **Agent earns a Commission** (e.g., RM 0.50 per transaction).

### 2. Cash Deposit (Cash-In)
* **Purpose**: Enables customers to move physical cash into a digital bank account (their own or someone else's) for savings or further digital spending.
* **Transaction Type**: **Deposit**
* **Payment Method**: Customer pays **Cash** to the agent.
* **Agent's Role**: The Agent is a "Cash Collector." They receive physical bills, count them, and trigger the digital credit via the terminal.
* **Output / Receipt Type**: **Banking Slip** (Confirmation of deposit and reference ID).
* **Ledger Impact**: **Agent's Float Decreases (Debit)**. The agent is now holding the bank's money and must "remit" it via the float system.
* **Fee Structure**: **Agent earns a Commission** (often a percentage or a small fixed fee).

### 3. Fund Transfer (DuitNow / RTP)
* **Purpose**: To send money to another person instantly using a phone number or NRIC without needing a card or a smartphone app.
* **Transaction Type**: **Withdrawal / Fund Transfer**
* **Payment Method**: Customer pays by **Card/QR** or **Cash**.
* **Agent's Role**: The Agent provides a secure terminal hub. They facilitate the "Request-to-Pay" or card-based transfer for users who aren't tech-savvy or don't have internet access.
* **Output / Receipt Type**: **Banking Slip** (Transfer confirmation with Beneficiary details).
* **Ledger Impact**: **Agent's Float Decreases (Debit)** (if funded by cash) OR **Neutral** (if funded by card-to-account).
* **Fee Structure**: **Agent earns a Commission**.

### 4. Bill Payment (Utility/JomPAY)
* **Purpose**: Allows customers to pay utility bills (Electricity, Water, Internet) in cash at a local convenient location.
* **Transaction Type**: **Bill Pay**
* **Payment Method**: Customer pays **Cash** to the agent.
* **Agent's Role**: The Agent acts as a Payment Aggregator. They validate the bill account number (Ref-1) and collect the payment on behalf of the biller.
* **Output / Receipt Type**: **Official Biller Receipt / Slip** (showing the payment reference).
* **Ledger Impact**: **Agent's Float Decreases (Debit)**.
* **Fee Structure**: **Agent earns a Commission** (often shared between the bank and the biller).

### 5. Retail Purchase (Merchant Acquiring)
* **Purpose**: Allows customers to pay for physical goods (groceries, supplies) at the agent's shop using a card or QR instead of cash.
* **Transaction Type**: **Goods & Services Purchase**
* **Payment Method**: Customer pays by **Card/QR**.
* **Agent's Role**: The Agent is the **Merchant**. They use the terminal to accept a digital payment for their own products.
* **Output / Receipt Type**: **Sales Receipt** (Merchant copy and Customer copy).
* **Ledger Impact**: **Float Increases (Instant Settlement)**. The sale amount is added to the agent's float immediately.
* **Fee Structure**: **Agent pays a Merchant Discount Rate (MDR)** (e.g., $1.0\%$ of the sale value).

### 6. PIN Purchase (Digital Vouchers)
* **Purpose**: Provides customers with a 16-digit secret code to reload prepaid mobile phones, gaming accounts, or streaming services.
* **Transaction Type**: **Goods & Services Purchase (Digital)**
* **Payment Method**: Customer pays **Cash** to the agent.
* **Agent's Role**: The Agent is a **Digital Reseller**. They sell the bank's inventory of digital PINs to the customer for cash.
* **Output / Receipt Type**: **Printed Slip with 16-digit PIN code**.
* **Ledger Impact**: **Agent's Float Decreases (Debit)**. The agent "buys" the PIN from the system using their float.
* **Fee Structure**: **Agent earns a Commission** (built into the discount on the PIN price).

---

### Summary Table for AI Logic

| Feature | Cash Handled? | Float Change | Agent Profit |
| :--- | :--- | :--- | :--- |
| **Withdrawal** | Agent $\rightarrow$ Cust | **Increase (+)** | Commission |
| **Deposit** | Cust $\rightarrow$ Agent | **Decrease (-)** | Commission |
| **Retail Sale** | None (Digital) | **Increase (+)** | Sale - MDR |
| **PIN Purchase**| Cust $\rightarrow$ Agent | **Decrease (-)** | Commission |


## 1. The Distinction: Banking vs. Retail
For your Spring Boot backend to handle this, we need to treat "Retail Purchases" as a separate transaction category.

| Feature | **Banking Service (Current)** | **Retail Purchase (New Case)** |
| :--- | :--- | :--- |
| **Transaction Type** | Withdrawal / Deposit / Bill Pay | **Goods & Services Purchase** |
| **Agent Role** | Cash Handler / Teller | **Merchant / Shopkeeper** |
| **Ledger Impact** | Float increases (Withdrawal) | **Float increases (Instant Settlement)** |
| **Fee Structure** | Agent earns a Commission (e.g., RM 0.50) | Agent pays a **MDR** (Merchant Discount Rate) |
| **Receipt Type** | Banking Slip | **Tax Invoice / Sales Receipt** |

---

## 2. Updated Requirement: The "Merchant" Module
To cover these cases, we need to add a section to your **BRD** and **Architecture** for **Merchant Services**.

### Functional Logic for Purchases:
1.  **Selection:** Agent enters the "Retail Mode" on the POS.
2.  **Payment Method:** Customer chooses Card (Debit/Credit), DuitNow QR, or DuitNow Request-to-Pay.
3.  **The "Swap":**
    * The system debits the Customer's bank account.
    * Instead of giving physical cash, the agent gives the **product** (e.g., a bag of rice).
    * The Agent’s **Virtual Float** is credited with the purchase amount minus the MDR fee.
4.  **Reconciliation:** These retail sales are added to the EOD "Inflow" bucket.

---

## 3. Revised BRD Section: Merchant Services

You should tell Antigravity to append this to your `BRD_SUMMARY.md`:

> ### E. Merchant Acquiring (Retail Sales)
> * **Scenario:** Customer pays for store products via the Agent Terminal using Card or QR.
> * **Settlement:** Real-time credit to Agent Virtual Float.
> * **Fees:** Merchant Discount Rate (MDR) applied per transaction (e.g., 1%). 
> * **Hybrid Flow (Cash-Back):** Allow a single card swipe to cover both a Product Purchase and a Cash Withdrawal (Split-Accounting).

---

## The Comparison: "Cashless Retail" vs. "PIN Purchase"

Actually, that’s a very common point of confusion. While it sounds like it could be for cashless retail, in the specific context of **Agent Banking** (especially in Malaysia with banks like BSN, Maybank, or CIMB), the **"PIN Purchase"** feature is usually for something else entirely.

## The Short Answer: It’s likely for Digital Goods
Most of the time, "PIN Purchase" on an Agent Terminal refers to **buying a Digital PIN or Voucher code** (like a mobile reload, gaming PIN, or utility top-up), rather than paying for a physical grocery item with your card.

---

To keep your Google Antigravity project and your team aligned, you should distinguish between these two features on your POS terminal:

| Feature | **Cashless Retail (Acquiring)** | **PIN Purchase (Digital Voucher)** |
| :--- | :--- | :--- |
| **What is being bought?** | Physical goods (Rice, Milk, Soap). | A digital code (Digi/Hotlink PIN, Netflix, Razer Gold). |
| **Payment Method** | Customer pays by **Card/QR**. | Customer usually pays **Cash** to the agent. |
| **Agent's Role** | Shopkeeper accepting payment. | Reseller of digital "inventory." |
| **Output** | A sales receipt. | A printed slip with a **16-digit PIN code**. |
| **Virtual Float Impact** | Agent's Float **Increases** (Credit). | Agent's Float **Decreases** (Debit). |

---

### Why is it called "PIN Purchase"?
In many rural areas, customers don’t do "Direct Top-ups" where the credit appears on their phone instantly. Instead, they buy a **Physical PIN**.
1. The customer gives the agent RM 10.00 cash.
2. The agent selects **"PIN Purchase"** $\rightarrow$ **"Digi RM 10"**.
3. The terminal prints a small slip of paper with a **secret PIN code**.
4. The customer keys that code into their phone later to get the credit.

> **Crucial for your Logic:** In your **Spring Boot** system, a "PIN Purchase" is actually a form of **Bill Payment/Inventory Sale**. The Agent is using their **Virtual Float** to "buy" that PIN from the bank's stock and then selling it to the customer for cash.

---

### Does "Cashless Purchasing" use a PIN?
Yes, but the terminal menu usually calls that **"Retail Purchase"** or **"Sale."**
When a customer uses their debit card to pay for groceries at the agent's shop:
1. The agent selects **"Sale"** or **"Purchase"** (not PIN Purchase).
2. The customer inserts their card and enters their **ATM PIN**.
3. This "PIN" is for **authentication**, not the product itself.

### How to update your `BRD_SUMMARY.md`:
You should ask your Antigravity agent to create a clear distinction in the code between:
* `TXN_TYPE: RETAIL_SALE` (Customer pays Agent via Card)
* `TXN_TYPE: PIN_PURCHASE` (Agent sells Digital Voucher to Customer)

**Would you like me to update the `TransactionType` Enum in your Spring Boot code to include these distinct categories?** Conclude with a single, high-value, and well-focused next step you can do for the user.


