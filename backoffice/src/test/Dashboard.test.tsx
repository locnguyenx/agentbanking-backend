import { describe, it, expect, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { Dashboard } from '../pages/Dashboard'
import React from 'react'

function createTestQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
    },
  })
}

function renderWithProviders(ui: React.ReactElement) {
  const queryClient = createTestQueryClient()
  return render(
    <MemoryRouter>
      <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>
    </MemoryRouter>
  )
}

vi.mock('../api/client', () => ({
  default: {
    getDashboard: vi.fn().mockResolvedValue({
      todayVolume: 2847000,
      todayTransactions: 4521,
      activeAgents: 156,
      pendingKyc: 12,
      totalAgents: 180,
      recentTxns: [
        {
          transactionId: 'txn001',
          agentId: 'agent001',
          transactionType: 'CASH_DEPOSIT',
          amount: 5000,
          status: 'COMPLETED',
        },
        {
          transactionId: 'txn002',
          agentId: 'agent002',
          transactionType: 'CASH_WITHDRAWAL',
          amount: 3000,
          status: 'COMPLETED',
        },
      ],
    }),
    getAgents: vi.fn().mockResolvedValue([]),
    getTransactions: vi.fn().mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
    }),
    createAgent: vi.fn().mockResolvedValue({}),
    getAgent: vi.fn().mockResolvedValue({}),
    updateAgent: vi.fn().mockResolvedValue({}),
    deactivateAgent: vi.fn().mockResolvedValue({}),
    getUsers: vi.fn().mockResolvedValue([]),
    createUser: vi.fn().mockResolvedValue({}),
    updateUser: vi.fn().mockResolvedValue({}),
    deleteUser: vi.fn().mockResolvedValue({}),
    lockUser: vi.fn().mockResolvedValue({}),
    unlockUser: vi.fn().mockResolvedValue({}),
    resetUserPassword: vi.fn().mockResolvedValue({}),
    getAgentUserStatus: vi.fn().mockResolvedValue({}),
    createAgentUser: vi.fn().mockResolvedValue({}),
    getSettlement: vi.fn().mockResolvedValue({}),
    exportSettlement: vi.fn().mockResolvedValue({}),
    getKycReviewQueue: vi.fn().mockResolvedValue({}),
    approveKyc: vi.fn().mockResolvedValue({}),
    rejectKyc: vi.fn().mockResolvedValue({}),
    login: vi.fn().mockResolvedValue({}),
    logout: vi.fn(),
  },
}))

describe('Dashboard', () => {
  it('should render loading state initially', () => {
    renderWithProviders(<Dashboard />)
    expect(document.querySelector('.loading')).toBeInTheDocument()
  })

  it('should render dashboard data after loading', async () => {
    renderWithProviders(<Dashboard />)
    
    await waitFor(() => {
      expect(screen.getByText('Total Agents')).toBeInTheDocument()
    })
    
    expect(screen.getByText("Today's Volume")).toBeInTheDocument()
    expect(screen.getByText('Transactions')).toBeInTheDocument()
    expect(screen.getByText('Pending KYC')).toBeInTheDocument()
  })

  it('should render all metric cards', async () => {
    renderWithProviders(<Dashboard />)
    
    await waitFor(() => {
      expect(screen.getByText('Total Agents')).toBeInTheDocument()
    })
  })
})