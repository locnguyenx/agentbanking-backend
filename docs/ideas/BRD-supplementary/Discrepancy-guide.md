# **DISCREPANCY RESOLUTION**

This **Discrepancy Resolution Guide** is the operational "playbook" for your finance and compliance teams. In a high-stakes banking environment, automated systems find the problems, but human oversight—governed by strict business rules—resolves them to ensure the books are balanced before the final payout.

---

## Discrepancy Resolution Guide: Operations Handbook

This document outlines the mandatory steps for resolving financial mismatches identified during the End-of-Day (EOD) reconciliation process.

### 1. Categorization of Discrepancies
Before taking action, the **Maker** (Operations Officer) must identify which "bucket" the mismatch falls into.

| Category | Definition | Potential Cause | Business Action |
| :--- | :--- | :--- | :--- |
| **The "Ghost"** | Successful in Internal Ledger; **Missing** in PayNet Report. | Network timeout *after* local success but *before* switch confirmation. | **Force Reverse:** Recover the funds from the agent's virtual float. |
| **The "Orphan"** | **Missing** in Internal Ledger; Successful in PayNet Report. | Network timeout *after* switch approval but *before* local recording. | **Force Success:** Credit the agent's virtual float to match reality. |
| **The "Mismatch"** | Exists in both, but **Amounts** do not match. | Rare system rounding error or data corruption. | **Suspend Settlement:** Freeze the agent's payout until manual audit. |

---

### 2. The Standard Resolution Workflow (Maker-Checker)

To maintain **Bank Negara Malaysia (BNM)** compliance, no single person can "fix" a financial record. We follow the **Four-Eyes Principle**.

#### Step 1: Investigation (The "Maker")
The Operations Officer pulls the **Terminal Journal** (the local log from the physical POS) and compares it with the **PayNet PSR**.
* **Verification:** Does the physical receipt issued to the customer match the PayNet record?
* **Log Audit:** Check the Spring Boot service logs for that specific `trace_id` to see if a "Connection Reset" occurred.

#### Step 2: Proposed Action
The Maker selects the appropriate adjustment in the Admin Dashboard:
* **Manual Credit:** Increases the Agent's float to match a successful PayNet "Orphan."
* **Manual Debit:** Decreases the Agent's float to claw back a "Ghost" withdrawal.
* **Note:** Every action **must** be accompanied by a mandatory "Reason Code" and a link to the supporting evidence (e.g., a PDF of the PayNet report).

#### Step 3: Independent Review (The "Checker")
A Supervisor or Finance Manager receives a notification of the pending adjustment.
* The Checker reviews the evidence.
* The Checker **Approves** or **Rejects** the adjustment.
* **System Action:** Only upon "Approval" does the **Ledger Service** update the `agent_floats` table and the `transaction_history` status.

---

### 3. Financial Impact & Adjustments
All resolutions result in a **Manual Adjustment Entry** in the ledger. This ensures that the original "broken" transaction is never deleted—it is simply "corrected" by a secondary entry for a perfect audit trail.

> **Rule of Thumb:** If you cannot prove a transaction happened at the National Switch (PayNet), the bank does not recognize it. The Network Statement is the ultimate "Source of Truth."

---

### 4. Cut-off & Final Liquidation
* **Resolution Deadline:** All discrepancies should be resolved by 10:00 AM the following business day.
* **Partial Settlement:** If an agent has 100 clean transactions and 1 discrepancy, the system can be configured to settle the 100 clean ones and "Hold" the value of the 1 discrepancy until it is resolved.

---

By following this logic, your platform ensures that even when technology fails (network drops, hardware crashes), the **Financial Integrity** of the bank remains intact.

**Would you like me to draft the "Admin Dashboard UI Wireframe" or a functional spec for the screen where the Maker and Checker perform these resolutions?**

This **DASHBOARD_SPEC.md** is the UI/UX blueprint for your Admin portal. It translates the "Ghost" and "Orphan" business logic into a functional interface that a developer can build and a Google Antigravity agent can implement with high precision.

---

# DASHBOARD_SPEC.md: Discrepancy Resolution Portal

