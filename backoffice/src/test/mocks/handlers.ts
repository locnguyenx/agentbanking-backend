/**
 * MSW Handlers
 * 
 * HTTP request handlers that mock the backend API.
 * These handlers should mirror the actual API responses from the backend.
 * 
 * Backend DTOs are defined in the Java services.
 * OpenAPI spec is at docs/api/openapi.yaml
 */
import { http, HttpResponse, delay } from 'msw'

// Mock data for users
const mockUsers = [
  {
    userId: 'a0000000-0000-0000-0000-000000000001',
    username: 'admin',
    email: 'admin@agentbanking.com',
    fullName: 'System Administrator',
    status: 'ACTIVE',
    userType: 'INTERNAL',
    agentId: null,
    permissions: [],
    createdAt: '2026-04-01T12:56:52.825224',
    lastLoginAt: '2026-04-03T05:38:14.587887',
  },
  {
    userId: '29440639-eab1-4b8a-bff9-d8f839276c87',
    username: 'AGT-E2E-001',
    email: 'AGT-E2E-001@agent.local',
    fullName: 'E2E Test Business',
    status: 'ACTIVE',
    userType: 'EXTERNAL',
    agentId: '78cbde90-232a-48a1-878e-0bed6ff52301',
    permissions: [],
    createdAt: '2026-04-01T12:59:10.529151',
    lastLoginAt: null,
  },
  {
    userId: 'd58d0809-d708-46c6-9c65-b6eb041473c8',
    username: 'AGT-008',
    email: 'AGT-008@agent.local',
    fullName: 'Loc',
    status: 'ACTIVE',
    userType: 'EXTERNAL',
    agentId: 'a0000000-0000-0000-0000-000000000008',
    permissions: [],
    createdAt: '2026-04-02T09:21:48.303084',
    lastLoginAt: null,
  },
  {
    userId: '52d1a15f-2a17-4b5c-a3e1-e8a81e0bae92',
    username: 'agent001',
    email: 'agent001@bank.com',
    fullName: 'Test Agent',
    status: 'ACTIVE',
    userType: 'EXTERNAL',
    agentId: 'a0000000-0000-0000-0000-000000000002',
    permissions: [],
    createdAt: '2026-04-01T12:59:08.666126',
    lastLoginAt: '2026-04-01T13:06:54.946616',
  },
  {
    userId: '05adcb2a-cb18-4010-ada5-82e9c31896e9',
    username: 'operator001',
    email: 'operator001@bank.com',
    fullName: 'Test Operator',
    status: 'ACTIVE',
    userType: 'INTERNAL',
    agentId: null,
    permissions: [],
    createdAt: '2026-04-01T12:59:08.777950',
    lastLoginAt: '2026-04-01T13:06:55.046145',
  },
  {
    userId: 'ddf1ba9e-902f-4b8c-844c-c71ccf994af8',
    username: 'maker001',
    email: 'maker001@bank.com',
    fullName: 'Test Maker',
    status: 'INACTIVE',
    userType: 'INTERNAL',
    agentId: null,
    permissions: [],
    createdAt: '2026-04-01T12:59:08.898110',
    lastLoginAt: '2026-04-01T13:06:55.180102',
  },
  {
    userId: '4b5393de-3bf9-4eb2-a331-f687c6e58bc0',
    username: 'checker001',
    email: 'checker001@bank.com',
    fullName: 'Test Checker',
    status: 'LOCKED',
    userType: 'INTERNAL',
    agentId: null,
    permissions: [],
    createdAt: '2026-04-01T12:59:08.999966',
    lastLoginAt: '2026-04-01T13:06:55.521304',
  },
  {
    userId: '7452ec9f-4326-4084-800d-2326ac8b8862',
    username: 'compliance001',
    email: 'compliance001@bank.com',
    fullName: 'Test Compliance Officer',
    status: 'ACTIVE',
    userType: 'INTERNAL',
    agentId: null,
    permissions: [],
    createdAt: '2026-04-01T12:59:09.163583',
    lastLoginAt: '2026-04-01T13:06:55.654733',
  },
  {
    userId: '0426d40f-65b1-4adc-bfbe-e7a7bee9babf',
    username: 'teller001',
    email: 'teller001@bank.com',
    fullName: 'Test Teller',
    status: 'DELETED',
    userType: 'INTERNAL',
    agentId: null,
    permissions: [],
    createdAt: '2026-04-01T12:59:09.390709',
    lastLoginAt: '2026-04-01T13:06:55.767698',
  },
  {
    userId: '03259706-a760-4ea8-b40b-61d5eaf44d4e',
    username: 'supervisor001',
    email: 'supervisor001@bank.com',
    fullName: 'Test Supervisor',
    status: 'ACTIVE',
    userType: 'INTERNAL',
    agentId: null,
    permissions: [],
    createdAt: '2026-04-01T12:59:09.585066',
    lastLoginAt: '2026-04-01T13:06:55.886822',
  },
]

