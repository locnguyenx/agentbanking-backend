# Backoffice Implementation Plan (US-BO01-BO06, US-G01-G02) [DONE]

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## Status: COMPLETED (2026-03-27)

All tasks completed. Backoffice React application is fully operational with all pages and API integration.

**Goal:** Implement Backoffice UI (React + TypeScript) and Gateway API endpoints for agent management, transaction monitoring, settlement review, KYC review queue, configuration, and audit logs per user stories US-BO01-BO06 and US-G01-G02.

**Architecture:** React + TypeScript + Vite frontend. Spring Cloud Gateway for routing. Backoffice calls Gateway endpoints which route to internal services.

**Tech Stack:** React 18, TypeScript 5, Vite 5, Ant Design, React Query, Axios, Spring Cloud Gateway, JWT authentication.

---

## Task 1: React Application Scaffolding [DONE]

**BDD Scenarios:** N/A (infrastructure)  
**BRD Requirements:** US-BO01-BO06, US-G01-G02  
**User-Facing:** YES  

**Files:**
- Create: `backoffice/package.json`
- Create: `backoffice/vite.config.ts`
- Create: `backoffice/tsconfig.json`
- Create: `backoffice/src/main.tsx`
- Create: `backoffice/src/App.tsx`
- Create: `backoffice/src/routes.tsx`

### Step 1: Initialize Vite + React + TypeScript project

### Step 2: Install dependencies (antd, react-query, axios, react-router)

### Step 3: Set up routing structure

### Step 4: Commit

```bash
git add backoffice/
git commit -m "feat(backoffice): scaffold React + TypeScript + Vite application"
```

---

## Task 2: Authentication & Layout [DONE]

**BDD Scenarios:** N/A (UI infrastructure)  
**BRD Requirements:** US-G02 (JWT authentication)  
**User-Facing:** YES  

**Files:**
- Create: `backoffice/src/contexts/AuthContext.tsx`
- Create: `backoffice/src/services/authService.ts`
- Create: `backoffice/src/components/Layout.tsx`
- Create: `backoffice/src/components/Sidebar.tsx`
- Create: `backoffice/src/components/Header.tsx`
- Create: `backoffice/src/pages/Login.tsx`

### Step 1: Create AuthContext with JWT token management

### Step 2: Create login page

### Step 3: Create layout with sidebar navigation

### Step 4: Commit

```bash
git add backoffice/src/contexts/ backoffice/src/services/authService.ts backoffice/src/components/ backoffice/src/pages/Login.tsx
git commit -m "feat(backoffice): add JWT authentication and layout components"
```

---

## Task 3: Agent Management Module (US-BO01) [DONE]

**BDD Scenarios:** BDD-BO01, BDD-BO01-EC-01, BDD-BO01-EC-02, BDD-BO01-EC-03  
**BRD Requirements:** US-BO01, FR-13.1  
**User-Facing:** YES  

**Files:**
- Create: `backoffice/src/pages/agents/AgentList.tsx`
- Create: `backoffice/src/pages/agents/AgentCreate.tsx`
- Create: `backoffice/src/pages/agents/AgentEdit.tsx`
- Create: `backoffice/src/services/agentService.ts`
- Create: `backoffice/src/types/agent.ts`

### Step 1: Create TypeScript types for Agent

```typescript
export interface Agent {
  agentId: string;
  agentCode: string;
  businessName: string;
  tier: 'MICRO' | 'STANDARD' | 'PREMIER';
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  merchantGpsLat: number;
  merchantGpsLng: number;
  phoneNumber: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAgentRequest {
  businessName: string;
  tier: 'MICRO' | 'STANDARD' | 'PREMIER';
  merchantGpsLat: number;
  merchantGpsLng: number;
  mykadNumber: string;
  phoneNumber: string;
}
```

### Step 2: Create agent service API calls

### Step 3: Create AgentList page with table and filters

### Step 4: Create AgentCreate form

### Step 5: Create AgentEdit form

### Step 6: Commit

```bash
git add backoffice/src/pages/agents/ backoffice/src/services/agentService.ts backoffice/src/types/
git commit -m "feat(backoffice): add agent management module (list, create, edit)"
```

---

## Task 4: Transaction Monitoring Module (US-BO02) [DONE]

