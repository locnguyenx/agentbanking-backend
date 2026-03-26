import { describe, it, expect, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Dashboard } from '../pages/Dashboard'

vi.mock('../api/client', () => ({
  default: {
    getDashboard: vi.fn().mockResolvedValue({
      totalTransactions: 150,
      totalVolume: '25000.00',
      activeAgents: 45,
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
    <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>
  )
}

describe('Dashboard', () => {
  it('should render loading state initially', () => {
    renderWithQuery(<Dashboard />)
    expect(screen.getByText('Loading...')).toBeInTheDocument()
  })

  it('should render dashboard data after loading', async () => {
    renderWithQuery(<Dashboard />)
    
    await waitFor(() => {
      expect(screen.getByText('Today\'s Transactions')).toBeInTheDocument()
    })
    
    expect(screen.getByText('150')).toBeInTheDocument()
    expect(screen.getByText('RM 25000.00')).toBeInTheDocument()
    expect(screen.getByText('45')).toBeInTheDocument()
  })

  it('should render all metric cards', async () => {
    renderWithQuery(<Dashboard />)
    
    await waitFor(() => {
      expect(screen.getByText('Today\'s Transactions')).toBeInTheDocument()
    })
    
    expect(screen.getByText('Total Volume')).toBeInTheDocument()
    expect(screen.getByText('Active Agents')).toBeInTheDocument()
  })
})
