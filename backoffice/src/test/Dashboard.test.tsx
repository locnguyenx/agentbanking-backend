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
    expect(document.querySelector('.loading')).toBeInTheDocument()
  })

  it('should render dashboard data after loading', async () => {
    renderWithQuery(<Dashboard />)
    
    await waitFor(() => {
      expect(screen.getByText('Total Agents')).toBeInTheDocument()
    })
    
    expect(screen.getByText("Today's Volume")).toBeInTheDocument()
    expect(screen.getByText('Transactions')).toBeInTheDocument()
    expect(screen.getByText('Pending KYC')).toBeInTheDocument()
  })

  it('should render all metric cards', async () => {
    renderWithQuery(<Dashboard />)
    
    await waitFor(() => {
      expect(screen.getByText('Total Agents')).toBeInTheDocument()
    })
    
    expect(screen.getByText('2,847')).toBeInTheDocument()
    expect(screen.getByText('RM 1.2M')).toBeInTheDocument()
    expect(screen.getByText('4,521')).toBeInTheDocument()
  })
})
