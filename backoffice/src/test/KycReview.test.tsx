import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { KycReview } from '../pages/KycReview'

vi.mock('../api/client', () => ({
  default: {
    getKycReviewQueue: vi.fn().mockResolvedValue({
      content: [
        { verificationId: '10000000-0000-0000-0000-000000000002', mykadMasked: '9204********', fullName: 'MUTHU KUMAR', biometricMatch: 'MATCH', amlStatus: 'CLEAN', rejectionReason: 'Biometric quality below threshold' },
        { verificationId: '10000000-0000-0000-0000-000000000003', mykadMasked: '8807********', fullName: 'PRIYA DEVI', biometricMatch: 'MATCH', amlStatus: 'FLAGGED', rejectionReason: 'AML screening requires review' },
      ]
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

describe('KycReview', () => {
  it('should render KYC review page', async () => {
    renderWithQuery(<KycReview />)
    
    await waitFor(() => {
      expect(screen.getByTestId('page-title')).toBeInTheDocument()
    })
  })

  it('should display page title', async () => {
    renderWithQuery(<KycReview />)
    
    await waitFor(() => {
      expect(screen.getByText('KYC Review Queue')).toBeInTheDocument()
    })
  })

  it('should render search input', async () => {
    renderWithQuery(<KycReview />)
    
    await waitFor(() => {
      expect(screen.getByTestId('search-input')).toBeInTheDocument()
    })
  })

  it('should display KYC stats', async () => {
    renderWithQuery(<KycReview />)
    
    await waitFor(() => {
      expect(screen.getByText('Pending Review')).toBeInTheDocument()
      expect(screen.getByText('Approved Today')).toBeInTheDocument()
      expect(screen.getByText('Rejected Today')).toBeInTheDocument()
      expect(screen.getByText('AML Flags')).toBeInTheDocument()
    })
  })

  it('should display KYC items in queue', async () => {
    renderWithQuery(<KycReview />)
    
    await waitFor(() => {
      expect(screen.getByText('Ali Bin Ahmad')).toBeInTheDocument()
      expect(screen.getByText('Muthu Kumar')).toBeInTheDocument()
    })
  })

  it('should display approve and reject buttons', async () => {
    renderWithQuery(<KycReview />)
    
    await waitFor(() => {
      expect(screen.getByTestId('approve-KYC-001-button')).toBeInTheDocument()
      expect(screen.getByTestId('reject-KYC-001-button')).toBeInTheDocument()
    })
  })

  it('should display view buttons', async () => {
    renderWithQuery(<KycReview />)
    
    await waitFor(() => {
      expect(screen.getByTestId('view-KYC-001-button')).toBeInTheDocument()
      expect(screen.getByTestId('view-KYC-002-button')).toBeInTheDocument()
    })
  })

  it('should have working search input', async () => {
    renderWithQuery(<KycReview />)
    
    await waitFor(() => {
      const searchInput = screen.getByTestId('search-input')
      fireEvent.change(searchInput, { target: { value: 'Ali' } })
      expect(searchInput).toHaveValue('Ali')
    })
  })

  it('should display priority badges', async () => {
    renderWithQuery(<KycReview />)
    
    await waitFor(() => {
      expect(screen.getAllByText('High Priority').length).toBeGreaterThan(0)
      expect(screen.getAllByText('Medium Priority').length).toBeGreaterThan(0)
    })
  })

  it('should display biometric status badges', async () => {
    renderWithQuery(<KycReview />)
    
    await waitFor(() => {
      expect(screen.getAllByText('Biometric: Match').length).toBeGreaterThan(0)
      expect(screen.getAllByText('Biometric: Low Match').length).toBeGreaterThan(0)
    })
  })

  it('should display AML status badges', async () => {
    renderWithQuery(<KycReview />)
    
    await waitFor(() => {
      expect(screen.getAllByText('AML: Clean').length).toBeGreaterThan(0)
    })
  })

  it('should display rejection reasons', async () => {
    renderWithQuery(<KycReview />)
    
    await waitFor(() => {
      expect(screen.getByText('Biometric quality below threshold')).toBeInTheDocument()
    })
  })
})
