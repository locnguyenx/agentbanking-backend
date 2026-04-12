# Agent Banking Backoffice — User Guide

**Version:** 1.1  
**Last Updated:** 2026-04-10  
**Audience:** Bank Administrators, Bank Operators, Tellers, Auditors

---

## Table of Contents

1. [Getting Started](#1-getting-started)
2. [Dashboard](#2-dashboard)
3. [Agent Management](#3-agent-management)
4. [User Management](#4-user-management)
5. [Transaction History](#5-transaction-history)
6. [Settlement Reports](#6-settlement-reports)
7. [KYC Review Queue](#7-kyc-review-queue)
8. [Orchestrator Workflows](#8-orchestrator-workflows)
9. [Transaction Resolution](#9-transaction-resolution)
10. [My Profile](#10-my-profile)
11. [Keyboard Shortcuts & Tips](#11-keyboard-shortcuts--tips)
12. [Troubleshooting](#12-troubleshooting)

---

## 1. Getting Started

### 1.1 Logging In

1. Open the backoffice URL in your browser.
2. Enter your **Username** and **Password**.
3. Click **Sign In**.

| Field | Description |
|-------|-------------|
| Username | Your assigned username (e.g., `admin`, `AGT-001`) |
| Password | Your password (case-sensitive) |

**Demo Credentials (Development Only):**
- Admin: `admin` / `password`
- Agent users: `NEW-AGT-001`, `AGT-E2E-001`, etc. / `12345678`
- Staff users: `operator001`, `teller001`, etc. / `12345678`
- Agent user for testing:
  - Agent ID: 20a8c8b4-8fa2-4f9b-9acb-86c68bcb9215
  - Agent Code: `NEW-AGT-001`
  - username: `NEW-AGT-001`, password: `12345678`
### 1.2 First Login — Password Change

If you are logging in with a temporary password for the first time, you will be required to change your password before accessing any features.

1. Log in with your temporary credentials.
2. Navigate to **My Profile** (click your avatar in the top-right header → **Profile**).
3. In the **Change Password** section:
   - Enter your **Current Password** (the temporary one).
   - Enter a **New Password** (minimum 8 characters).
   - Confirm the new password.
   - Click **Change Password**.

**Password Requirements:**
- Minimum 8 characters
- Must match the confirmation password

****NOTES: ****
For External Apps, You can use `/api/v1/auth/me` which now returns your profile including agentId:
```bash
# 1. Login
POST /api/v1/auth/token
{"username": "NEW-AGT-001", "password": "12345678"}
# 2. Get profile with agentId
GET /api/v1/auth/me
Authorization: Bearer
```

### 1.3 Navigation

The backoffice uses a **collapsible sidebar** on the left:

| Navigation Item | Purpose |
|-----------------|---------|
| **Dashboard** | System overview and key metrics |
| **Agents** | Manage registered agents |
| **User Management** | Manage system users (admins, staff, agents) |
| **Transactions** | Legacy ledger transactions |
| **Orchestrator Workflows** | Monitor transaction workflows and create resolution cases |
| **Transaction Resolution** | Four-eyes approval for transaction dispute resolution |
| **Settlement** | Daily settlement reports |
| **KYC Review** | Review pending KYC verifications |
| **My Profile** | View profile and change password |

**To collapse/expand the sidebar:** Click the collapse arrow at the bottom of the sidebar.

**To log out:** Click your avatar in the top-right corner → **Logout**.

---

## 2. Dashboard

The Dashboard is your landing page and provides an at-a-glance overview of the system.

### 2.1 Stats Cards

| Card | Description |
|------|-------------|
| **Total Agents** | Number of registered agents with week-over-week trend |
| **Today's Volume** | Total transaction volume today (in RM) with trend |
| **Transactions** | Total transaction count with trend |
| **Pending KYC** | Number of agents awaiting KYC review |

### 2.2 Transaction Volume Chart

An area chart showing transaction volume over recent days. Hover over any point to see the exact RM amount.

### 2.3 Quick Stats

| Stat | Description |
|------|-------------|
| **Active Agents** | Count and percentage of agents with ACTIVE status |
| **Pending Review** | Number of agents requiring attention |
| **Successful Txns** | Percentage of completed transactions |

### 2.4 Recent Transactions

A table showing the 5 most recent transactions:

| Column | Description |
|--------|-------------|
| Transaction ID | Shortened ID (first 8 characters) |
| Agent | Agent name (truncated) |
| Type | Transaction type badge (Deposit, Withdrawal, Bill Pay, Topup, DuitNow) |
| Amount | Transaction amount in RM |
| Status | **Success** (green), **Pending** (yellow), or **Failed** (red) |
| Time | Transaction time (HH:MM) |

---

## 3. Agent Management

### 3.1 Overview

The Agents page lets you manage the full lifecycle of registered agents — from creation to deactivation — and manage their linked user accounts.

### 3.2 Stats Cards

| Card | Description |
|------|-------------|
| **Total Agents** | All registered agents |
| **Active** | Agents with ACTIVE status |
| **Suspended** | Agents with SUSPENDED status |
| **Inactive** | Agents with INACTIVE status |

### 3.3 Searching and Filtering

| Control | Description |
|---------|-------------|
| **Search** | Type to filter by business name, agent code, or phone number |
| **Status Filter** | Filter by: All Status, Active, Inactive, Suspended |
| **Tier Filter** | Filter by agent tier (Micro, Standard, Premier) |

Search and filter changes automatically reset to page 1.

### 3.4 Agent Table

| Column | Description |
|--------|-------------|
| Agent Code | Unique code (e.g., `AGT-001`) |
| Name | Business name with avatar initials |
| Phone | Contact phone number |
| Location | GPS coordinates |
| Status | **Active** (green), **Suspended** (yellow), **Inactive** (red) |
| User Account | Linked user account status (see below) |
| Tier | Agent tier label |
| Actions | Dropdown menu for agent actions |

### 3.5 User Account Status

Each agent has a linked user account for POS terminal login. The status indicators are:

| Status | Meaning |
|--------|---------|
| **Created** (green) | User account exists and is active |
| **Inactive** (yellow) | User account exists but is locked or inactive |
| **Not Created** (red) | No user account has been created yet |
| **Failed** (red) | User account creation failed — click **Retry User Creation** to try again |
| **Pending** (indigo) | User account creation is in progress |

### 3.6 Agent Actions

Click the **⋮** (three dots) button on any agent row to access:

| Action | Description |
|--------|-------------|
| **View Details** | Open the agent detail modal |
| **Edit Agent** | Edit business name, phone, or tier |
| **Create User Account** | Create a POS login account for agents without one |
| **Retry User Creation** | Retry user creation for agents with FAILED status |
| **Deactivate** | Deactivate the agent (requires confirmation) |

### 3.7 Adding a New Agent

1. Click **Add Agent** in the page header.
2. Fill in the form:

| Field | Required | Format |
|-------|----------|--------|
| Business Name | Yes | Free text |
| Phone Number | Yes | e.g., `012-3456789` |
| MyKad Number | Yes | 12 digits, no dashes |
| Agent Tier | Yes | Micro, Standard, or Premier |

3. Click **Add Agent**.

The system automatically:
- Generates a unique agent code (`AGT-001`, `AGT-002`, etc.)
- Creates a linked user account for POS login
- Sends a temporary password to the agent's phone

### 3.8 Viewing Agent Details

Click **View Details** on any agent to see:
- Agent code, business name, phone, tier
- GPS location coordinates
- Status and timestamps (created/updated)
- **User Account** section showing:
  - Current user account status
  - User ID (clickable link to navigate to User Management)
  - Error message (if user creation failed)
  - Action buttons (Create User Account / Retry User Creation)

### 3.9 Editing an Agent

1. Click **Edit Agent** from the actions menu or the View Details modal.
2. Modify any of:
   - Business Name
   - Phone Number
   - Agent Tier
3. Click **Save Changes**.

### 3.10 Deactivating an Agent

1. Click **Deactivate** from the actions menu.
2. Confirm the action in the dialog.
3. The agent status changes to INACTIVE.

---

## 4. User Management

### 4.1 Overview

The User Management page lets you manage all system users — both internal staff (Bank Admins, Operators, Tellers) and external users (agent POS accounts).

### 4.2 Stats Cards

| Card | Description |
|------|-------------|
| **Total Users** | All system users |
| **Active** | Users with ACTIVE status |
| **Locked** | Users with LOCKED status (too many failed login attempts) |
| **Inactive** | Users with INACTIVE status |

### 4.3 Searching and Filtering

| Control | Description |
|---------|-------------|
| **Search** | Filter by full name, username, or email |
| **Status Filter** | Filter by: All Status, Active, Locked, Deleted, Inactive |

### 4.4 User Table

| Column | Description |
|--------|-------------|
| Username | Login username (monospace font) |
| Full Name | Display name |
| Email | Email address |
| Status | **Active** (green), **Locked** (yellow), **Inactive** (blue), **Deleted** (red) |
| User Type | **Internal** (blue) for staff, **External** (yellow) for agents |
| Agent ID | Linked agent ID (shown only for External users, truncated) |
| Last Login | Date/time of last successful login, or "Never" |
| Actions | Dropdown menu for user actions |

### 4.5 User Actions

Click the **⋮** (three dots) button on any user row:

| Action | Description |
|--------|-------------|
| **View Details** | Open the user detail modal |
| **Edit User** | Edit username, full name, or email |
| **Reset Password** | Set a new temporary password for the user |
| **Lock User** | Lock the user account (shown when status is ACTIVE) |
| **Unlock User** | Unlock the user account (shown when status is not ACTIVE) |
| **Delete User** | Delete the user (requires confirmation) |

### 4.6 Adding a New User

1. Click **Add User** in the page header.
2. Fill in the form:

| Field | Required | Notes |
|-------|----------|-------|
| Username | Yes | Must be unique across all users |
| Full Name | Yes | Display name |
| Email | Yes | Valid email format |
| Temporary Password | Yes | Minimum 8 characters |

3. Click **Create User**.

The user is created with:
- Status: ACTIVE
- Must Change Password: true (user must change on first login)

### 4.7 Viewing User Details

Click **View Details** to see:
- Username, full name, email
- Status and user type
- Last login and created timestamps

**Tip:** You can also open a specific user's details directly by navigating to `/users?userId=<user-id>`.

### 4.8 Editing a User

1. Click **Edit User** from the actions menu.
2. Modify:
   - Username
   - Full Name
   - Email
3. Click **Save Changes**.

### 4.9 Locking / Unlocking a User

- **Lock:** Prevents the user from logging in. Use this for security concerns or policy violations.
- **Unlock:** Restores login access. Use this after investigating a locked account.

### 4.10 Resetting a User Password

1. Click **Reset Password** from the actions menu.
2. Enter a new temporary password (minimum 8 characters).
3. Click **Reset Password**.

The user will be required to change this password on their next login.

### 4.11 Deleting a User

1. Click **Delete User** from the actions menu.
2. Confirm the action in the dialog.

> **Warning:** This action cannot be undone.

---

## 5. Transaction History

### 5.1 Overview

The Transaction History page provides a searchable, filterable view of all financial transactions across the platform.

### 5.2 Stats Cards

| Card | Description |
|------|-------------|
| **Total Transactions** | All transactions in the system |
| **Successful** | Transactions with COMPLETED status |
| **Pending** | Transactions awaiting processing |
| **Failed** | Transactions that failed |

### 5.3 Searching and Filtering

| Control | Description |
|---------|-------------|
| **Search** | Filter by transaction ID or type |
| **Status Filter** | Filter by: All Status, Success, Pending, Failed |

### 5.4 Transaction Table

| Column | Description |
|--------|-------------|
| Transaction ID | Full UUID |
| Type | **Deposit**, **Withdrawal**, **Bill Pay**, **Topup**, **DuitNow** |
| Amount | Transaction amount in RM |
| Status | **Success** (green), **Pending** (yellow), **Failed** (red) |
| Agent ID | Associated agent (truncated) |
| Time | Transaction timestamp (HH:MM:SS) |
| Actions | View Details, Refund |

### 5.5 Viewing Transaction Details

Click **View Details** to see:
- Transaction type and amount (color-coded by status)
- Status badge
- Transaction ID, Agent ID
- Date and time

### 5.6 Refunding a Transaction

1. Click **Refund** from the actions menu.
2. Confirm the action in the dialog.

---

## 6. Settlement Reports

### 6.1 Overview

The Settlement page generates daily settlement reports showing total deposits, withdrawals, commissions, and net settlement amounts.

### 6.2 Selecting a Date

1. Use the **Settlement Date** date picker to select a date (defaults to today).
2. The report automatically loads for the selected date.

### 6.3 Summary Statistics

| Stat | Description |
|------|-------------|
| **Total Deposits** | Sum of all credit transactions for the selected date |
| **Total Withdrawals** | Sum of all debit transactions for the selected date |
| **Total Commissions** | Total commission earned |
| **Net Settlement** | Net amount (deposits minus withdrawals) |

### 6.4 Agent Settlement Details

A table showing individual transactions contributing to the settlement:

| Column | Description |
|--------|-------------|
| Transaction ID | Shortened ID |
| Type | Transaction type badge |
| Agent ID | Associated agent |
| Amount | Transaction amount in RM |
| Status | COMPLETED, PENDING, or other |

### 6.5 Exporting Settlement

1. Click **Export CSV** in the page header.
2. The system downloads a CSV file named `settlement-YYYY-MM-DD.csv`.

---

## 7. KYC Review Queue

### 7.1 Overview

The KYC Review Queue is where manual KYC (Know Your Customer) verifications are processed. Each item represents a pending identity verification that requires human review.

### 7.2 Stats Cards

| Card | Description |
|------|-------------|
| **Pending Review** | Number of items in the review queue |
| **Approved Today** | KYC approvals completed today |
| **Rejected Today** | KYC rejections completed today |
| **AML Flags** | Number of items flagged by Anti-Money Laundering checks |

### 7.3 Searching and Filtering

| Control | Description |
|---------|-------------|
| **Search** | Filter by full name, masked MyKad, or verification ID |
| **Priority Filter** | Filter by: High, Medium, Low |
| **AML Status Filter** | Filter by: Clean, Flagged |

### 7.4 KYC Review Cards

Each item in the queue is displayed as a card showing:

| Element | Description |
|---------|-------------|
| Avatar | Initials of the applicant |
| Full Name | Applicant's full name |
| ID | Verification ID and masked MyKad number |
| Biometric Match | **Match** (green), **Low Match** (yellow), **No Match** (red) |
| AML Status | **Clean** (green) or **Flagged** (red) |
| Rejection Reason | Previous rejection reason (if applicable) |

### 7.5 Reviewing a KYC Item

Click **View** on any card to see full details:
- MyKad number
- Biometric match status
- AML status
- Verification status
- Previous rejection reason (if any)

### 7.6 Approving a KYC Item

1. Click **Approve** on the card or in the detail modal.
2. Confirm the action in the dialog.
3. The item is removed from the queue.

### 7.7 Rejecting a KYC Item

1. Click **Reject** on the card or in the detail modal.
2. Enter a **rejection reason** when prompted.
3. The item is removed from the queue and the reason is recorded.

---

## 8. Orchestrator Workflows

### 8.1 Overview

The Orchestrator Workflows page provides real-time visibility into all transaction workflows managed by the Temporal orchestration engine. This is the central hub for monitoring transaction processing, troubleshooting stuck transactions, and initiating resolution cases for failed transactions.

**Key Features:**
- **Real-time Activity Timeline**: See exactly which workflow step is currently executing
- **Live Status Updates**: Auto-refresh every 5 seconds for pending workflows
- **Activity Details**: View input/output parameters, execution times, and error details
- **External Service Monitoring**: Track health status of Rules, Ledger, Switch, and Biller services
- **Enhanced Debugging**: Comprehensive information for troubleshooting transaction issues

### 8.2 Stats Cards

| Card | Description |
|------|-------------|
| **Total Workflows** | All workflows in the selected time period |
| **Completed** | Workflows with COMPLETED status |
| **Pending** | Workflows awaiting processing |
| **Failed** | Workflows with FAILED status |

### 8.3 Searching and Filtering

| Control | Description |
|---------|-------------|
| **Search** | Filter by workflow ID or transaction ID |
| **Date Range** | Filter by: Last 7 days, Last 30 days, Last 3 months |
| **Status Filter** | Filter by: All Status, Pending, Completed, Failed, Compensating, Pending Review |
| **Transaction Type** | Filter by: Cash Withdrawal, Retail Sale, Prepaid Topup, Bill Payment, DuitNow Transfer, Deposit |

### 8.4 Workflow Views

The Orchestrator Workflows page offers three viewing modes:

#### **Table View** (Default)
Standard tabular view with workflow details.

#### **Card View** 
Card-based layout with visual workflow progress indicators:
- **Progress Bar**: Shows completion percentage with current step highlighted
- **Activity Timeline**: Visual indicators for each workflow step
- **Service Status**: Color-coded indicators for external services
- **Real-time Updates**: Live status updates for running workflows

#### **Timeline View**
Detailed chronological view of workflow execution (coming soon).

### 8.5 Workflow Table

| Column | Description |
|--------|-------------|
| Workflow ID | Unique workflow identifier (monospace font) |
| Transaction ID | Associated transaction UUID |
| Type | Transaction type badge |
| Amount | Transaction amount in RM |
| Status | Status badge (Completed, Failed, Pending, Compensating, Pending Review) |
| Case Status | Resolution case status if applicable (Pending Maker, Pending Checker, Approved, Rejected, or "No case") |
| Created At | Workflow creation timestamp |
| Actions | View Details, Create Case |

### 8.6 Enhanced Workflow Details (New)

Click **View Details** on any workflow to access the comprehensive workflow execution details:

#### **8.6.1 Execution Summary**
- **Current Status**: Real-time workflow status with visual indicator
- **Progress Statistics**: Total activities, completed count, failed count
- **Estimated Completion**: Predicted completion time for running workflows
- **Auto-refresh Toggle**: Enable/disable automatic updates every 5 seconds

#### **8.6.2 Current Activity Panel**
For workflows with RUNNING status:
- **Activity Name**: Currently executing activity (e.g., "EvaluateSTPActivity", "AuthorizeAtSwitch")
- **Execution Time**: Time elapsed since activity started
- **Retry Information**: Current attempt number and maximum retries
- **Input Parameters**: Activity input data (expandable)
- **Error Details**: Any error messages or timeout information

#### **8.6.3 Activity Timeline**
Visual timeline showing all workflow activities:
- **Step-by-step Progress**: Each activity with status indicator
- **Execution Times**: Start time and duration for each activity
- **Status Indicators**:
  - ✅ **Completed**: Green checkmark
  - ⚠️ **Running**: Yellow circle with pulse animation
  - ❌ **Failed**: Red X mark
  - ⏰ **Scheduled**: Gray clock icon
- **Expandable Details**: Click any activity to view input/output parameters

#### **8.6.4 External Service Status**
Real-time health monitoring of external services:
- **Rules Service**: Status of business rules engine
- **Ledger Service**: Status of float/ledger management
- **Switch Adapter**: Status of payment switch connection
- **Biller Service**: Status of bill payment services

**Status Indicators:**
- 🟢 **Responding**: Service is operational and responding
- 🟡 **Available**: Service is available but not recently tested
- 🔴 **Timeout**: Service experienced timeout or failure
- ⚪ **Not Required**: Service not used in this workflow

#### **8.6.5 Debug Information**
Technical details for advanced troubleshooting:
- **Temporal Workflow ID**: Internal workflow identifier
- **History Event Count**: Number of events in workflow history
- **Recent Events**: Last 5 workflow events with timestamps
- **Export Options**: Copy debug info to clipboard or export as JSON

### 8.7 Pending Reason Display

When a workflow has PENDING or RUNNING status, enhanced pending information shows:

#### **8.7.1 Current Activity Details**
- **Activity Name**: Exact activity being executed (e.g., "CheckVelocityActivity")
- **Service Being Called**: External service (Rules, Ledger, Switch, Biller)
- **Elapsed Time**: How long the activity has been running
- **Retry Status**: Current attempt and maximum retries

#### **8.7.2 Common Pending Reasons**
- **Evaluating Business Rules**: Processing velocity checks and STP decisions
- **Blocking Float Amount**: Reserving funds in agent's float account
- **Authorizing at Switch**: Waiting for payment switch response
- **Awaiting Biller Response**: Waiting for bill payment confirmation
- **Processing Commission**: Calculating agent commission
- **Compensating Transaction**: Rolling back due to failure

### 8.8 Creating a Resolution Case

For failed workflows without an existing resolution case:

1. Click **Create Case** in the Actions column.
2. A resolution case is created and the workflow appears in the Transaction Resolution page.
3. Click **View Case** to navigate directly to the resolution details.

### 8.9 Troubleshooting Workflow Issues

#### **8.9.1 Identifying Stuck Workflows**
- Look for workflows with **RUNNING** status and long elapsed times (> 2 minutes)
- Check the **Current Activity** panel to see which step is stuck
- Review **External Service Status** for any failed services

#### **8.9.2 Common Issues and Solutions**

| Issue | Symptoms | Solution |
|-------|----------|----------|
| **Switch Timeout** | "AuthorizeAtSwitchActivity" stuck, Switch Adapter shows "Timeout" | Check switch connectivity, may need manual intervention |
| **Rules Engine Down** | "EvaluateSTPActivity" failing, Rules Service shows "Failed" | Contact technical team to restart rules service |
| **Float Insufficient** | "BlockFloatActivity" failing with insufficient funds error | Agent needs to top up their float account |
| **Biller Service Down** | "PayBillActivity" timing out, Biller Service shows "Timeout" | Check biller service status, may need to retry later |

#### **8.9.3 Debug Steps**
1. **Check Activity Timeline**: Look for failed activities in red
2. **Review Error Messages**: Click failed activities to see detailed error information
3. **Verify Service Status**: Check external service health indicators
4. **Export Debug Info**: Use the export function to share technical details with support team

### 8.10 Performance Tips

- **Auto-refresh**: Enable for real-time monitoring of active workflows
- **Date Range**: Use shorter ranges (7 days) for faster loading
- **Status Filter**: Filter by "Pending" to focus on active issues
- **Search**: Use workflow ID or transaction ID for quick lookup

---

## 9. Transaction Resolution

### 9.1 Overview

The Transaction Resolution page handles manual resolution of failed or stuck transactions. It implements a four-eyes approval workflow where:
- **Maker** proposes a resolution action (Commit or Reverse)
- **Checker** reviews and approves or rejects the proposed action

### 9.2 Stats Cards

| Card | Description |
|------|-------------|
| **Total Cases** | All resolution cases |
| **Pending Maker** | Cases awaiting maker proposal |
| **Pending Checker** | Cases awaiting checker approval |
| **Approved Today** | Cases approved today |

### 9.3 Searching and Filtering

| Control | Description |
|---------|-------------|
| **Search** | Filter by workflow ID |
| **Status Filter** | Filter by: All Status, Pending Maker, Pending Checker, Approved, Rejected |

### 9.4 Resolution Table

| Column | Description |
|--------|-------------|
| Workflow | Workflow ID (monospace) |
| Transaction | Transaction ID |
| Type | Transaction type |
| Amount | Transaction amount in RM |
| Status | Status badge |
| Actions | View Details, Propose, Approve, Reject, Force Resolve |

### 9.5 Viewing Resolution Details

Click **View Details** to see:
- Transaction type and amount
- Workflow ID and Transaction ID
- Status with visual indicator
- **Maker Pending Reason** (for PENDING_MAKER status) or **Checker Pending Reason** (for PENDING_CHECKER status)
- Agent ID
- Created At timestamp
- Reference Number
- Customer Fee
- Completed At timestamp
- Error code and message (if applicable)
- Maker proposed action and reason (if proposed)
- Checker decision and reason (if decided)

### 9.6 Proposing a Resolution (Maker Role)

For cases in PENDING_MAKER status:

1. Click **Propose** in the Actions column.
2. Fill in the form:
   - **Action**: Commit (confirm transaction) or Reverse (rollback transaction)
   - **Reason Code**: Select from predefined codes (SYSTEM_ERROR, TIMEOUT, DUPLICATE, INVALID_DATA, INSUFFICIENT_FUNDS, etc.)
   - **Reason**: Detailed explanation
   - **Evidence URL**: Optional link to supporting documentation
3. Click **Submit Proposal**.
4. The status changes to PENDING_CHECKER.

### 9.7 Approving a Resolution (Checker Role)

For cases in PENDING_CHECKER status:

1. Click **Approve** in the Actions column.
2. Enter an optional approval reason.
3. Click **Confirm Approval**.
4. The resolution action is executed.

### 9.8 Rejecting a Resolution (Checker Role)

For cases in PENDING_CHECKER status:

1. Click **Reject** in the Actions column.
2. Enter a rejection reason.
3. Click **Confirm Rejection**.
4. The case is marked as REJECTED.

### 9.9 Force Resolve (Admin Only)

For resolving cases bypassing the four-eyes workflow:

1. Click **Force Resolve** in the Actions column.
2. Select the action: Commit or Reverse.
3. Enter a reason for force resolution.
4. Click **Confirm**.
5. The resolution is executed immediately.

---

## 10. My Profile

### 10.1 Accessing Your Profile

Click your **avatar** in the top-right header → **Profile**, or navigate directly to `/profile`.

### 10.2 Viewing Your Profile

The Profile Information card displays:

| Field | Description |
|-------|-------------|
| Full Name | Your display name |
| Email | Your email address |
| Username | Your login username |
| User Type | **Internal** (bank staff) or **External** (agent) |
| Agent ID | Your linked agent ID (External users only) |
| Status | Your account status |
| Last Login | Date and time of your last successful login |

### 10.3 Changing Your Password

1. In the **Change Password** section:
2. Enter your **Current Password**.
3. Enter a **New Password** (minimum 8 characters).
4. Enter the new password again in **Confirm New Password**.
5. The system shows real-time feedback:
   - **Green text** "Passwords match" when the confirmation matches.
   - **Red text** "Passwords do not match" when they differ.
6. Click **Change Password**.

**On success:** A green toast notification appears confirming the password change.
**On failure:** A red toast notification shows the error message.

### 10.4 Password Visibility Toggle

Each password field has an **eye icon** to show/hide the password text. Click the icon to toggle visibility.

---

## 11. Keyboard Shortcuts & Tips

| Action | Shortcut / Tip |
|--------|---------------|
| **Quick search** | Click the search bar at the top of any page and start typing |
| **Navigate pages** | Use the sidebar on the left |
| **Return to dashboard** | Click the "Agent Banking" logo in the sidebar |
| **Collapse sidebar** | Click the collapse arrow at the bottom of the sidebar for more screen space |
| **View specific user** | Navigate to `/users?userId=<id>` to auto-open a user's details |

---

## 12. Troubleshooting

### 12.1 Login Issues

| Problem | Solution |
|---------|----------|
| "Invalid username or password" | Verify your credentials. Passwords are case-sensitive. |
| Account locked | Contact your administrator to unlock your account. |
| Temporary password expired | Request a password reset from your administrator. |

### 12.2 User Account Issues

| Problem | Solution |
|---------|----------|
| Cannot create user (duplicate username) | Usernames must be unique. Try a different username. |
| Cannot create user (duplicate email) | Emails must be unique. Try a different email. |
| User creation failed for agent | Check the View Agent modal for the error message. Click **Retry User Creation**. |

### 12.3 Agent Issues

| Problem | Solution |
|---------|----------|
| Agent user account shows "Failed" | View the agent details to see the error. Click **Retry User Creation**. |
| Agent user account shows "Pending" | User creation is in progress. Wait a moment and refresh the page. |
| Cannot deactivate agent with pending transactions | Ensure all transactions for the agent are completed or cancelled first. |

### 12.4 General Issues

| Problem | Solution |
|---------|----------|
| Page not loading | Check your internet connection. Try refreshing the page. |
| Session expired | You will be redirected to the login page. Log in again. |
| Action failed with error | Note the error message and contact your system administrator. |

---

## Appendix A: Status Reference

### Agent Status

| Status | Description |
|--------|-------------|
| ACTIVE | Agent is operational and can process transactions |
| SUSPENDED | Agent is temporarily suspended (e.g., compliance review) |
| INACTIVE | Agent is inactive and cannot process transactions |

### User Account Status (per Agent)

| Status | Description |
|--------|-------------|
| CREATED | User account exists and is ready for POS login |
| INACTIVE | User account exists but is locked or disabled |
| NOT_CREATED | No user account has been created yet |
| FAILED | User account creation encountered an error |
| PENDING | User account creation is in progress |

### User Status

| Status | Description |
|--------|-------------|
| ACTIVE | User can log in and access the system |
| LOCKED | User account is locked (too many failed login attempts) |
| INACTIVE | User account is inactive |
| DELETED | User account has been deleted |

### Transaction Status

| Status | Description |
|--------|-------------|
| COMPLETED | Transaction completed successfully |
| PENDING | Transaction is being processed |
| FAILED | Transaction failed |

### Workflow Status (Orchestrator)

| Status | Description |
|--------|-------------|
| PENDING | Workflow is waiting for processing |
| RUNNING | Workflow is actively being processed |
| COMPLETED | Workflow completed successfully |
| FAILED | Workflow failed |
| COMPENSATING | Workflow is compensating (rolling back) |
| PENDING_REVIEW | Workflow is pending manual review |

### Resolution Case Status

| Status | Description |
|--------|-------------|
| PENDING_MAKER | Case awaiting maker to propose resolution |
| PENDING_CHECKER | Case awaiting checker to approve/reject |
| APPROVED | Resolution was approved and executed |
| REJECTED | Resolution was rejected by checker |

### Pending Reason Values

| Reason | Description |
|--------|-------------|
| AWAITING_SWITCH_RESPONSE | Waiting for response from payment switch |
| AWAITING_BILLER_RESPONSE | Waiting for response from biller |
| AWAITING_REVIEW | Requires manual review |
| AWAITING_AGENT_CONFIRMATION | Waiting for agent confirmation |
| AWAITING_CUSTOMER_VERIFICATION | Waiting for customer verification |

---

## Appendix B: Transaction Types

| Type Code | Display Name | Description |
|-----------|-------------|-------------|
| CASH_DEPOSIT | Deposit | Customer deposits cash at agent location |
| CASH_WITHDRAWAL | Withdrawal | Customer withdraws cash at agent location |
| BILL_PAYMENT | Bill Pay | Customer pays a bill through the agent |
| PREPAID_TOPUP | Topup | Customer tops up a prepaid account |
| DUITNOW_TRANSFER | DuitNow | Customer transfers funds via DuitNow |

---

## Appendix C: User Types

| Type | Description | Access |
|------|-------------|--------|
| **Internal** | Bank staff (admins, operators, tellers) | Backoffice functions — user management, settlement, KYC review |
| **External** | Agent POS users | Agent/merchant transaction functions only |
