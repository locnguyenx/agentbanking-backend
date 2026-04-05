import { describe, it, expect, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { SystemAdmin } from '../pages/SystemAdmin'
import React from 'react'

vi.mock('../api/client', () => ({
  default: {
    getAdminHealthAll: vi.fn().mockResolvedValue({
      services: [
        { name: 'gateway', port: 8080, purpose: 'API Gateway', status: 'UP', lastChecked: '2026-04-05T10:30:00+08:00' },
        { name: 'ledger', port: 8082, purpose: 'Ledger', status: 'UP', lastChecked: '2026-04-05T10:30:00+08:00' },
      ],
      summary: { total: 2, healthy: 2, unhealthy: 0 },
      timestamp: '2026-04-05T10:30:00+08:00',
    }),
    getAdminMetrics: vi.fn().mockResolvedValue({
      serviceName: 'ledger-service',
      jvm: { memoryUsedMb: 256, memoryMaxMb: 512, threadsActive: 45, cpuUsagePercent: 12, uptimeSeconds: 86400 },
      http: { requestsTotal: 15000, errorsTotal: 23, avgResponseTimeMs: 45 },
      timestamp: '2026-04-05T10:30:00+08:00',
    }),
    getAdminAuditLogs: vi.fn().mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
  },
}))

function createTestQueryClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } })
}

function renderWithProviders(ui: React.ReactElement) {
  const qc = createTestQueryClient()
  return render(<MemoryRouter><QueryClientProvider client={qc}>{ui}</QueryClientProvider></MemoryRouter>)
}

describe('SystemAdmin', () => {
  it('should render System Administration page', async () => {
    renderWithProviders(<SystemAdmin />)
    await waitFor(() => expect(screen.getByText('System Administration')).toBeInTheDocument())
  })

  it('should display service health section', async () => {
    renderWithProviders(<SystemAdmin />)
    await waitFor(() => {
      expect(screen.getByText('Service Health')).toBeInTheDocument()
    })
  })

  it('should show metrics panel with service selector', async () => {
    renderWithProviders(<SystemAdmin />)
    await waitFor(() => {
      expect(screen.getByText('Service Metrics')).toBeInTheDocument()
      expect(screen.getByRole('combobox')).toBeInTheDocument()
    })
  })

  it('should have refresh buttons', async () => {
    renderWithProviders(<SystemAdmin />)
    await waitFor(() => expect(screen.getAllByText('Refresh').length).toBeGreaterThan(0))
  })

  it('should display Audit Logs section', async () => {
    renderWithProviders(<SystemAdmin />)
    await waitFor(() => expect(screen.getByText('Audit Logs')).toBeInTheDocument())
  })
})