// Mock data for agents
const mockAgents = [
  {
    agentId: 'a0000000-0000-0000-0000-000000000001',
    agentCode: 'AGT-001',
    businessName: 'Ahmad Razak Store',
    tier: 'PREMIUM',
    status: 'ACTIVE',
    merchantGpsLat: 3.139003,
    merchantGpsLng: 101.686855,
    phoneNumber: '012-3456789',
    createdAt: '2026-04-01T12:56:49.938566',
    updatedAt: '2026-04-01T12:56:49.938566',
  },
  {
    agentId: 'a0000000-0000-0000-0000-000000000002',
    agentCode: 'AGT-002',
    businessName: 'Siti Aminah Enterprise',
    tier: 'STANDARD',
    status: 'ACTIVE',
    merchantGpsLat: 3.0734,
    merchantGpsLng: 101.6065,
    phoneNumber: '013-8765432',
    createdAt: '2026-04-01T12:56:49.938566',
    updatedAt: '2026-04-01T12:56:49.938566',
  },
  {
    agentId: 'a0000000-0000-0000-0000-000000000003',
    agentCode: 'AGT-003',
    businessName: 'Faisal Trading',
    tier: 'BASIC',
    status: 'SUSPENDED',
    merchantGpsLat: 3.0685,
    merchantGpsLng: 101.5182,
    phoneNumber: '014-2468135',
    createdAt: '2026-04-01T12:56:49.938566',
    updatedAt: '2026-04-01T12:56:49.938566',
  },
  {
    agentId: 'a0000000-0000-0000-0000-000000000004',
    agentCode: 'AGT-004',
    businessName: 'Lee Ming Retail',
    tier: 'PREMIUM',
    status: 'ACTIVE',
    merchantGpsLat: 5.4141,
    merchantGpsLng: 100.3287,
    phoneNumber: '016-9753124',
    createdAt: '2026-04-01T12:56:49.938566',
    updatedAt: '2026-04-01T12:56:49.938566',
  },
  {
    agentId: 'a0000000-0000-0000-0000-000000000005',
    agentCode: 'AGT-005',
    businessName: 'Nurul Huda Mart',
    tier: 'STANDARD',
    status: 'ACTIVE',
    merchantGpsLat: 1.4927,
    merchantGpsLng: 103.7431,
    phoneNumber: '011-6543210',
    createdAt: '2026-04-01T12:56:49.938566',
    updatedAt: '2026-04-01T12:56:49.938566',
  },
  {
    agentId: 'a0000000-0000-0000-0000-000000000006',
    agentCode: 'AGT-006',
    businessName: 'Tan Kah Seng',
    tier: 'BASIC',
    status: 'ACTIVE',
    merchantGpsLat: 4.5975,
    merchantGpsLng: 101.0921,
    phoneNumber: '017-1234567',
    createdAt: '2026-04-01T12:56:49.938566',
    updatedAt: '2026-04-01T12:56:49.938566',
  },
  {
    agentId: 'a0000000-0000-0000-0000-000000000007',
    agentCode: 'AGT-007',
    businessName: 'Ali Ahmad',
    tier: 'BASIC',
    status: 'INACTIVE',
    merchantGpsLat: 3.1,
    merchantGpsLng: 101.6,
    phoneNumber: '019-1111111',
    createdAt: '2026-04-01T12:56:49.938566',
    updatedAt: '2026-04-01T12:56:49.938566',
  },
]

