import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MetricsPanel } from '../components/MetricsPanel'
import React from 'react'

describe('MetricsPanel', () => {
  const mockMetrics = {
    serviceName: 'ledger-service',
    jvm: { memoryUsedMb: 256.5, memoryMaxMb: 512.0, threadsActive: 45, cpuUsagePercent: 12.3, uptimeSeconds: 86400 },
    http: { requestsTotal: 15000, errorsTotal: 23, avgResponseTimeMs: 45.2 },
    timestamp: '2026-04-05T10:30:00+08:00',
  }

  const mockServices = [
    { name: 'gateway' },
    { name: 'ledger' },
    { name: 'rules' },
  ]

  it('should render service selector', () => {
    render(<MetricsPanel service={null} metrics={null} services={mockServices} onSelectService={() => {}} />)
    expect(screen.getByRole('combobox')).toBeInTheDocument()
  })

  it('should show placeholder when no service selected', () => {
    render(<MetricsPanel service={null} metrics={null} services={mockServices} onSelectService={() => {}} />)
    expect(screen.getByText(/Select a service to view metrics/)).toBeInTheDocument()
  })

  it('should render JVM metrics when service selected', () => {
    render(<MetricsPanel service="ledger" metrics={mockMetrics} services={mockServices} onSelectService={() => {}} />)
    expect(screen.getByText(/256.5/)).toBeInTheDocument()
    expect(screen.getByText(/512.0/)).toBeInTheDocument()
  })

  it('should render HTTP metrics', () => {
    render(<MetricsPanel service="ledger" metrics={mockMetrics} services={mockServices} onSelectService={() => {}} />)
    expect(screen.getByText(/15,000/)).toBeInTheDocument()
    expect(screen.getByText(/45.2/)).toBeInTheDocument()
  })

  it('should show error when metrics unavailable', () => {
    render(<MetricsPanel service="ledger" metrics={{ error: 'Metrics unavailable' }} services={mockServices} onSelectService={() => {}} />)
    expect(screen.getByText(/Metrics unavailable/)).toBeInTheDocument()
  })

  it('should call onSelectService when dropdown changes', () => {
    const mockSelect = vi.fn()
    render(<MetricsPanel service={null} metrics={null} services={mockServices} onSelectService={mockSelect} />)
    
    const select = screen.getByRole('combobox')
    select.value = 'ledger'
    select.dispatchEvent(new Event('change', { bubbles: true }))
    
    expect(mockSelect).toHaveBeenCalled()
  })

  it('should show loading state', () => {
    render(<MetricsPanel service="ledger" metrics={null} isLoading={true} services={mockServices} onSelectService={() => {}} />)
    expect(screen.getByText(/Loading metrics/)).toBeInTheDocument()
  })
})
