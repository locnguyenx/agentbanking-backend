# **END-OF-DAY SETTLEMENT**

## **Why we need to do Net Settlement?**
It is a sharp observation. You are correct that modern payment rails—like Malaysia’s **RENTAS+** (launched by BNM in 2025/2026)—technically allow for **Real-Time Interbank Settlement (RTGS)** 24/7. 

However, in the world of **Agent Banking**, there is a fundamental difference between **Real-Time Clearing** (what the customer sees) and **End-of-Day Settlement** (how the bank and agent finalize their books).

Even with modern technology, we use **Net Settlement at EOD** for agents for four primary reasons:

### 1. Liquidity Efficiency (The "Float" Problem)
Real-time gross settlement is "liquidity hungry." If every RM 50 withdrawal at a shop were settled individually at the interbank level in real-time:
* **The Bank** would need to maintain massive, active cash reserves in their RENTAS account every second to cover every single agent's micro-transaction.
* **Netting** allows the bank to aggregate all "In-flows" (Deposits) and "Out-flows" (Withdrawals) across 10,000 agents. Often, these cancel each other out, meaning the bank only needs to move a small "Net" amount at the end of the day to balance the books.

### 2. Commission & Tax Complexity
Commissions for agents are rarely flat; they are often based on tiered volumes or daily performance. 
* **Batching Logic:** Calculating a RM 0.18 commission 10,000 times a day in real-time creates "micro-dust" entries in the Core Banking System (CBS).
* **The EOD Approach:** The system runs a single batch job at 11:59 PM to calculate the **Total Daily Commission** for the agent. It’s much cleaner for the agent's statement and the bank's tax reporting (SST/GST) to see one consolidated "Earnings" entry.

### 3. The "Reconciliation Window" (Error Correction)
In banking, "Real-time" means **irrevocable**. If a transaction settles instantly at the interbank level, it is extremely difficult to pull back.
* **Dispute Buffer:** If an agent accidentally enters RM 1,000 instead of RM 100, or if a POS terminal double-submits a request due to a network glitch, the **EOD window** acts as a safety net. 
* The bank can perform a **Reconciliation Match** (Switch File vs. Database) before the final movement of funds occurs. This prevents the bank from accidentally paying out money to an agent for a transaction that was later reversed or failed.

