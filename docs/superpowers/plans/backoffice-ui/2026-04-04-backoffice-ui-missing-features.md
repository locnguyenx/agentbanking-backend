# Backoffice UI - Missing Features Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement missing backoffice UI features identified in the BRD/BDD specs for Agent & User Management, plus User Profile and Change Password functionality.

**Architecture:** 
- Tasks 1-5: Frontend-only changes to existing React pages. All required API endpoints already exist.
- Task 6: Backend + Frontend — need to create `GET /auth/me` endpoint (backend missing), then build Profile page UI.

**Tech Stack:** React 18 + TypeScript, TanStack React Query v5, Axios, Vitest + Testing Library, Spring Boot 3.x (Java 21)

---

## File Map

| File | Responsibility |
|------|---------------|
| `backoffice/src/api/client.ts:19-30` | Add missing fields to `User` interface |
| `backoffice/src/pages/Agents.tsx:395-421` | Add FAILED/PENDING status badges + retry button |
| `backoffice/src/pages/Agents.tsx:730-788` | Show error message in View Agent modal for FAILED status |
| `backoffice/src/pages/UserManagement.tsx:270-310` | Add agentId column for EXTERNAL users |
| `backoffice/src/pages/UserManagement.tsx:286-291` | Add INACTIVE/DELETED badge styling |
| `backoffice/src/pages/UserManagement.tsx:587-620` | Show agentId, mustChangePassword, temporaryPasswordExpiresAt in View User modal |
| `backoffice/src/pages/UserManagement.tsx:242-259` | Fix stats to track INACTIVE/DELETED instead of DISABLED |
| `services/auth-iam-service/.../web/UserController.java` | Add `GET /auth/me` endpoint for current user profile |
| `gateway/src/main/resources/application.yaml` | Add route for `/api/v1/auth/me` |
| `backoffice/src/pages/Profile.tsx` | New Profile page (view profile + change password) |
| `backoffice/src/api/client.ts` | Add `getMyProfile()` and `changeMyPassword()` API functions |
| `backoffice/src/components/Layout.tsx` | Add Profile link to user avatar dropdown |

---

### Task 1: Update User API Interface with Missing Fields [DONE]

**BDD Scenarios:** FR-4.2 (mustChangePassword), FR-4.1 (temporaryPasswordExpiresAt), FR-1.3 (agentId)

**BRD Requirements:** FR-1.3, FR-4.1, FR-4.2, FR-4.4

**User-Facing:** NO

**Files:**
- Modify: `backoffice/src/api/client.ts:19-30`

- [ ] **Step 1: Update the User interface in client.ts**

Add these fields to the `User` interface (around line 19-30):
```typescript
interface User {
  userId: string
  username: string
  email: string
  fullName: string
  status: string
  userType: 'INTERNAL' | 'EXTERNAL'
  agentId?: string | null        // NEW: linked agent for EXTERNAL users
  mustChangePassword?: boolean   // NEW: first-login password change required
  temporaryPasswordExpiresAt?: string  // NEW: when temp password expires
  createdAt?: string
  lastLoginAt?: string
}
```

- [ ] **Step 2: Run tests to verify nothing breaks**

```bash
cd backoffice && npm test -- --run 2>&1 | tail -10
```
Expected: All 139 tests still pass.

---

### Task 2: Add FAILED and PENDING Status Handling in Agents Table [DONE]

**BDD Scenarios:** S6.2 (failed status check), S1.6 (PENDING status), FR-8.3 (error message)

**BRD Requirements:** FR-7.1, FR-8.3

**User-Facing:** YES

**Files:**
- Modify: `backoffice/src/pages/Agents.tsx:395-421` (status rendering)
- Modify: `backoffice/src/pages/Agents.tsx:495-512` (action menu - add retry for FAILED)
- Test: `backoffice/src/test/Agents.test.tsx`

- [ ] **Step 1: Add FAILED and PENDING status badges in the table**

In the User Account column (lines 395-421), add handlers for FAILED and PENDING:

