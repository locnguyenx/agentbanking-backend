# Agent Banking Backoffice User Guide

## Overview

The Backoffice application enables bank operations staff to manage agents, monitor transactions, review KYC applications, and generate settlement reports.

**Access:** `http://localhost:3001` (default)

---

## Navigation

| Menu Item | Description |
|-----------|-------------|
| Dashboard | Overview of daily operations |
| Agents | Manage registered agents |
| Transactions | View all transaction history |
| Settlement | Generate daily settlement reports |
| KYC Review | Approve/reject agent applications |

---

## Dashboard

The dashboard provides a real-time overview of daily operations.

### Metrics Displayed

| Metric | Description |
|--------|-------------|
| Today's Transactions | Total number of transactions processed today |
| Total Volume | Total monetary value in MYR |
| Active Agents | Number of agents currently active |

### Usage

1. Upon login, the dashboard loads automatically
2. Data refreshes automatically via the query system
3. Click on any metric to navigate to the detailed view

---

## Agents Management

### Viewing Agents

1. Navigate to **Agents** from the sidebar
2. The agent list displays:
   - **Agent Code** - Unique identifier
   - **Business Name** - Registered business name
   - **Tier** - Agent tier level (T1, T2, T3)
   - **Status** - ACTIVE, SUSPENDED, PENDING
   - **Phone Number** - Contact number

### Searching Agents

Use the search/filter bar to find agents by:
- Agent Code
- Business Name
- Status

### Agent Tiers

| Tier | Description | Float Limit |
|------|-------------|-------------|
| T1 | Starter | RM 10,000 |
| T2 | Standard | RM 50,000 |
| T3 | Premium | RM 100,000 |

---

## Transactions

### Viewing Transactions

1. Navigate to **Transactions** from the sidebar
2. View all transactions with:
   - **Transaction ID** - Unique reference
   - **Type** - Transaction type (CASH_IN, CASH_OUT, etc.)
   - **Amount** - Value in MYR
   - **Status** - COMPLETED, FAILED, PENDING
   - **Agent** - Agent code who processed
   - **Created** - Transaction timestamp

### Transaction Types

| Code | Description |
|------|-------------|
| CASH_IN | Customer deposits cash to agent |
| CASH_OUT | Customer withdraws cash from agent |
| BALANCE_INQUIRY | Check customer wallet balance |
| MINI_STATEMENT | View recent transactions |

### Filtering

Filter transactions by:
- Date range
- Transaction type
- Status
- Agent code

### Exporting

Click **Export** to download filtered transactions as CSV.

---

## Settlement Reports

### Generating Reports

1. Navigate to **Settlement** from the sidebar
2. Select date using the date picker
3. Click **Load** to retrieve settlement data

### Settlement Components

| Component | Description |
|-----------|-------------|
| Total Deposits | Sum of all CASH_IN transactions |
| Total Withdrawals | Sum of all CASH_OUT transactions |
| Total Commissions | Agent commission earned |
| Net Settlement | Deposits - Withdrawals + Commissions |

### Exporting Settlement

1. Select the settlement date
2. Click **Export CSV**
3. File downloads as `settlement-YYYY-MM-DD.csv`

### Settlement Cutoff

- Settlement cutoff time: **8:00 PM MYT**
- Reports are finalized at end of business day

---

## KYC Review Queue

### Overview

The KYC Review queue displays pending agent applications requiring verification.

### Reviewing Applications

Each application shows:

| Field | Description |
|-------|-------------|
| Verification ID | Unique application reference |
| MyKad | Masked MyKad number (e.g., XXXX-XXXX-1234) |
| Full Name | Legal name from MyKad |
| AML Status | Anti-Money Laundering check result |
| Biometric | Biometric verification result |
| Reason | Notes or rejection reason |

### Approval Workflow

1. Review applicant details
2. Verify MyKad information
3. Check AML status (must be CLEAR)
4. Verify biometric match (must be TRUE)
5. Click **Approve** or **Reject**

### Decision Codes

| Status | Action Required |
|--------|-----------------|
| CLEAR | Proceed with approval |
| FLAG | Manual review required |
| PENDING | Awaiting external check |
| REJECT | Decline application |

### Rejection Reasons

Common rejection reasons:
- Invalid MyKad details
- AML flag triggered
- Biometric verification failed
- Duplicate application
- Documents incomplete

---

## User Roles

| Role | Access Level |
|------|--------------|
| Viewer | Dashboard, Transactions (read-only) |
| Operator | All operations except KYC approval |
| Supervisor | Full access including KYC approval |
| Admin | All access + user management |

---

## Error Handling

### Common Errors

| Error | Solution |
|-------|----------|
| "Error loading data" | Refresh the page; check network connection |
| "Session expired" | Log out and log back in |
| "Permission denied" | Contact system administrator |

### Getting Help

For issues:
1. Note the error message and timestamp
2. Contact IT Support with details
3. Reference transaction ID if applicable

---

## Security Guidelines

1. **Never share credentials** - Each user has individual login
2. **Lock workstation** - Lock when stepping away
3. **Report suspicious activity** - Immediately report to supervisor
4. **Data confidentiality** - Don't discuss customer details externally
5. **Audit compliance** - All actions are logged

---

## Appendix: Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| F5 | Refresh data |
| Ctrl+F | Search/filter |
| Esc | Close modal |
