import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { Transactions } from '../pages/Transactions'

vi.mock('../api/client', () => ({
  default: {
    getTransactions: vi.fn().mockResolvedValue({
      content: [
        { transactionId: 'txn-001', transactionType: 'CASH_DEPOSIT', amount: 500000, status: 'COMPLETED', agentId: 'agent-001', createdAt: '2026-03-26T08:45:00Z' },
      ],
      totalElements: 1
    }),
  },
}))

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: false },
  },
})

const renderWithQuery = (ui: React.ReactElement) => {
  return render(
    <MemoryRouter>
      <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>
    </MemoryRouter>
  )
}

describe('Transactions', () => {
  it('should render transactions page', async () => {
    renderWithQuery(<Transactions />)
    
    await waitFor(() => {
      expect(screen.getByTestId('page-title')).toBeInTheDocument()
    })
  })

  it('should display page title', async () => {
    renderWithQuery(<Transactions />)
    
    await waitFor(() => {
      expect(screen.getByText('Transaction History')).toBeInTheDocument()
    })
  })

  it('should render search input', async () => {
    renderWithQuery(<Transactions />)
    
    await waitFor(() => {
      expect(screen.getByTestId('search-input')).toBeInTheDocument()
    })
  })

  it('should render status filter', async () => {
    renderWithQuery(<Transactions />)
    
    await waitFor(() => {
      expect(screen.getByTestId('status-filter')).toBeInTheDocument()
    })
  })

  it('should render pagination buttons', async () => {
    renderWithQuery(<Transactions />)
    
    await waitFor(() => {
      expect(screen.getByTestId('prev-page-button')).toBeInTheDocument()
      expect(screen.getByTestId('page-1-button')).toBeInTheDocument()
      expect(screen.getByTestId('page-2-button')).toBeInTheDocument()
      expect(screen.getByTestId('page-3-button')).toBeInTheDocument()
      expect(screen.getByTestId('next-page-button')).toBeInTheDocument()
    })
  })

  it('should render export button', async () => {
    renderWithQuery(<Transactions />)
    
    await waitFor(() => {
      expect(screen.getByTestId('export-button')).toBeInTheDocument()
    })
  })

  it('should change page when page number is clicked', async () => {
    renderWithQuery(<Transactions />)
    
    await waitFor(() => {
      expect(screen.getByTestId('page-1-button')).toBeInTheDocument()
    })
    
    fireEvent.click(screen.getByTestId('page-2-button'))
    
    await waitFor(() => {
      expect(screen.getByTestId('page-2-button')).toHaveClass('btn-primary')
    })
  })

  it('should disable prev button on first page', async () => {
    renderWithQuery(<Transactions />)
    
    await waitFor(() => {
      const prevButton = screen.getByTestId('prev-page-button')
      expect(prevButton).toBeDisabled()
    })
  })

  it('should enable prev button after navigating to next page', async () => {
    renderWithQuery(<Transactions />)
    
    await waitFor(() => {
      fireEvent.click(screen.getByTestId('next-page-button'))
    })
    
    await waitFor(() => {
      const prevButton = screen.getByTestId('prev-page-button')
      expect(prevButton).not.toBeDisabled()
    })
  })

  it('should have working search input', async () => {
    renderWithQuery(<Transactions />)
    
    await waitFor(() => {
      const searchInput = screen.getByTestId('search-input')
      fireEvent.change(searchInput, { target: { value: 'TXN-001' } })
      expect(searchInput).toHaveValue('TXN-001')
    })
  })

  it('should have working status filter', async () => {
    renderWithQuery(<Transactions />)
    
    await waitFor(() => {
      const statusFilter = screen.getByTestId('status-filter')
      fireEvent.change(statusFilter, { target: { value: 'Success' } })
      expect(statusFilter).toHaveValue('Success')
    })
  })
})