// Mock data for transactions
const mockTransactions = Array.from({ length: 25 }, (_, i) => ({
  transactionId: `f${i.toString(16).padStart(2, '0')}eebc99-9c0b-4ef8-bb6d-6bb9bd380a${(61 + i).toString(16).padStart(2, '0')}`,
  agentId: 'd3eebc99-9c0b-4ef8-bb6d-6bb9bd380a44',
  transactionType: i % 2 === 0 ? 'CASH_DEPOSIT' : 'CASH_WITHDRAWAL',
  amount: (i + 1) * 1000,
  status: i === 6 ? 'PENDING' : 'COMPLETED',
  customerCardMasked: `411111******${String(1000 + i).padStart(4, '0')}`,
  createdAt: `2026-03-26T${String(8 + Math.floor(i / 4)).padStart(2, '0')}:${String((i % 4) * 15).padStart(2, '0')}:00`,
}))

export const handlers = [
  // ============================================================
  // Auth Endpoints
  // ============================================================
  http.post('/api/v1/auth/token', async () => {
    await delay(100)
    return HttpResponse.json({
      access_token: 'mock-access-token',
      refresh_token: 'mock-refresh-token',
      expires_in: 900,
      token_type: 'Bearer',
    })
  }),

  // ============================================================
  // Backoffice Admin Users
  // ============================================================
  http.get('/api/v1/backoffice/admin/users', async ({ request }) => {
    await delay(100)
    const url = new URL(request.url)
    const page = parseInt(url.searchParams.get('page') || '0')
    const size = parseInt(url.searchParams.get('size') || '10')
    
    const start = page * size
    const end = start + size
    const paginatedUsers = mockUsers.slice(start, end)
    
    return HttpResponse.json(paginatedUsers)
  }),

  http.post('/api/v1/backoffice/admin/users', async () => {
    await delay(100)
    return HttpResponse.json({
      userId: 'new-user-id',
      username: 'newuser',
      email: 'newuser@agentbanking.com',
      fullName: 'New User',
      status: 'ACTIVE',
      createdAt: new Date().toISOString(),
    })
  }),

  http.get('/api/v1/backoffice/admin/users/:id', async ({ params }) => {
    await delay(100)
    const user = mockUsers.find(u => u.userId === params.id)
    if (!user) {
      return HttpResponse.json({ status: 'FAILED', error: { code: 'ERR_NOT_FOUND', message: 'User not found' } }, { status: 404 })
    }
    return HttpResponse.json(user)
  }),

  http.put('/api/v1/backoffice/admin/users/:id', async ({ params }) => {
    await delay(100)
    return HttpResponse.json({ ...mockUsers[0], userId: params.id, updatedAt: new Date().toISOString() })
  }),

  http.delete('/api/v1/backoffice/admin/users/:id', async () => {
    await delay(100)
    return HttpResponse.json({ status: 'SUCCESS' })
  }),

  // ============================================================
  // Backoffice Agents
  // ============================================================
  http.get('/api/v1/backoffice/agents', async ({ request }) => {
    await delay(100)
    const url = new URL(request.url)
    const page = parseInt(url.searchParams.get('page') || '0')
    const size = parseInt(url.searchParams.get('size') || '10')
    
    const start = page * size
    const end = start + size
    const paginatedAgents = mockAgents.slice(start, end)
    
    return HttpResponse.json(paginatedAgents)
  }),

  http.post('/api/v1/backoffice/agents', async () => {
    await delay(100)
    return HttpResponse.json({
      agentId: 'new-agent-id',
      agentCode: 'AGT-NEW',
      businessName: 'New Agent',
      status: 'ACTIVE',
      createdAt: new Date().toISOString(),
    })
  }),

  http.get('/api/v1/backoffice/agents/:id', async ({ params }) => {
    await delay(100)
    const agent = mockAgents.find(a => a.agentId === params.id)
    if (!agent) {
      return HttpResponse.json({ status: 'FAILED', error: { code: 'ERR_NOT_FOUND', message: 'Agent not found' } }, { status: 404 })
    }
    return HttpResponse.json(agent)
  }),

  http.get('/api/v1/backoffice/agents/:agentId/user-status', async ({ params }) => {
    await delay(100)
    return HttpResponse.json({
      agentId: params.agentId,
      status: 'NOT_CREATED',
    })
  }),

  http.post('/api/v1/backoffice/agents/:agentId/create-user', async () => {
    await delay(100)
    return HttpResponse.json({
      userId: 'new-user-id',
      status: 'ACTIVE',
    })
  }),

  // ============================================================
  // Backoffice Transactions
  // ============================================================
  http.get('/api/v1/backoffice/transactions', async ({ request }) => {
    await delay(100)
    const url = new URL(request.url)
    const page = parseInt(url.searchParams.get('page') || '0')
    const size = parseInt(url.searchParams.get('size') || '10')
    
    const start = page * size
    const end = start + size
    const paginatedTransactions = mockTransactions.slice(start, end)
    
    return HttpResponse.json({
      content: paginatedTransactions,
      totalElements: mockTransactions.length,
      totalPages: Math.ceil(mockTransactions.length / size),
      page,
      size,
    })
  }),

  // ============================================================
  // Backoffice Dashboard
  // ============================================================
  http.get('/api/v1/backoffice/dashboard', async () => {
    await delay(100)
    return HttpResponse.json({
      todayVolume: 2847000,
      todayTransactions: 4521,
      activeAgents: 156,
      pendingKyc: 12,
      totalAgents: 180,
      recentTxns: mockTransactions.slice(0, 5),
    })
  }),

  // ============================================================
  // Backoffice Settlement
  // ============================================================
  http.get('/api/v1/backoffice/settlement', async () => {
    await delay(100)
    return HttpResponse.json({
      totalCredits: 1250000.00,
      totalDebits: 850000.00,
      netAmount: 400000.00,
      transactions: mockTransactions.slice(0, 5).map(t => ({
        ...t,
        agentId: mockAgents[0].agentId,
      })),
    })
  }),

  // ============================================================
  // Backoffice KYC Review Queue
  // ============================================================
  http.get('/api/v1/backoffice/kyc/review-queue', async ({ request }) => {
    await delay(100)
    const url = new URL(request.url)
    const page = parseInt(url.searchParams.get('page') || '0')
    const size = parseInt(url.searchParams.get('size') || '10')
    
    const mockKycItems = [
      {
        verificationId: '10000000-0000-0000-0000-000000000002',
        mykadMasked: '9204********',
        fullName: 'MUTHU KUMAR',
        biometricMatch: 'MATCH',
        amlStatus: 'CLEAR',
        priority: 'HIGH',
      },
      {
        verificationId: '10000000-0000-0000-0000-000000000003',
        mykadMasked: '8807********',
        fullName: 'PRIYA DEVI',
        biometricMatch: 'MATCH',
        amlStatus: 'FLAGGED',
        priority: 'MEDIUM',
      },
    ]
    
    const start = page * size
    const end = start + size
    
    return HttpResponse.json({
      content: mockKycItems.slice(start, end),
      totalElements: mockKycItems.length,
      totalPages: 1,
      page,
      size,
    })
  }),

  http.post('/api/v1/backoffice/kyc/:id/approve', async () => {
    await delay(100)
    return HttpResponse.json({ status: 'SUCCESS' })
  }),

  http.post('/api/v1/backoffice/kyc/:id/reject', async () => {
    await delay(100)
    return HttpResponse.json({ status: 'SUCCESS' })
  }),
]