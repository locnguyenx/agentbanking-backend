import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { Transactions } from '../pages/Transactions'
import React from 'react'

vi.mock('../api/client', () => {
  const MOCK_TRANSACTIONS_RESPONSE = {
    content: Array.from({ length: 25 }, (_, i) => ({
      transactionId: `f${i.toString(16).padStart(2, '0')}eebc99-9c0b-4ef8-bb6d-6bb9bd380a${(61 + i).toString(16).padStart(2, '0')}`,
      agentId: 'd3eebc99-9c0b-4ef8-bb6d-6bb9bd380a44',
      transactionType: i % 2 === 0 ? 'CASH_DEPOSIT' : 'CASH_WITHDRAWAL',
      amount: (i + 1) * 1000,
      status: i === 6 ? 'PENDING' : 'COMPLETED',
      customerCardMasked: `411111******${String(1000 + i).padStart(4, '0')}`,
      createdAt: `2026-03-26T${String(8 + Math.floor(i / 4)).padStart(2, '0')}:${String((i % 4) * 15).padStart(2, '0')}:00`,
    })),
    totalElements: 25,
    totalPages: 3,
    page: 0,
    size: 10,
  }

  return {
    default: {
      getDashboard: vi.fn().mockResolvedValue({}),
      getAgents: vi.fn().mockResolvedValue([]),
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
      getTransactions: vi.fn().mockResolvedValue(MOCK_TRANSACTIONS_RESPONSE),
      getSettlement: vi.fn().mockResolvedValue({}),
      exportSettlement: vi.fn().mockResolvedValue({}),
      getKycReviewQueue: vi.fn().mockResolvedValue({}),
      approveKyc: vi.fn().mockResolvedValue({}),
      rejectKyc: vi.fn().mockResolvedValue({}),
      login: vi.fn().mockResolvedValue({}),
      logout: vi.fn(),
    },
  }
})

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

describe('Transactions', () => {
  it('should render transactions page', async () => {
    renderWithProviders(<Transactions />)
    
    await waitFor(() => {
      expect(screen.getByTestId('page-title')).toBeInTheDocument()
    })
  })

  it('should display page title', async () => {
    renderWithProviders(<Transactions />)
    
    await waitFor(() => {
      expect(screen.getByText('Transaction History')).toBeInTheDocument()
    })
  })

  it('should render search input', async () => {
    renderWithProviders(<Transactions />)
    
    await waitFor(() => {
      expect(screen.getByTestId('search-input')).toBeInTheDocument()
    })
  })

  it('should render status filter', async () => {
    renderWithProviders(<Transactions />)
    
    await waitFor(() => {
      expect(screen.getByTestId('status-filter')).toBeInTheDocument()
    })
  })

  it('should render pagination buttons for 3 pages (25 transactions)', async () => {
    renderWithProviders(<Transactions />)
    
    await waitFor(() => {
      expect(screen.getByTestId('prev-page-button')).toBeInTheDocument()
      expect(screen.getByTestId('page-1-button')).toBeInTheDocument()
      expect(screen.getByTestId('page-2-button')).toBeInTheDocument()
      expect(screen.getByTestId('page-3-button')).toBeInTheDocument()
      expect(screen.getByTestId('next-page-button')).toBeInTheDocument()
    })
  })

  it('should render export button', async () => {
    renderWithProviders(<Transactions />)
    
    await waitFor(() => {
      expect(screen.getByTestId('export-button')).toBeInTheDocument()
    })
  })

  it('should change page when page number is clicked', async () => {
    renderWithProviders(<Transactions />)
    
    await waitFor(() => {
      expect(screen.getByTestId('page-1-button')).toBeInTheDocument()
      expect(screen.getByTestId('page-2-button')).toBeInTheDocument()
    })
    
    fireEvent.click(screen.getByTestId('page-2-button'))
    
    await waitFor(() => {
      expect(screen.getByText(/Showing \d+ to \d+ of 25 transactions/)).toBeInTheDocument()
    })
  })

  it('should enable prev button on second page via page button', async () => {
    renderWithProviders(<Transactions />)
    
    await waitFor(() => {
      expect(screen.getByTestId('page-2-button')).toBeInTheDocument()
    })
    
    fireEvent.click(screen.getByTestId('page-2-button'))
    
    await waitFor(() => {
      const prevButton = screen.getByTestId('prev-page-button')
      expect(prevButton).not.toBeDisabled()
    })
  })

  it('should disable prev button on first page', async () => {
    renderWithProviders(<Transactions />)
    
    await waitFor(() => {
      const prevButton = screen.getByTestId('prev-page-button')
      expect(prevButton).toBeDisabled()
    })
  })

  it('should have working search input', async () => {
    renderWithProviders(<Transactions />)
    
    await waitFor(() => {
      const searchInput = screen.getByTestId('search-input')
      fireEvent.change(searchInput, { target: { value: 'TXN-001' } })
      expect(searchInput).toHaveValue('TXN-001')
    })
  })

  it('should have working status filter', async () => {
    renderWithProviders(<Transactions />)
    
    await waitFor(() => {
      const statusFilter = screen.getByTestId('status-filter')
      fireEvent.change(statusFilter, { target: { value: 'COMPLETED' } })
      expect(statusFilter).toHaveValue('COMPLETED')
    })
  })
})