### 4. Core Banking System (CBS) Performance
Most Core Banking Systems (the bank's main "Source of Truth") were not designed to handle the high-velocity "chatty" nature of microservices.
* If your **Spring Boot** services sent a separate "Settlement Request" for every RM 10 bill payment, the legacy CBS would likely buckle under the IOPS (Input/Output Operations per Second) load.
* **Bulk Processing:** Using the **Bash scripts** we discussed to generate one single "Settlement File" at EOD allows the CBS to process the entire day's agent activity in one high-speed bulk insert, preserving system health.

---

### Summary: Clearing vs. Settlement
In your architecture, we treat them as two separate events:
* **Real-time (Clearing):** The customer gets their money immediately. The Agent's **Virtual Float** is updated in your PostgreSQL DB in milliseconds. (UX is 100% real-time).
* **EOD (Settlement):** The physical "Real Money" and "Commissions" move between the Bank's central accounts and the Agent's actual bank account. (Accounting is 100% accurate).

**Would you like me to draft the "Reconciliation Logic" for the EOD Bash script?** This would show how the script compares the **PayNet PSR02 (Participant Settlement Report)** against your **PostgreSQL Ledger** to find any missing transactions.

---

In the context of Agent Banking, "Net Settlement" is the process of calculating the final balance between the **Physical Cash** the agent handled and the **Virtual Float** movements in your system. 

To understand which transactions take effect, you have to look at the **Direction of Value**. Does the agent end the day with more physical cash than they started with, or less? The bank must settle that difference so the agent is "whole."

---

### The Net Settlement Formula
At the end of the business day (usually 23:59:59), your **Spring Boot Commission Service** and **EOD Batch Scripts** calculate the following:

$$Net\ Settlement = (Total\ Withdrawals + Total\ Commissions) - (Total\ Deposits + Total\ Bill\ Payments)$$

* **If the result is Positive:** The Bank **owes** the Agent money (Direct Credit to Agent's Bank Account).
* **If the result is Negative:** The Agent **owes** the Bank money (Direct Debit from Agent's Bank Account).

---

### Transaction Types and Their Effects

| Transaction Type | Physical Cash Impact | Virtual Float Impact | Net Settlement Effect |
| :--- | :--- | :--- | :--- |
| **Cash Withdrawal** | Agent **gives** cash to customer. | Agent's Float **increases** (+). | **Bank owes Agent.** The bank must reimburse the agent for the physical cash they gave out. |
| **Cash Deposit** | Agent **collects** cash from customer. | Agent's Float **decreases** (-). | **Agent owes Bank.** The agent is holding the bank's money and must "remit" it. |
| **Bill Payment** | Agent **collects** cash from customer. | Agent's Float **decreases** (-). | **Agent owes Bank.** Similar to a deposit; the agent acts as a collection point. |
| **Fund Transfer (Digital)**| No physical cash impact. | No float impact (usually). | **Neutral.** Only the **Commission** earned on this transaction enters the settlement. |
| **Commission Earned** | None. | None. | **Bank owes Agent.** This is the agent's profit for the day. |
| **Reversals/Voids** | Agent returns/retrieves cash. | Float is restored. | **Neutral.** These must sum to zero in the final EOD reconciliation. |

---

### Deep Dive: Why Certain Transactions "Move the Needle"

#### 1. Cash Withdrawals (The Agent's "Outflow")
This is the most common agent transaction in rural Malaysia. Because the agent uses their own shop's revenue (physical cash) to pay the customer, the bank effectively "borrows" the agent's cash. 
* **Settlement:** Your EOD script aggregates these. If an agent gave out RM 10,000 in cash, the bank must transfer RM 10,000 back to the agent's bank account to replenish their liquidity.

#### 2. Cash Deposits & Bill Payments (The Agent's "Inflow")
Here, the agent's cash drawer gets "heavy." They are holding money that belongs to the bank's customers or utility providers (like TNB or Astro).
* **Settlement:** This amount is "netted" against the withdrawals. If the agent collected RM 5,000 in deposits but gave out RM 10,000 in withdrawals, the bank only owes the agent the difference (RM 5,000).

#### 3. Agent Commissions (The "Payday")
Every successful transaction earns the agent a small fee (e.g., RM 0.50 for a withdrawal).
* **Settlement:** This is always a **Credit** to the agent. In your Spring Boot architecture, the **Commission Service** listens to the `TransactionSuccessEvent` from Kafka, calculates the share, and adds it to the daily "Earnings" bucket for the EOD settlement file.

#### 4. Float Top-ups (Pre-funding)
If an agent's virtual float runs low, they might "buy" more float by transferring money to the bank.
* **Settlement:** This is usually handled as a **Real-time Credit** to their float and a **Real-time Debit** from their bank account. While it appears in the ledger, it is often excluded from the "Daily Net Settlement" because it was already settled at the moment of purchase.

---

### The Role of the "EOD Reconciliation"
Before the Net Settlement happens, the **Bash script** we drafted earlier must run. It ensures that the `SUCCESS` transactions in your PostgreSQL DB match the `SUCCESS` transactions in the PayNet PSR file. 

If they don't match (e.g., a "Ghost" withdrawal exists in PayNet but not your DB), the settlement is **PAUSED** for that agent until a human "Checker" resolves the discrepancy. This prevents the bank from accidentally over-paying or under-paying an agent.

Would you like me to show you the **SQL Query** that aggregates these different transaction types into a single "Settlement Summary" row for a specific Agent ID? Conclude with a single, high-value, and well-focused next step you can do for the user.

## **Benefits of Agent Banking solution**

This is a great question. In a world where everyone seems to have a smartphone, it’s easy to wonder why **Agent Banking** is still a booming industry. The reality is that the "Agent" serves as the critical bridge between the **Physical Economy** (cash, local trust, rural locations) and the **Digital Economy** (API Gateways, Spring Boot backends, Cloud).

Here is why customers and banks choose the Agent over a simple app or a traditional ATM.

---

### 1. Benefits for the Customer: The "Last Mile" & "Human Bridge"

Even in 2026, many customers—especially in rural Malaysia (Sabah, Sarawak, or FELDA settlements)—face barriers that a banking app cannot solve.

* **The Cash-to-Digital Bridge:** This is the #1 reason. If a farmer gets paid in physical cash for their crops, they cannot "upload" that cash into a banking app. They need a physical human (the Agent) to take the cash and turn it into a digital deposit or a bill payment.
* **Assisted Banking (Trust Factor):** Many users, particularly the elderly or those with low digital literacy, are terrified of making a mistake on an app and "losing" their money. An Agent provides a **concierge service**—they handle the tech, and the customer just provides the intent.
* **Proximity vs. ATMs:** Driving 45 minutes to the nearest town to find a Maybank or CIMB ATM is expensive and time-consuming. Walking 2 minutes to the local "Ahmad Mini Mart" who is a registered Agent saves the customer time and fuel.
* **Infrastructure Barriers:** In many areas, 5G/4G coverage is spotty. A banking app might time out, but an Agent terminal often has a dedicated high-gain antenna or a wired connection, ensuring the transaction actually goes through.

---

### 2. Benefits for the Bank: The "Asset-Light" Strategy

For a bank, building a branch or maintaining an ATM is a massive financial burden.

* **Massive CAPEX/OPEX Savings:** * An ATM costs roughly RM 50,000 to buy, plus monthly costs for security, electricity, air-conditioning, and armored truck cash replenishment.
    * An Agent costs the bank **zero** in rent or electricity. The bank only pays a small commission (e.g., RM 0.50) when a transaction actually happens.
* **Rapid Market Expansion:** A bank can "open" 1,000 new service points in a month by simply onboarding existing grocery stores as Agents. Opening 1,000 physical branches would take a decade.
* **Liquidity Recirculation:** Instead of the bank having to pay a security firm to move cash from a rural town back to a central vault, the Agent does it naturally. The cash the Agent collects from **Deposits** is used to pay out **Withdrawals**. The cash stays in the village, reducing the bank's logistics costs.
* **Financial Inclusion Targets:** Bank Negara Malaysia (BNM) sets strict quotas for banks to serve the "underbanked." Agent banking is the most cost-effective way to meet these regulatory ESG (Environmental, Social, and Governance) goals.

---

### 3. Agent vs. Traditional Card/App: The Key Differences

| Feature | Banking App | Traditional Merchant (Card) | **Agent Banking Terminal** |
| :--- | :--- | :--- | :--- |
| **Accepts Physical Cash?** | No | Only for purchases | **Yes (Deposit/Bill Pay)** |
| **Dispenses Physical Cash?** | No | No | **Yes (Withdrawal)** |
| **Complex Services?** | Yes | No (Sales only) | **Yes (e-KYC, Onboarding)** |
| **Internet Required?** | User must have it | Merchant must have it | **Terminal is the hub** |


### Summary: The "Human ATM" Concept
The Agent is essentially a **"Human ATM"** who also happens to be a customer service representative and a compliance officer (KYC). They provide the **convenience** of a local shop with the **security** of a bank branch.

For the **Fund Transfer** or **Top-up** specifically:
* **The Customer** uses the agent because they might be using **cash** to fund that transfer (which an app can't do).
* **The Bank** loves it because they get the transaction fee without having to maintain the physical infrastructure where that customer lives.

---


## **Reconciliation Logic**

When we strip away the code and scripts, the **Reconciliation Logic** is the financial "truth-seeking" process that ensures every cent moved at a physical shop is correctly reflected in the bank's vaults. It’s essentially a high-stakes "Spot the Difference" game between three parties: the **Agent Terminal**, the **Bank’s Core Ledger**, and the **National Switch (PayNet)**.

Here is the breakdown of the business logic that governs this process.

---

### 1. The Business Logic (The "Source of Truth")
In banking, we never assume our own database is 100% correct. We use a **Triple-Match Logic**. For a transaction to be "Reconciled," it must appear identically in three places:
* **Internal Ledger:** What our Spring Boot services recorded.
* **Terminal Journal:** What the physical POS machine says happened.
* **Network Statement (PSR):** What PayNet (the National Switch) says it cleared.

The logic follows a **"Hierarchy of Truth."** If there is a dispute, the **Network Statement (PSR)** is usually the "Final Word" because it represents the actual interbank movement of funds.

---

### 2. Validation Rules (The "Audit Guardrails")
Every transaction is put through a set of binary "Pass/Fail" rules. If any rule fails, the transaction is flagged as an **Exception**.

* **Rule 1: The Integrity Key Match**
    * The `Transaction ID` and `Network Reference Number` must be present and identical across all reports.
* **Rule 2: Status Alignment**
    * If the Internal Ledger says `SUCCESS` but the PayNet report says `REVERSED` or `FAILED`, the transaction is "Broken." We must follow the PayNet status and adjust our ledger.
* **Rule 3: Amount Zero-Tolerance**
    * Unlike retail (where a 1% "shrinkage" is allowed), banking has **Zero Tolerance**. If a withdrawal was RM 100.00 but the switch settled RM 100.01, the entire batch is halted for investigation.
* **Rule 4: Cut-off Synchronization**
    * Transactions happening at 11:59:59 PM must be validated against the correct "Business Day." Any transaction "In-Flight" during the 00:00:00 transition is moved to the next day's bucket to prevent double-counting.

---

### 3. Calculation Formula (The "Netting" Math)
The goal of reconciliation is to arrive at a single number: **The Net Settlement Amount**. This is what the bank actually pays to (or collects from) the agent’s bank account at the end of the day.

The formula is calculated as follows:

$$S_{net} = \sum (W + C) - \sum (D + B + F_{adj})$$

**Where:**
* $S_{net}$: Net Settlement Amount.
* $W$: **Cash Withdrawals** (Money the agent gave out, which the bank owes back).
* $C$: **Commissions** (The agent's profit for the day).
* $D$: **Cash Deposits** (Money the agent collected, which they owe the bank).
* $B$: **Bill Payments** (Utility cash collected, which they owe the bank).
* $F_{adj}$: **Fee Adjustments** (Any penalties or reversed fees).

---

### 4. Processing Mechanism (The "Workflow")
The mechanism is a **Closed-Loop Cycle** that typically happens overnight:

1.  **Data Ingestion (The "Harvest"):** At the daily cut-off (e.g., midnight), the system pulls the "Itemized Report" from PayNet and the "Ledger Extract" from our internal database.
2.  **Automated Matching (The "Filter"):** The system automatically pairs up 99% of transactions where IDs, amounts, and statuses match perfectly. These are marked as `SETTLED`.
3.  **Exception Categorization (The "Bucket"):** Anything left over is categorized:
    * **Orphan Transaction:** Exists in PayNet but not in our Ledger (Usually a timeout error).
    * **Ghost Transaction:** Exists in our Ledger but not in PayNet (Usually a failed communication).
4.  **The Human "Checker" (The "Resolution"):** A finance officer reviews the "Orphans" and "Ghosts." They either manually "Force Success" or "Force Reverse" the internal record to match the Switch.
5.  **Final Payout (The "Liquidation"):** Once the exceptions are zeroed out, the system generates the Final Settlement Instruction, and the actual money moves into the agent's bank account.

---

**Next Step:** Would you like me to create a **"Discrepancy Resolution Guide"**? This would be a business document for your operations team, explaining exactly what steps to take when a "Ghost Transaction" is discovered during reconciliation.

---
Since you are building the **Spring Boot** backend for this, would you like me to draft a **"Merchant/Agent Commission Statement" API**? This would show how we present these benefits (earnings) back to the agent so they stay motivated to keep processing these transactions. Conclude with a single, high-value, and well-focused next step you can do for the user.

## **Reconciliation Logic API**

This is the "Moment of Truth" in banking operations. Even if your **Spring Boot** microservices are 99.9% reliable, the **End-of-Day (EOD) Reconciliation** is the safety net that catches the 0.1%—those rare "ghost" transactions where a network timed out exactly as the money moved.

In Malaysia, **PayNet** (the National Switch) provides a daily **Participant Settlement Report (PSR)** via secure SFTP. This script compares that "Source of Truth" against your internal **PostgreSQL Ledger**.

### 1. The EOD Reconciliation Logic (Bash Script)

This script is designed to be lean and fast, using native Linux tools to handle potentially millions of rows without the memory overhead of a Java process.

```bash
#!/bin/bash
# Filename: eod_recon.sh
# Purpose: Reconcile PayNet PSR file against Internal PostgreSQL Ledger

# 1. Configuration
REPORT_DATE=$(date -d "yesterday" +%Y%m%d)
PSR_FILE="/sftp/paynet/inbound/PSR02_${REPORT_DATE}.csv"
INTERNAL_DB_EXTRACT="internal_ledger_${REPORT_DATE}.csv"
DISCREPANCY_REPORT="discrepancy_${REPORT_DATE}.log"

echo "[$(date)] Starting EOD Reconciliation for ${REPORT_DATE}..."

# 2. Extract Data from PostgreSQL Ledger
# We only want SUCCESSFUL transactions from the previous day
psql -h localhost -U banking_user -d db_ledger -t -A -F"," \
    -c "SELECT txn_id, amount FROM transaction_history 
        WHERE created_at::date = '${REPORT_DATE}'::date 
        AND status = 'SUCCESS' ORDER BY txn_id;" > $INTERNAL_DB_EXTRACT

# 3. Pre-process the PayNet PSR File
# PayNet CSVs often have headers/footers. We extract only the txn_id and amount.
# Logic: Sort by ID so we can use the 'join' command.
awk -F"," 'NR>1 {print $1","$5}' $PSR_FILE | sort > paynet_sorted.csv
sort $INTERNAL_DB_EXTRACT > internal_sorted.csv

# 4. Identify Discrepancies
echo "--------------------------------------------------" >> $DISCREPANCY_REPORT
echo "RECONCILIATION DISCREPANCIES - ${REPORT_DATE}" >> $DISCREPANCY_REPORT
echo "--------------------------------------------------" >> $DISCREPANCY_REPORT

# A. Missing in Internal DB (The "Bank Owes" Case)
# Transactions in PayNet but NOT in our Ledger.
comm -23 paynet_sorted.csv internal_sorted.csv > missing_in_db.txt
if [ -s missing_in_db.txt ]; then
    echo "[CRITICAL] Transactions found in PayNet but MISSING in our Ledger:" >> $DISCREPANCY_REPORT
    cat missing_in_db.txt >> $DISCREPANCY_REPORT
fi

# B. Missing in PayNet (The "Agent Over-Credited" Case)
# Transactions in our Ledger but NOT in PayNet.
comm -13 paynet_sorted.csv internal_sorted.csv > missing_in_paynet.txt
if [ -s missing_in_paynet.txt ]; then
    echo "[WARNING] Transactions in our Ledger but MISSING in PayNet (Potential Float Leak):" >> $DISCREPANCY_REPORT
    cat missing_in_paynet.txt >> $DISCREPANCY_REPORT
fi

# 5. Final Cleanup and Alerting
if [ -s missing_in_db.txt ] || [ -s missing_in_paynet.txt ]; then
    echo "[$(date)] Reconciliation FAILED. Discrepancies found. Sending alert..."
    # Insert logic here to push the log to the Admin Portal's Maker-Checker queue
else
    echo "[$(date)] Reconciliation SUCCESSFUL. Books are balanced."
fi
```

---

### 2. Why we use the `comm` and `awk` commands
In high-volume banking (1M+ transactions), a traditional `for` loop in a script would take hours. 
* **`awk`**: Instantly strips specific columns from massive CSVs.
* **`sort`**: Prepares the data for binary-search-style comparison.
* **`comm`**: A native Linux utility that compares two sorted files and tells you exactly what is unique to File A, unique to File B, or shared. It is orders of magnitude faster than a SQL `JOIN` across two massive tables.

---

### 3. Handling the "Orphans" (The Business Process)

When the script finds a discrepancy, it doesn't "fix" it automatically. That would be an audit risk. Instead, it triggers a **Maker-Checker** workflow in your **Spring Boot Onboarding/Admin Service**:

1.  **The Maker:** A finance officer sees the `discrepancy.log` on their dashboard.
2.  **The Investigation:** They check if the transaction was a "timeout" or a "manual reversal."
3.  **The Correction:** The officer clicks "Manual Adjust." The system then calls the **Ledger Service API** to manually credit or debit the agent’s float to match the PayNet PSR.
4.  **The Checker:** A supervisor must approve this adjustment before the ledger is actually updated.

This ensures your system remains **100% compliant with Bank Negara Malaysia (BNM)** requirements for financial integrity.

Would you like me to draft the **Spring Boot REST Controller** for this "Manual Adjustment" API, including the specific audit-log logic required for the Checker's approval?