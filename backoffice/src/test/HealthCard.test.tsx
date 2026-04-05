import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { HealthCard } from '../components/HealthCard'
import React from 'react'

describe('HealthCard', () => {
  it('should render service info with UP status', () => {
    render(
      <HealthCard 
        name="gateway" 
        port={8080} 
        purpose="API Gateway" 
        status="UP" 
        lastChecked="2026-04-05T10:30:00+08:00" 
        onDrillDown={() => {}} 
      />
    )
    expect(screen.getByText('gateway')).toBeInTheDocument()
    expect(screen.getByText('UP')).toBeInTheDocument()
  })

  it('should show green badge for UP status', () => {
    render(
      <HealthCard 
        name="ledger" 
        port={8082} 
        purpose="Ledger" 
        status="UP" 
        lastChecked="2026-04-05T10:30:00+08:00" 
        onDrillDown={() => {}} 
      />
    )
    expect(screen.getByText('UP')).toHaveClass('badge-success')
  })

  it('should show red badge for DOWN status', () => {
    render(
      <HealthCard 
        name="rules" 
        port={8081} 
        purpose="Rules" 
        status="DOWN" 
        lastChecked="2026-04-05T10:30:00+08:00" 
        onDrillDown={() => {}} 
      />
    )
    expect(screen.getByText('DOWN')).toHaveClass('badge-error')
  })

  it('should show amber badge for DEGRADED status', () => {
    render(
      <HealthCard 
        name="switch" 
        port={8084} 
        purpose="Switch" 
        status="DEGRADED" 
        lastChecked="2026-04-05T10:30:00+08:00" 
        onDrillDown={() => {}} 
      />
    )
    expect(screen.getByText('DEGRADED')).toHaveClass('badge-warning')
  })

  it('should call onDrillDown when clicked', () => {
    const mockDrillDown = vi.fn()
    render(
      <HealthCard 
        name="auth" 
        port={8087} 
        purpose="Auth" 
        status="UP" 
        lastChecked="2026-04-05T10:30:00+08:00" 
        onDrillDown={mockDrillDown} 
      />
    )
    screen.getByTestId('health-card').click()
    expect(mockDrillDown).toHaveBeenCalledWith('auth')
  })

  it('should show error message when provided', () => {
    render(
      <HealthCard 
        name="biller" 
        port={8085} 
        purpose="Biller" 
        status="DOWN" 
        lastChecked="2026-04-05T10:30:00+08:00"
        error="Connection refused"
        onDrillDown={() => {}} 
      />
    )
    expect(screen.getByText('Connection refused')).toBeInTheDocument()
  })
})
