# Phase 5: Backoffice UI Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development.

**Goal:** Build React + TypeScript backoffice web application for bank operations.

**Tech Stack:** React 18, TypeScript, Vite, React Router, TanStack Query

---

## File Structure

```
backoffice/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── index.html
├── src/
│   ├── main.tsx
│   ├── App.tsx
│   ├── api/
│   │   ├── client.ts              # Axios instance with auth
│   │   ├── types.ts              # Generated from OpenAPI
│   │   └── mock.ts                # Mock client using OpenAPI spec
│   ├── pages/
│   │   ├── Dashboard.tsx
│   │   ├── Agents.tsx
│   │   ├── AgentDetail.tsx
│   │   ├── Transactions.tsx
│   │   ├── Settlement.tsx
│   │   └── KycReview.tsx
│   ├── components/
│   │   ├── Layout.tsx
│   │   ├── AgentForm.tsx
│   │   ├── TransactionTable.tsx
│   │   └── Filters.tsx
│   └── hooks/
│       └── useAgents.ts
├── src/test/
└── openapi/                       # Generated from docs/api/openapi.yaml
```

---

## Tasks

### Task 1: Project Setup [DONE]

### Task 2: Layout and Navigation [DONE]

### Task 3: Dashboard Page [DONE]

### Task 4: Agent Management [DONE]

### Task 5: Transaction Monitor [DONE]

### Task 6: Settlement Report [DONE]

### Task 7: KYC Review Queue [DONE]

---

## Phase 5: Backoffice UI - COMPLETE ✅

- [ ] Create vite project with React + TypeScript
- [ ] Add dependencies: react-router-dom, @tanstack/react-query, axios
- [ ] Configure ESLint + Prettier
- [ ] Set up OpenAPI code generation (openapi-generator)
- [ ] Generate API client from docs/api/openapi.yaml
- [ ] Commit

### Task 2: Layout and Navigation

**BDD Scenarios:** BDD-BO01, BDD-BO02, BDD-BO03

- [ ] **Step 1: Create Layout component with sidebar**

```tsx
export const Layout = ({ children }) => {
  return (
    <div className="flex">
      <Sidebar />
      <main>{children}</main>
    </div>
  );
};
```

- [ ] **Step 2: Set up React Router**

```tsx
<Routes>
  <Route path="/" element={<Dashboard />} />
  <Route path="/agents" element={<Agents />} />
  <Route path="/agents/:id" element={<AgentDetail />} />
  <Route path="/transactions" element={<Transactions />} />
  <Route path="/settlement" element={<Settlement />} />
  <Route path="/kyc-review" element={<KycReview />} />
</Routes>
```

- [ ] **Step 3: Test navigation works**

- [ ] **Step 4: Commit**

### Task 3: Dashboard Page

**BDD Scenarios:** BDD-BO02

- [ ] **Step 1: Create Dashboard with KPIs**

```tsx
export const Dashboard = () => {
  const { data } = useQuery({ queryKey: ['dashboard'], queryFn: api.getDashboard });
  return (
    <div>
      <KPICard title="Today's Transactions" value={data.totalTransactions} />
      <KPICard title="Total Volume" value={data.totalVolume} />
      <KPICard title="Active Agents" value={data.activeAgents} />
    </div>
  );
};
```

- [ ] **Step 2: Write tests**

- [ ] **Step 3: Commit**

### Task 4: Agent Management

**BDD Scenarios:** BDD-BO01, BDD-BO01-EC-01, BDD-BO01-EC-02

- [ ] **Step 1: Create agent list page**

- [ ] **Step 2: Create agent form (create/edit)**

- [ ] **Step 3: Handle validation (duplicate MyKad, pending transactions)**

- [ ] **Step 4: Write tests**

- [ ] **Step 5: Commit**

### Task 5: Transaction Monitor

**BDD Scenarios:** BDD-BO02

- [ ] **Step 1: Create transaction list with filters**

```tsx
export const Transactions = () => {
  const [filters, setFilters] = useState({ status: '', agentId: '', dateFrom: '', dateTo: '' });
  const { data } = useQuery({ queryKey: ['transactions', filters], queryFn: () => api.getTransactions(filters) });
  return <TransactionTable data={data} />;
};
```

- [ ] **Step 2: Write tests**

- [ ] **Step 3: Commit**

### Task 6: Settlement Report

**BDD Scenarios:** BDD-BO03

- [ ] **Step 1: Create settlement page with date picker**

- [ ] **Step 2: Add CSV export**

- [ ] **Step 3: Write tests**

- [ ] **Step 4: Commit**

### Task 7: KYC Review Queue

**BDD Scenarios:** BDD-O02-EC-01, BDD-O02-EC-02

- [ ] **Step 1: Create review queue page**

- [ ] **Step 2: Add approve/reject actions**

- [ ] **Step 3: Write tests**

- [ ] **Step 4: Commit**

---

## Summary

| Task | BDD Coverage |
|------|--------------|
| 1 | Foundation |
| 2 | Layout (BDD-BO01, BO02, BO03) |
| 3 | Dashboard (BDD-BO02) |
| 4 | Agent CRUD (BDD-BO01, BO01-EC-01, BO01-EC-02) |
| 5 | Transaction monitor (BDD-BO02) |
| 6 | Settlement report (BDD-BO03) |
| 7 | KYC review (BDD-O02-EC-01, O02-EC-02) |