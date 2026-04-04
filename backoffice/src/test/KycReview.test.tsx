import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { KycReview } from '../pages/KycReview'
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
    getKycReviewQueue: vi.fn().mockResolvedValue({
      content: [
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
      ],
      totalElements: 2,
      totalPages: 1,
    }),
    approveKyc: vi.fn().mockResolvedValue({}),
    rejectKyc: vi.fn().mockResolvedValue({}),
  },
}))

describe('KycReview', () => {
  it('should render KYC review page', async () => {
    renderWithProviders(<KycReview />)
    
    await waitFor(() => {
      expect(screen.getByTestId('page-title')).toBeInTheDocument()
    })
  })

  it('should display page title', async () => {
    renderWithProviders(<KycReview />)
    
    await waitFor(() => {
      expect(screen.getByText('KYC Review Queue')).toBeInTheDocument()
    })
  })

  it('should render search input', async () => {
    renderWithProviders(<KycReview />)
    
    await waitFor(() => {
      expect(screen.getByTestId('search-input')).toBeInTheDocument()
    })
  })

  it('should display KYC items in queue', async () => {
    renderWithProviders(<KycReview />)
    
    await waitFor(() => {
      expect(screen.getByText('MUTHU KUMAR')).toBeInTheDocument()
      expect(screen.getByText('PRIYA DEVI')).toBeInTheDocument()
    })
  })

  it('should display approve and reject buttons', async () => {
    renderWithProviders(<KycReview />)
    
    await waitFor(() => {
      const approveButtons = document.querySelectorAll('[data-testid^="approve-"]')
      const rejectButtons = document.querySelectorAll('[data-testid^="reject-"]')
      expect(approveButtons.length).toBeGreaterThan(0)
      expect(rejectButtons.length).toBeGreaterThan(0)
    })
  })

  it('should have working search input', async () => {
    renderWithProviders(<KycReview />)
    
    await waitFor(() => {
      const searchInput = screen.getByTestId('search-input')
      fireEvent.change(searchInput, { target: { value: 'MUTHU' } })
      expect(searchInput).toHaveValue('MUTHU')
    })
  })
})