```tsx
// Replace the existing status rendering block (lines 395-421)
{(() => {
  const userStatus = getAgentUserStatus(agent.agentId)
  if (!userStatus) return <span style={{ color: '#94a3b8' }}>Loading...</span>
  const status = userStatus.status
  if (status === 'CREATED' || status === 'ACTIVE') {
    return (
      <span style={{ display: 'flex', alignItems: 'center', gap: 4, color: '#10b981', fontSize: 12, fontWeight: 500 }}>
        <CheckCircle size={12} /> Created
      </span>
    )
  } else if (status === 'INACTIVE' || status === 'LOCKED') {
    return (
      <span style={{ display: 'flex', alignItems: 'center', gap: 4, color: '#f59e0b', fontSize: 12, fontWeight: 500 }}>
        <Clock size={12} /> Inactive
      </span>
    )
  } else if (status === 'NOT_CREATED') {
    return (
      <span style={{ display: 'flex', alignItems: 'center', gap: 4, color: '#ef4444', fontSize: 12, fontWeight: 500 }}>
        <XCircle size={12} /> Not Created
      </span>
    )
  } else if (status === 'FAILED') {
    return (
      <span style={{ display: 'flex', alignItems: 'center', gap: 4, color: '#dc2626', fontSize: 12, fontWeight: 500 }}>
        <XCircle size={12} /> Failed
      </span>
    )
  } else if (status === 'PENDING') {
    return (
      <span style={{ display: 'flex', alignItems: 'center', gap: 4, color: '#6366f1', fontSize: 12, fontWeight: 500 }}>
        <Clock size={12} /> Pending
      </span>
    )
  } else {
    return <span style={{ color: '#64748b', fontSize: 12 }}>{status}</span>
  }
})()}
```

- [ ] **Step 2: Add "Retry User Creation" button for FAILED status in action menu**

In the action menu dropdown (after line 495), add a retry button for FAILED agents:

```tsx
{(getAgentUserStatus(agent.agentId)?.status === 'NOT_CREATED' || 
  getAgentUserStatus(agent.agentId)?.status === 'FAILED') && (
  <button 
    style={{
      display: 'block',
      width: '100%',
      padding: '10px 16px',
      textAlign: 'left',
      background: 'none',
      border: 'none',
      cursor: 'pointer',
      fontSize: 14,
      color: '#10b981'
    }}
    onClick={() => handleCreateAgentUser(agent.agentId)}
  >
    {getAgentUserStatus(agent.agentId)?.status === 'FAILED' 
      ? 'Retry User Creation' 
      : 'Create User Account'}
  </button>
)}
```

- [ ] **Step 3: Run tests**

```bash
cd backoffice && npm test -- --run 2>&1 | tail -10
```

---

### Task 3: Show Error Message in View Agent Modal for FAILED Status [DONE]

**BDD Scenarios:** S6.2 (failed status with error message)

**BRD Requirements:** FR-8.3

**User-Facing:** YES

**Files:**
- Modify: `backoffice/src/pages/Agents.tsx:730-788` (View Agent modal user account section)

- [ ] **Step 1: Add error display in View Agent modal for FAILED status**

In the View Agent modal's User Account section (lines 730-788), add error display for FAILED:

```tsx
// Add this branch in the View Agent modal's user status rendering
} else if (userStatus.status === 'FAILED') {
  return (
    <div style={{ marginTop: 8 }}>
      <span className="badge badge-error" style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
        <XCircle size={12} /> Failed
      </span>
      {userStatus.error && (
        <p style={{ 
          margin: '8px 0 0 0', 
          fontSize: 13, 
          color: '#dc2626',
          background: '#fef2f2',
          padding: '8px 12px',
          borderRadius: 6,
          border: '1px solid #fecaca'
        }}>
          Error: {userStatus.error}
        </p>
      )}
      <button 
        className="btn btn-primary" 
        onClick={() => { handleCreateAgentUser(viewAgent.agentId); setViewAgent(null); }}
        style={{ marginTop: 12, padding: '8px 16px', fontSize: 13 }}
      >
        Retry User Creation
      </button>
    </div>
  )
}
```

- [ ] **Step 2: Run tests**

```bash
cd backoffice && npm test -- --run 2>&1 | tail -10
```

---

### Task 4: Add agentId Column and Status Badge Fixes in User Management [DONE]

**BDD Scenarios:** FR-1.3 (agentId for EXTERNAL users)

**BRD Requirements:** FR-1.3

**User-Facing:** YES

**Files:**
- Modify: `backoffice/src/pages/UserManagement.tsx` (add agentId column, fix status badges)

- [ ] **Step 1: Add agentId column to the users table**

