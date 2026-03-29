import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { Settlement } from '../pages/Settlement'

vi.mock('../api/client', () => ({
  default: {
    getSettlement: vi.fn().mockResolvedValue({
      totalCredits: 1250000.00,
      totalDebits: 850000.00,
      netAmount: 400000.00,
      transactions: [
        { transactionId: 'txn-001', transactionType: 'CASH_DEPOSIT', agentId: 'agent-001', amount: 500000, status: 'COMPLETED' },
        { transactionId: 'txn-002', transactionType: 'CASH_WITHDRAWAL', agentId: 'agent-002', amount: 300000, status: 'COMPLETED' },
      ],
      date: '2026-03-26',
    }),
    exportSettlement: vi.fn().mockResolvedValue(new Blob()),
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

describe('Settlement', () => {
  it('should render settlement page', async () => {
    renderWithQuery(<Settlement />)
    
    await waitFor(() => {
      expect(screen.getByTestId('page-title')).toBeInTheDocument()
    })
  })

  it('should display page title', async () => {
    renderWithQuery(<Settlement />)
    
    await waitFor(() => {
      expect(screen.getByText('Settlement Report')).toBeInTheDocument()
    })
  })

  it('should render date picker', async () => {
    renderWithQuery(<Settlement />)
    
    await waitFor(() => {
      expect(screen.getByTestId('date-picker')).toBeInTheDocument()
    })
  })

  it('should render export button', async () => {
    renderWithQuery(<Settlement />)
    
    await waitFor(() => {
      expect(screen.getByTestId('export-button')).toBeInTheDocument()
    })
  })

  it('should display settlement stats', async () => {
    renderWithQuery(<Settlement />)
    
    await waitFor(() => {
      expect(screen.getAllByText('Total Deposits').length).toBeGreaterThan(0)
      expect(screen.getAllByText('Total Withdrawals').length).toBeGreaterThan(0)
      expect(screen.getAllByText('Net Settlement').length).toBeGreaterThan(0)
    })
  })

  it('should display agent settlement table', async () => {
    renderWithQuery(<Settlement />)
    
    await waitFor(() => {
      expect(screen.getAllByText('Agent').length).toBeGreaterThan(0)
      expect(screen.getAllByText('Deposits').length).toBeGreaterThan(0)
      expect(screen.getAllByText('Withdrawals').length).toBeGreaterThan(0)
      expect(screen.getAllByText('Net Settlement').length).toBeGreaterThan(0)
    })
  })

  it('should have working date picker', async () => {
    renderWithQuery(<Settlement />)
    
    await waitFor(() => {
      const datePicker = screen.getByTestId('date-picker')
      fireEvent.change(datePicker, { target: { value: '2026-03-20' } })
      expect(datePicker).toHaveValue('2026-03-20')
    })
  })
})