**BDD Scenarios:** BDD-BO02, BDD-BO02-EC-01  
**BRD Requirements:** US-BO02, FR-13.2  
**User-Facing:** YES  

**Files:**
- Create: `backoffice/src/pages/transactions/TransactionList.tsx`
- Create: `backoffice/src/services/transactionService.ts`
- Create: `backoffice/src/types/transaction.ts`

### Step 1: Create TypeScript types for Transaction

### Step 2: Create transaction service API calls

### Step 3: Create TransactionList page with filters and pagination

### Step 4: Commit

```bash
git add backoffice/src/pages/transactions/ backoffice/src/services/transactionService.ts backoffice/src/types/
git commit -m "feat(backoffice): add transaction monitoring module"
```

---

## Task 5: Settlement Module (US-BO03) [DONE]

**BDD Scenarios:** BDD-BO03, BDD-SM01, BDD-SM02  
**BRD Requirements:** US-BO03, US-SM01, US-SM02, FR-13.3  
**User-Facing:** YES  

**Files:**
- Create: `backoffice/src/pages/settlement/SettlementList.tsx`
- Create: `backoffice/src/pages/settlement/SettlementDetail.tsx`
- Create: `backoffice/src/services/settlementService.ts`
- Create: `backoffice/src/types/settlement.ts`

### Step 1: Create TypeScript types for Settlement

### Step 2: Create settlement service API calls

### Step 3: Create SettlementList page

### Step 4: Create SettlementDetail page

### Step 5: Commit

```bash
git add backoffice/src/pages/settlement/ backoffice/src/services/settlementService.ts
git commit -m "feat(backoffice): add settlement module"
```

---

## Task 6: e-KYC Review Queue Module (US-BO04) [DONE]

**BDD Scenarios:** BDD-BO04, BDD-O04  
**BRD Requirements:** US-BO04, US-O04, FR-13.4  
**User-Facing:** YES  

**Files:**
- Create: `backoffice/src/pages/kyc/KycReviewQueue.tsx`
- Create: `backoffice/src/pages/kyc/KycReviewDetail.tsx`
- Create: `backoffice/src/services/kycService.ts`
- Create: `backoffice/src/types/kyc.ts`

### Step 1: Create TypeScript types for KYC verification

### Step 2: Create KYC service API calls

### Step 3: Create KycReviewQueue page

### Step 4: Create KycReviewDetail page with approve/reject actions

### Step 5: Commit

```bash
git add backoffice/src/pages/kyc/ backoffice/src/services/kycService.ts
git commit -m "feat(backoffice): add e-KYC review queue module"
```

---

## Task 7: Configuration Module (US-BO05) [DONE]

**BDD Scenarios:** BDD-BO05, BDD-R01, BDD-R02  
**BRD Requirements:** US-BO05, US-R01, US-R02  
**User-Facing:** YES  

**Files:**
- Create: `backoffice/src/pages/config/FeeConfigList.tsx`
- Create: `backoffice/src/pages/config/FeeConfigEdit.tsx`
- Create: `backoffice/src/pages/config/VelocityRuleList.tsx`
- Create: `backoffice/src/pages/config/VelocityRuleEdit.tsx`
- Create: `backoffice/src/services/configService.ts`
- Create: `backoffice/src/types/config.ts`

### Step 1: Create TypeScript types for configuration

### Step 2: Create config service API calls

### Step 3: Create fee configuration pages

### Step 4: Create velocity rule pages

### Step 5: Commit

```bash
git add backoffice/src/pages/config/ backoffice/src/services/configService.ts
git commit -m "feat(backoffice): add configuration module for fee and velocity rules"
```

---

## Task 8: Audit Log Module (US-BO06) [DONE]

**BDD Scenarios:** BDD-BO06, BDD-BO06-EC-01  
**BRD Requirements:** US-BO06, FR-13.5  
**User-Facing:** YES  

**Files:**
- Create: `backoffice/src/pages/audit/AuditLogList.tsx`
- Create: `backoffice/src/services/auditService.ts`
- Create: `backoffice/src/types/audit.ts`

### Step 1: Create TypeScript types for AuditLog

### Step 2: Create audit service API calls

### Step 3: Create AuditLogList page with filters

### Step 4: Commit

```bash
git add backoffice/src/pages/audit/ backoffice/src/services/auditService.ts
git commit -m "feat(backoffice): add audit log module"
```

---