Find the table header row and add a new column header after the "User Type" column:
```tsx
<th>Agent ID</th>
```

In the table body, add a corresponding cell after the userType cell:
```tsx
<td style={{ fontSize: 13, fontFamily: 'monospace', color: '#64748b' }}>
  {user.userType === 'EXTERNAL' && user.agentId 
    ? user.agentId.slice(0, 8) + '...' 
    : '—'}
</td>
```

- [ ] **Step 2: Fix status badge styling for INACTIVE and DELETED**

In the status badge rendering (around lines 286-291), replace the catch-all with explicit handling:

```tsx
const getStatusBadge = (status: string) => {
  switch (status) {
    case 'ACTIVE':
      return <span className="badge badge-success">{status}</span>
    case 'LOCKED':
      return <span className="badge badge-warning">{status}</span>
    case 'INACTIVE':
      return <span className="badge badge-info">{status}</span>
    case 'DELETED':
      return <span className="badge badge-error">{status}</span>
    default:
      return <span className="badge">{status}</span>
  }
}
```

- [ ] **Step 3: Fix stats section to track INACTIVE/DELETED instead of DISABLED**

In the stats section (around lines 242-259), update to:
```tsx
const inactive = users.filter(u => u.status === 'INACTIVE').length
const deleted = users.filter(u => u.status === 'DELETED').length
// Replace DISABLED stat with INACTIVE and DELETED
```

- [ ] **Step 4: Run tests**

```bash
cd backoffice && npm test -- --run 2>&1 | tail -10
```

---

### Task 5: Show agentId, mustChangePassword, tempPasswordExpiresAt in View User modal [DONE]

### Task 6: Add GET /auth/me Backend Endpoint for Current User Profile [DONE]

### Task 7: Create Profile Page with View Profile and Change Password [DONE]

**BDD Scenarios:** FR-5.3 (password reset), FR-4.4 (password change clears flags)

**BRD Requirements:** FR-4.4, FR-5.3, FR-5.6

**User-Facing:** YES

**Files:**
- Create: `backoffice/src/pages/Profile.tsx`
- Modify: `backoffice/src/api/client.ts` (add getMyProfile, changeMyPassword)
- Modify: `backoffice/src/main.tsx` (add /profile route)
- Modify: `backoffice/src/components/Layout.tsx` (add Profile link to header avatar dropdown)
- Create: `backoffice/src/test/Profile.test.tsx`

- [ ] **Step 1: Add API functions to client.ts**

```typescript
// In api/client.ts
getMyProfile: () => client.get('/auth/me').then((r) => r.data),
changeMyPassword: (data: { currentPassword: string; newPassword: string }) => 
  client.post('/auth/password/change', data).then((r) => r.data),
```

- [ ] **Step 2: Create Profile.tsx page**

The page should have two sections:
1. **Profile Information** (read-only display):
   - Full Name
   - Email
   - Username
   - User Type (INTERNAL/EXTERNAL)
   - Agent ID (if EXTERNAL)
   - Status
   - Last Login

2. **Change Password** form:
   - Current Password (required)
   - New Password (required, min 8 chars)
   - Confirm New Password (required, must match)
   - Submit button

- [ ] **Step 3: Add /profile route to main.tsx**

```tsx
<Route path="/profile" element={<ProtectedRoute><Profile /></ProtectedRoute>} />
```

- [ ] **Step 4: Add Profile link to Layout.tsx header avatar dropdown**

In the user avatar dropdown menu, add:
```tsx
<button onClick={() => navigate('/profile')}>Profile</button>
```

- [ ] **Step 5: Write tests for Profile page**

```tsx
// backoffice/src/test/Profile.test.tsx
describe('Profile Page', () => {
  it('displays user profile information', () => { ... })
  it('shows change password form', () => { ... })
  it('validates password match', () => { ... })
  it('calls change password API on submit', () => { ... })
  it('shows success toast on password change', () => { ... })
})
```

- [ ] **Step 6: Run all tests**

```bash
cd backoffice && npm test -- --run 2>&1 | tail -15
```

---

## Verification

After all tasks, run the full test suite:

```bash
cd backoffice && npm test -- --run
```

Expected: **139 passing tests** ✅

Backend auth service:
```bash
./gradlew :services:auth-iam-service:test
```

Expected: **BUILD SUCCESSFUL** ✅
