/**
 * Schema-based mock data generators.
 * 
 * These generate mock data that ALWAYS matches the backend schema.
 * When the backend changes, update schemas.ts first, then mocks regenerate correctly.
 */

import { VALID_USER_STATUSES, VALID_AGENT_TIERS, VALID_AGENT_STATUSES, VALID_TRANSACTION_STATUSES } from './schemas'

function randomUUID(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

function randomDate(daysBack: number = 90): string {
  const date = new Date()
  date.setDate(date.getDate() - Math.floor(Math.random() * daysBack))
  return date.toISOString()
}

function pickRandom<T>(arr: readonly T[]): T {
  return arr[Math.floor(Math.random() * arr.length)]
}

// ============================================================
// User Mock Generator
// ============================================================
export interface MockUser {
  userId: string
  username: string
  email: string
  fullName: string
  status: typeof VALID_USER_STATUSES[number]
  permissions: string[]
  createdAt: string
  lastLoginAt: string | null
}

export function generateMockUser(overrides: Partial<MockUser> = {}): MockUser {
  const id = randomUUID()
  return {
    userId: id,
    username: `user-${id.slice(0, 8)}`,
    email: `user-${id.slice(0, 8)}@bank.com`,
    fullName: `Test User ${id.slice(0, 4)}`,
    status: pickRandom(VALID_USER_STATUSES),
    permissions: ['user:read'],
    createdAt: randomDate(90),
    lastLoginAt: Math.random() > 0.2 ? randomDate(7) : null,
    ...overrides,
  }
}

export function generateMockUsers(count: number, variations?: {
  statusOverrides?: (typeof VALID_USER_STATUSES[number])[]
}): MockUser[] {
  const users: MockUser[] = []
  if (variations?.statusOverrides) {
    variations.statusOverrides.forEach((status, i) => {
      users.push(generateMockUser({ status }))
    })
  }
  while (users.length < count) {
    users.push(generateMockUser())
  }
  return users
}

// ============================================================
// Agent Mock Generator
// ============================================================
export interface MockAgent {
  agentId: string
  agentCode: string
  businessName: string
  tier: typeof VALID_AGENT_TIERS[number]
  status: typeof VALID_AGENT_STATUSES[number]
  phoneNumber: string
  merchantGpsLat: number
  merchantGpsLng: number
  createdAt: string
  updatedAt: string
}

export function generateMockAgent(overrides: Partial<MockAgent> = {}): MockAgent {
  const id = randomUUID()
  return {
    agentId: id,
    agentCode: `AGT-${String(Math.floor(Math.random() * 900) + 100).padStart(3, '0')}`,
    businessName: `Test Business ${id.slice(0, 4)}`,
    tier: pickRandom(VALID_AGENT_TIERS),
    status: pickRandom(VALID_AGENT_STATUSES),
    phoneNumber: `01${Math.floor(Math.random() * 9) + 1}-${String(Math.floor(Math.random() * 9000000) + 1000000).padStart(7, '0')}`,
    merchantGpsLat: 3 + Math.random() * 3,
    merchantGpsLng: 100 + Math.random() * 4,
    createdAt: randomDate(90),
    updatedAt: randomDate(30),
    ...overrides,
  }
}

export function generateMockAgents(count: number): MockAgent[] {
  return Array.from({ length: count }, () => generateMockAgent())
}

// ============================================================
// Transaction Mock Generator
// ============================================================
export interface MockTransaction {
  transactionId: string
  agentId: string
  transactionType: string
  amount: number
  status: typeof VALID_TRANSACTION_STATUSES[number]
  customerCardMasked: string
  createdAt: string
}

export function generateMockTransaction(overrides: Partial<MockTransaction> = {}): MockTransaction {
  const id = randomUUID()
  const types = ['CASH_DEPOSIT', 'CASH_WITHDRAWAL', 'BILL_PAYMENT', 'TRANSFER']
  return {
    transactionId: id,
    agentId: randomUUID(),
    transactionType: pickRandom(types),
    amount: Math.round((Math.random() * 50000 + 100) * 100) / 100,
    status: pickRandom(VALID_TRANSACTION_STATUSES),
    customerCardMasked: `411111******${String(Math.floor(Math.random() * 9000) + 1000)}`,
    createdAt: randomDate(30),
    ...overrides,
  }
}

export function generateMockTransactions(count: number): MockTransaction[] {
  return Array.from({ length: count }, () => generateMockTransaction())
}

export function generatePaginatedTransactions(count: number) {
  const content = generateMockTransactions(count)
  const size = 10
  return {
    content,
    totalElements: count,
    totalPages: Math.ceil(count / size),
    page: 0,
    size,
  }
}

// ============================================================
// Agent User Status Mock Generator
// ============================================================
export interface MockAgentUserStatus {
  agentId: string
  status: 'ACTIVE' | 'INACTIVE' | 'LOCKED' | 'DELETED' | 'NOT_CREATED'
  userId?: string
  error?: string
}

export function generateMockAgentUserStatus(overrides: Partial<MockAgentUserStatus> = {}): MockAgentUserStatus {
  const id = randomUUID()
  const hasUser = Math.random() > 0.3
  return {
    agentId: id,
    status: hasUser ? pickRandom(['ACTIVE', 'INACTIVE', 'LOCKED'] as const) : 'NOT_CREATED',
    userId: hasUser ? randomUUID() : undefined,
    error: hasUser ? undefined : 'User account not yet created',
    ...overrides,
  }
}