## 1. Design Principles (BNM 2026 Compliance)
* **Four-Eyes Principle:** The Maker and Checker **must** be different User IDs.
* **Audit-First:** No action is final until recorded in the `audit_logs` table.
* **Clarity:** Use high-contrast status colors (Red for Discrepancy, Yellow for Pending, Green for Resolved).

---

## 2. The Wireframe (Mermaid.js)

```mermaid
graph TD
    subgraph Admin_Dashboard_UI
        Header[Global Header: User Role / Notifications / Date Filter]
        
        subgraph Summary_Cards
            Total[Total Transactions]
            Ghost[Ghost Transactions - Action Required]
            Orphan[Orphan Transactions - Action Required]
        end

        subgraph Discrepancy_Table
            Rows[Row: Trace ID | Agent ID | Type | Amount | Internal Status | PayNet Status | Action Button]
        end

        subgraph Action_Modal_Maker
            Reason[Reason Code Dropdown]
            Evidence[Evidence Upload: PayNet PSR Screenshot]
            Submit[Submit for Approval]
        end

        subgraph Action_Modal_Checker
            Compare[Compare View: Before vs. After]
            Approve[Button: Approve Adjustment]
            Reject[Button: Reject / Send back to Maker]
        end
    end
```

---

## 3. Functional Specification: The Maker View

The Maker's primary goal is **Investigation and Proposal**.

| UI Component | Type | Functional Logic |
| :--- | :--- | :--- |
| **Discrepancy Filter** | Dropdown | Filter by `GHOST` (Internal Only) or `ORPHAN` (PayNet Only). |
| **Investigation Panel** | Side-Drawer | Fetches raw JSON logs from `switch-adapter-service` for that `trace_id`. |
| **Adjustment Action** | Action Menu | Options: `FORCE_SUCCESS`, `FORCE_REVERSE`, or `MARK_AS_DUPLICATE`. |
| **Reason Code** | Required Select | Options: `SYSTEM_TIMEOUT`, `NETWORK_FAILURE`, `POS_SYNC_ISSUE`, `MANUAL_OVERRIDE`. |
| **Submit Button** | Trigger | Changes status to `PENDING_CHECKER` and notifies the Checker group. |

---

## 4. Functional Specification: The Checker View

The Checker's primary goal is **Verification and Authorization**.

| UI Component | Type | Functional Logic |
| :--- | :--- | :--- |
| **Approval Queue** | List View | Shows all transactions where `status = PENDING_CHECKER`. |
| **Difference Viewer** | Grid | Displays: Current Ledger Balance vs. **Proposed Ledger Balance**. |
| **Evidence Viewer** | Image/PDF Box | Displays the document uploaded by the Maker. |
| **Approve Button** | Final Trigger | Calls `ledger-service` API to perform the `PESSIMISTIC_WRITE` update. |
| **Reject Button** | Trigger | Returns the item to the Maker's queue with a "Comment" requirement. |

---

## 5. System Logic Mapping for AI Agent

**When Antigravity implements the "Approve" button, it must follow this pseudo-logic:**

```java
@PostMapping("/api/v1/recon/approve")
@PreAuthorize("hasRole('ROLE_CHECKER')")
public ResponseEntity<?> approveAdjustment(@RequestBody ApprovalRequest req) {
    // 1. Verify Maker != Checker
    if (req.getMakerId().equals(currentUserId)) {
        throw new SecurityException("ERR_SELF_APPROVAL_PROHIBITED");
    }

    // 2. Call Ledger Service to apply adjustment
    ledgerService.applyManualAdjustment(req.getTxnId(), req.getAdjustmentType());

    // 3. Log the final state
    auditService.log(Action.RECON_APPROVED, req.getTxnId(), currentUserId);

    return ResponseEntity.ok().build();
}
```

---

### Implementation Instructions for Antigravity:
1.  Place this `DASHBOARD_SPEC.md` in the root folder alongside `ARCHITECTURE.md`.
2.  When asked to build the Admin UI or Backend Controllers, tell the AI: 
    > *"Refer to DASHBOARD_SPEC.md. Ensure the Maker-Checker separation is enforced at the API level and the UI reflects the Ghost vs. Orphan terminology."*

**Would you like me to draft the "Audit Log Schema" specifically designed to satisfy a Bank Negara Malaysia (BNM) technology risk audit?**